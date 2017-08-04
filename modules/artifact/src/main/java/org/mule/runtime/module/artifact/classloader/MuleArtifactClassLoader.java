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
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;

import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract implementation of the ArtifactClassLoader interface, that manages shutdown listeners.
 */
public class MuleArtifactClassLoader extends FineGrainedControlClassLoader implements ArtifactClassLoader {

  static {
    registerAsParallelCapable();
  }

  private static final String DEFAULT_RESOURCE_RELEASER_CLASS_LOCATION =
      "/org/mule/module/artifact/classloader/DefaultResourceReleaser.class";
  private static final String DEFAULT_ERROR_HOOKS_CLASS_LOCATION =
      "/org/mule/module/artifact/classloader/ErrorHooksConfiguration.class";

  protected List<ShutdownListener> shutdownListeners = new ArrayList<>();

  private final String artifactId;
  private LocalResourceLocator localResourceLocator;
  private String resourceReleaserClassLocation = DEFAULT_RESOURCE_RELEASER_CLASS_LOCATION;
  private String errorHooksClassLocation = DEFAULT_ERROR_HOOKS_CLASS_LOCATION;
  private ArtifactDescriptor artifactDescriptor;

  /**
   * Constructs a new {@link MuleArtifactClassLoader } for the given URLs
   *
   * @param artifactId artifact unique ID. Non empty.
   * @param artifactDescriptor descriptor for the artifact owning the created class loader. Non null.
   * @param urls the URLs from which to load classes and resources
   * @param parent the parent class loader for delegation
   * @param lookupPolicy policy used to guide the lookup process. Non null
   */
  public MuleArtifactClassLoader(String artifactId, ArtifactDescriptor artifactDescriptor, URL[] urls, ClassLoader parent,
                                 ClassLoaderLookupPolicy lookupPolicy) {
    this(artifactId, artifactDescriptor, urls, parent, lookupPolicy, true);
  }

  /**
   * Alternative constructor that allows not registering error hooks, useful for subclasses that need to do it after
   * their own initialization or not at all.
   */
  protected MuleArtifactClassLoader(String artifactId, ArtifactDescriptor artifactDescriptor, URL[] urls, ClassLoader parent,
                                    ClassLoaderLookupPolicy lookupPolicy, Boolean initialise) {
    super(urls, parent, lookupPolicy);
    checkArgument(!isEmpty(artifactId), "artifactId cannot be empty");
    checkArgument(artifactDescriptor != null, "artifactDescriptor cannot be null");
    this.artifactId = artifactId;
    this.artifactDescriptor = artifactDescriptor;

    if (initialise) {
      configureErrorHooks();
    }
  }

  @Override
  public String getArtifactId() {
    return artifactId;
  }

  @Override
  public <T extends ArtifactDescriptor> T getArtifactDescriptor() {
    return (T) artifactDescriptor;
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
    try {
      createResourceReleaserInstance().release();
    } catch (Exception e) {
      logger.error("Cannot create resource releaser instance", e);
    }
    super.dispose();
    shutdownListeners();
  }

  private void shutdownListeners() {
    for (ShutdownListener listener : shutdownListeners) {
      try {
        listener.execute();
      } catch (Exception e) {
        logger.error("Error executing shutdown listener", e);
      }
    }

    // Clean up references to shutdown listeners in order to avoid class loader leaks
    shutdownListeners.clear();
  }

  public void setResourceReleaserClassLocation(String resourceReleaserClassLocation) {
    this.resourceReleaserClassLocation = resourceReleaserClassLocation;
  }

  public void setErrorHooksClassLocation(String errorHooksClassLocation) {
    this.errorHooksClassLocation = errorHooksClassLocation;
  }

  protected Object createCustomInstance(String classLocation) {
    InputStream classStream = null;
    try {
      classStream = this.getClass().getResourceAsStream(classLocation);
      byte[] classBytes = IOUtils.toByteArray(classStream);
      classStream.close();
      Class clazz = this.defineClass(null, classBytes, 0, classBytes.length);
      return clazz.newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Can not create instance from resource: " + classLocation, e);
    } finally {
      closeQuietly(classStream);
    }
  }

  protected ResourceReleaser createResourceReleaserInstance() {
    return (ResourceReleaser) createCustomInstance(resourceReleaserClassLocation);
  }

  /**
   * Setup reactor-core error hooks, these are required for plugins that end up executing reactive streams.
   *
   * For instance, compatibility plugin which inherits from transformers that call {@code block()}.
   */
  protected void configureErrorHooks() {
    if (getURLs().length == 0 || !isReactorLoaded()) {
      return;
    }

    try {
      createCustomInstance(errorHooksClassLocation);
    } catch (Exception e) {
      logger.error("Cannot configure error hooks", e);
    }
  }

  protected Boolean isReactorLoaded() {
    try {
      Class reactorHooks = loadClass("reactor.core.publisher.Hooks");
      return reactorHooks.getClassLoader().equals(this);
    } catch (ClassNotFoundException e) {
      // ignore, we don't care if the plugin does not include reactor-core
    }

    return false;
  }

  @Override
  public URL findLocalResource(String resourceName) {
    return getLocalResourceLocator().findLocalResource(resourceName);
  }

  private LocalResourceLocator getLocalResourceLocator() {
    if (localResourceLocator == null) {
      localResourceLocator = new DirectoryResourceLocator(getLocalResourceLocations());
    }
    return localResourceLocator;
  }

  @Override
  public String toString() {
    return format("%s[%s]@%s", getClass().getName(), getArtifactId(), toHexString(identityHashCode(this)));
  }
}
