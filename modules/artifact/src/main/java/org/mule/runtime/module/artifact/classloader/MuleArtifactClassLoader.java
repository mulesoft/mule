/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static java.lang.Integer.toHexString;
import static java.lang.String.format;
import static java.lang.System.identityHashCode;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.mule.runtime.core.util.Preconditions.checkArgument;

import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.StringUtils;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation of the ArtifactClassLoader interface, that manages shutdown listeners.
 */
public class MuleArtifactClassLoader extends FineGrainedControlClassLoader implements ArtifactClassLoader {

  static {
    registerAsParallelCapable();
  }

  private static final String DEFAULT_RESOURCE_RELEASER_CLASS_LOCATION =
      "/org/mule/runtime/module/artifact/classloader/DefaultResourceReleaser.class";
  private final String name;

  protected Logger logger = LoggerFactory.getLogger(getClass());

  protected List<ShutdownListener> shutdownListeners = new ArrayList<>();

  private LocalResourceLocator localResourceLocator;

  private String resourceReleaserClassLocation = DEFAULT_RESOURCE_RELEASER_CLASS_LOCATION;

  public MuleArtifactClassLoader(String name, URL[] urls, ClassLoader parent, ClassLoaderLookupPolicy lookupPolicy) {
    super(urls, parent, lookupPolicy);
    checkArgument(!StringUtils.isEmpty(name), "Artifact name cannot be empty");
    this.name = name;
  }

  @Override
  public String getArtifactName() {
    return name;
  }

  protected String[] getLocalResourceLocations() {
    return new String[0];
  }

  @Override
  public ClassLoader getClassLoader() {
    return this;
  }

  @Override
  public void addShutdownListener(ShutdownListener listener) {
    this.shutdownListeners.add(listener);
  }

  @Override
  public void dispose() {
    for (ShutdownListener listener : shutdownListeners) {
      try {
        listener.execute();
      } catch (Exception e) {
        logger.error("Error executing shutdown listener", e);
      }
    }

    // Clean up references to shutdown listeners in order to avoid class loader leaks
    shutdownListeners.clear();

    try {
      createResourceReleaserInstance().release();
    } catch (Exception e) {
      logger.error("Cannot create resource releaser instance", e);
    }

    super.dispose();
  }

  public void setResourceReleaserClassLocation(String resourceReleaserClassLocation) {
    this.resourceReleaserClassLocation = resourceReleaserClassLocation;
  }

  protected ResourceReleaser createResourceReleaserInstance() {
    InputStream classStream = null;
    try {
      classStream = this.getClass().getResourceAsStream(resourceReleaserClassLocation);
      byte[] classBytes = IOUtils.toByteArray(classStream);
      classStream.close();
      Class clazz = this.defineClass(null, classBytes, 0, classBytes.length);
      return (ResourceReleaser) clazz.newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Can not create resource releaser instance from resource: " + resourceReleaserClassLocation, e);
    } finally {
      closeQuietly(classStream);
    }
  }

  @Override
  public URL findLocalResource(String resourceName) {
    URL resource = getLocalResourceLocator().findLocalResource(resourceName);
    if (resource == null && getParent() instanceof LocalResourceLocator) {
      resource = ((LocalResourceLocator) getParent()).findLocalResource(resourceName);
    }
    return resource;
  }

  private LocalResourceLocator getLocalResourceLocator() {
    if (localResourceLocator == null) {
      localResourceLocator = new DirectoryResourceLocator(getLocalResourceLocations());
    }
    return localResourceLocator;
  }

  @Override
  public String toString() {
    return format("%s[%s]@%s", getClass().getName(), getArtifactName(), toHexString(identityHashCode(this)));
  }
}
