/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static java.lang.Integer.toHexString;
import static java.lang.String.format;
import static java.lang.System.identityHashCode;
import static org.apache.commons.io.FilenameUtils.normalize;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

/**
 * Abstract implementation of the ArtifactClassLoader interface, that manages shutdown listeners.
 */
public class MuleArtifactClassLoader extends FineGrainedControlClassLoader implements ArtifactClassLoader {

  static {
    registerAsParallelCapable();
  }

  private static final Logger LOGGER = getLogger(MuleArtifactClassLoader.class);
  private static final String DEFAULT_RESOURCE_RELEASER_CLASS_LOCATION =
      "/org/mule/module/artifact/classloader/DefaultResourceReleaser.class";

  static final Pattern DOT_REPLACEMENT_PATTERN = Pattern.compile("\\.");
  static final String PATH_SEPARATOR = "/";
  static final String RESOURCE_PREFIX = "resource::";
  static final Pattern GAV_PATTERN = Pattern.compile(RESOURCE_PREFIX + "(\\S+):(\\S+):(\\S+)?:(\\S+)");

  protected List<ShutdownListener> shutdownListeners = new ArrayList<>();

  private final String artifactId;
  private final Object localResourceLocatorLock = new Object();
  private volatile LocalResourceLocator localResourceLocator;
  private String resourceReleaserClassLocation = DEFAULT_RESOURCE_RELEASER_CLASS_LOCATION;
  private ResourceReleaser resourceReleaserInstance;
  private ArtifactDescriptor artifactDescriptor;
  private Map<String, URLClassLoader> pathMapping = new HashMap<>();

  /**
   * Constructs a new {@link MuleArtifactClassLoader} for the given URLs
   *
   * @param artifactId artifact unique ID. Non empty.
   * @param artifactDescriptor descriptor for the artifact owning the created class loader. Non null.
   * @param urls the URLs from which to load classes and resources
   * @param parent the parent class loader for delegation
   * @param lookupPolicy policy used to guide the lookup process. Non null
   */
  public MuleArtifactClassLoader(String artifactId, ArtifactDescriptor artifactDescriptor, URL[] urls, ClassLoader parent,
                                 ClassLoaderLookupPolicy lookupPolicy) {
    super(urls, parent, lookupPolicy);
    checkArgument(!isEmpty(artifactId), "artifactId cannot be empty");
    checkArgument(artifactDescriptor != null, "artifactDescriptor cannot be null");
    this.artifactId = artifactId;
    this.artifactDescriptor = artifactDescriptor;
  }

  @Override
  public String getArtifactId() {
    return artifactId;
  }

  @Override
  public <T extends ArtifactDescriptor> T getArtifactDescriptor() {
    return (T) artifactDescriptor;
  }

  @Override
  public URL findResource(String name) {
    if (name.startsWith(RESOURCE_PREFIX)) {
      Matcher matcher = GAV_PATTERN.matcher(name);
      // Check for specific artifact requests within our URLs
      if (matcher.matches()) {
        String groupId = matcher.group(1);
        String artifactId = matcher.group(2);
        String version = matcher.group(3);
        String resource = matcher.group(4);
        LOGGER.debug("Artifact request for '{}' in group '{}', artifact '{}' and version '{}'.", resource, groupId, artifactId,
                     version);
        String normalizedResource = normalize(resource, true);

        URLClassLoader classLoader =
            pathMapping.computeIfAbsent(asPath(groupId, artifactId, version),
                                        (CheckedFunction<String, URLClassLoader>) path -> Arrays.stream(getURLs())
                                            .filter(url -> url.getPath().contains(path))
                                            .findAny()
                                            .map(url -> new URLClassLoader(new URL[] {url}))
                                            .orElse(null));

        if (classLoader != null) {
          return classLoader.findResource(normalizedResource);
        }
      }
    }
    return super.findResource(name);
  }

  private String asPath(String groupId, String artifactId, String version) {
    String groupIdPath = DOT_REPLACEMENT_PATTERN.matcher(groupId).replaceAll(PATH_SEPARATOR);
    String versionPath = version != null ? version : "";
    return groupIdPath + PATH_SEPARATOR + artifactId + PATH_SEPARATOR + versionPath;
  }

  @Override
  public URL findInternalResource(String resource) {
    return findResource(resource);
  }

  @Override
  public Class<?> loadInternalClass(String name) throws ClassNotFoundException {
    return loadClass(name);
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
    pathMapping.forEach((path, classloader) -> {
      try {
        classloader.close();
      } catch (IOException e) {
        reportPossibleLeak(e, path);
      }
    });
    pathMapping.clear();
    try {
      createResourceReleaserInstance().release();
    } catch (Exception e) {
      LOGGER.error("Cannot create resource releaser instance", e);
    }
    super.dispose();
    shutdownListeners();
  }

  void reportPossibleLeak(Exception e, String artifactId) {
    final String message = "Error disposing classloader for '{}'. This can cause a memory leak";
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(message, artifactId, e);
    } else {
      LOGGER.error(message, artifactId);
    }
  }

  private void shutdownListeners() {
    for (ShutdownListener listener : shutdownListeners) {
      try {
        listener.execute();
      } catch (Exception e) {
        LOGGER.error("Error executing shutdown listener", e);
      }
    }

    // Clean up references to shutdown listeners in order to avoid class loader leaks
    shutdownListeners.clear();
  }

  private <T> T createCustomInstance(String classLocation) {
    InputStream classStream = null;
    try {
      classStream = this.getClass().getResourceAsStream(classLocation);
      byte[] classBytes = IOUtils.toByteArray(classStream);
      classStream.close();
      Class clazz = this.defineClass(null, classBytes, 0, classBytes.length);
      return (T) clazz.newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Can not create instance from resource: " + classLocation, e);
    } finally {
      closeQuietly(classStream);
    }
  }

  /**
   * Creates a {@link ResourceReleaser} using this classloader, only used outside in unit tests.
   */
  protected ResourceReleaser createResourceReleaserInstance() {
    if (resourceReleaserInstance == null) {
      resourceReleaserInstance = createCustomInstance(resourceReleaserClassLocation);
    }
    return resourceReleaserInstance;
  }

  public void setResourceReleaserClassLocation(String resourceReleaserClassLocation) {
    this.resourceReleaserClassLocation = resourceReleaserClassLocation;
  }

  @Override
  public URL findLocalResource(String resourceName) {
    return getLocalResourceLocator().findLocalResource(resourceName);
  }

  private LocalResourceLocator getLocalResourceLocator() {
    if (localResourceLocator == null) {
      synchronized (localResourceLocatorLock) {
        if (localResourceLocator == null) {
          localResourceLocator = new DirectoryResourceLocator(getLocalResourceLocations());
        }
      }
    }
    return localResourceLocator;
  }

  @Override
  public String toString() {
    return format("%s[%s]@%s", getClass().getName(), getArtifactId(), toHexString(identityHashCode(this)));
  }
}
