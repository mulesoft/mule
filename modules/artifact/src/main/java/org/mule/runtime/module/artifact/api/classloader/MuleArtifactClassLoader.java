/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.artifact.api.classloader.BlockingLoggerResolutionClassRegistry.getBlockingLoggerResolutionClassRegistry;

import static java.lang.Integer.toHexString;
import static java.lang.String.format;
import static java.lang.System.identityHashCode;
import static java.lang.reflect.Modifier.isAbstract;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.apache.commons.io.FilenameUtils.normalize;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.module.artifact.classloader.ClassLoaderResourceReleaser;
import org.mule.module.artifact.classloader.GroovyResourceReleaser;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.internal.classloader.ResourceReleaserExecutor;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

/**
 * Abstract implementation of the ArtifactClassLoader interface, that manages shutdown listeners.
 */
public class MuleArtifactClassLoader extends FineGrainedControlClassLoader implements ArtifactClassLoader {

  static {
    registerAsParallelCapable();
    getBlockingLoggerResolutionClassRegistry().registerClassNeedingBlockingLoggerResolution(MuleArtifactClassLoader.class);
  }

  private static final Logger LOGGER = getLogger(MuleArtifactClassLoader.class);

  static final Pattern DOT_REPLACEMENT_PATTERN = Pattern.compile("\\.");
  static final String PATH_SEPARATOR = "/";
  static final String RESOURCE_PREFIX = "resource::";
  static final String WILDCARD = "*";

  private static final String NO_WILDCARD = "([^\\" + WILDCARD + "]+)";
  private static final String NO_WILDCARD_NO_SPACES = "([^\\" + WILDCARD + "|\\s]+)";
  private static final String NO_SPACES = "([^\\s]+)";

  static final Pattern GAV_EXTENDED_PATTERN = Pattern.compile(
                                                              RESOURCE_PREFIX
                                                                  // groupId
                                                                  + NO_WILDCARD_NO_SPACES + ":"
                                                                  // artifactId
                                                                  + NO_WILDCARD_NO_SPACES + ":"
                                                                  // version (can be a wildcard)
                                                                  + NO_SPACES + ":"
                                                                  // classifier (optional)
                                                                  + NO_WILDCARD_NO_SPACES + "?:"
                                                                  // type
                                                                  + NO_WILDCARD_NO_SPACES + ":"
                                                                  // resource
                                                                  + NO_WILDCARD);

  private static final Pattern MAVEN_ARTIFACT_PATTERN = Pattern.compile(
                                                                        PATH_SEPARATOR
                                                                            // artifactId
                                                                            + NO_SPACES + PATH_SEPARATOR
                                                                            // version
                                                                            + NO_SPACES + PATH_SEPARATOR
                                                                            // artifactId-version
                                                                            + "\\1-\\2"
                                                                            // classifier (optional)
                                                                            + "-?" + NO_SPACES + "?"
                                                                            // type
                                                                            + "\\." + NO_SPACES);

  protected List<ShutdownListener> shutdownListeners = new ArrayList<>();

  private final String artifactId;
  private final Object localResourceLocatorLock = new Object();
  private volatile LocalResourceLocator localResourceLocator;
  private volatile boolean shouldReleaseJdbcReferences = false;
  private volatile boolean shouldReleaseIbmMQResources = false;
  private volatile boolean shouldReleaseActiveMQReferences = false;
  private volatile boolean shouldReleaseGroovyReferences = false;
  private ResourceReleaser jdbcResourceReleaserInstance;
  private final ArtifactDescriptor artifactDescriptor;
  private final Object descriptorMappingLock = new Object();
  private final Map<BundleDescriptor, URLClassLoader> descriptorMapping = new HashMap<>();
  private final ResourceReleaserExecutor resourceReleaserExecutor = new ResourceReleaserExecutor(this::reportPossibleLeak);
  private Optional<ModuleLayerInformationSupplier> moduleLayerInformation = empty();

  /**
   * Constructs a new {@link MuleArtifactClassLoader} for the given URLs
   *
   * @param artifactId         artifact unique ID. Non empty.
   * @param artifactDescriptor descriptor for the artifact owning the created class loader. Non null.
   * @param urls               the URLs from which to load classes and resources
   * @param parent             the parent class loader for delegation
   * @param lookupPolicy       policy used to guide the lookup process. Non null
   */
  public MuleArtifactClassLoader(String artifactId, ArtifactDescriptor artifactDescriptor, URL[] urls, ClassLoader parent,
                                 ClassLoaderLookupPolicy lookupPolicy) {
    super(urls, parent, lookupPolicy);
    checkArgument(!isEmpty(artifactId), "artifactId cannot be empty");
    checkArgument(artifactDescriptor != null, "artifactDescriptor cannot be null");
    this.artifactId = artifactId;
    this.artifactDescriptor = artifactDescriptor;
    this.resourceReleaserExecutor.addResourceReleaser(() -> new ClassLoaderResourceReleaser(this));
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
      Matcher matcher = GAV_EXTENDED_PATTERN.matcher(name);
      // Check for specific artifact requests within our URLs
      if (matcher.matches()) {
        String groupId = matcher.group(1);
        String artifactId = matcher.group(2);
        String version = matcher.group(3);
        String classifier = matcher.group(4);
        String type = matcher.group(5);
        String resource = matcher.group(6);
        LOGGER
            .debug("Artifact request for '{}' in group '{}', artifact '{}' and version '{}', with classifier '{}' and type '{}'.",
                   resource, groupId, artifactId, version, classifier, type);
        String normalizedResource = normalize(resource, true);

        BundleDescriptor requestDescriptor = new BundleDescriptor.Builder()
            .setGroupId(groupId)
            .setArtifactId(artifactId)
            .setVersion(version)
            // As the requested version could be an SNAPSHOT we have to compare using baseVersion instead of version
            .setBaseVersion(version)
            .setClassifier(classifier)
            .setType(type)
            .build();

        URLClassLoader classLoader;
        if (WILDCARD.equals(version)) {
          // Need to check for equals in this case
          classLoader = descriptorMapping.entrySet().stream()
              .filter(entry -> isRequestedArtifact(entry.getKey(), requestDescriptor, () -> false))
              .map(Map.Entry::getValue)
              .findAny().orElse(null);
        } else {
          // Otherwise hash will work
          classLoader = descriptorMapping.get(requestDescriptor);
        }
        if (classLoader != null) {
          return classLoader.findResource(normalizedResource);
        } else {
          // Analyze whether there's a matching URL in this CL
          Optional<URL> match = Arrays.stream(getURLs())
              .filter(url -> {
                String urlPath = url.getPath();
                return urlPath.contains(asPath(requestDescriptor)) && urlPath.endsWith(asExtension(requestDescriptor));
              })
              .findFirst();
          if (match.isPresent()) {
            URL url = match.get();
            BundleDescriptor matchDescriptor = toBundleDescriptor(url, groupId);
            // We don't want class loaders in limbo
            synchronized (descriptorMappingLock) {
              if (descriptorMapping.get(matchDescriptor) == null) {
                URLClassLoader urlClassLoader =
                    new URLClassLoader(new URL[] {url}, getSystemClassLoader());;
                descriptorMapping.put(matchDescriptor, urlClassLoader);
              }
            }
            return descriptorMapping.get(matchDescriptor).findResource(normalizedResource);
          }
        }
      }
    }
    return super.findResource(name);
  }

  private String asPath(BundleDescriptor descriptor) {
    String groupIdPath = getGroupIdPath(descriptor.getGroupId());
    String versionPath = WILDCARD.equals(descriptor.getVersion()) ? "" : descriptor.getVersion();
    return groupIdPath + PATH_SEPARATOR + descriptor.getArtifactId() + PATH_SEPARATOR + versionPath;
  }

  private String asExtension(BundleDescriptor descriptor) {
    return descriptor.getClassifier().orElse("") + "." + descriptor.getType();
  }

  private BundleDescriptor toBundleDescriptor(URL artifactUrl, String groupId) {
    String artifactPath = artifactUrl.getPath();
    String groupIdPath = getGroupIdPath(groupId);
    int artifactIdIndex = artifactPath.indexOf(groupIdPath) + groupIdPath.length();
    Matcher urlMatcher = MAVEN_ARTIFACT_PATTERN.matcher(artifactPath.substring(artifactIdIndex));
    urlMatcher.find();
    return new BundleDescriptor.Builder()
        .setGroupId(groupId)
        .setArtifactId(urlMatcher.group(1))
        // This should be modified if we would need to work with timestamped SNAPSHOT versions
        .setVersion(urlMatcher.group(2))
        .setBaseVersion(urlMatcher.group(2))
        .setClassifier(urlMatcher.group(3))
        .setType(urlMatcher.group(4))
        .build();
  }

  private String getGroupIdPath(String groupId) {
    return DOT_REPLACEMENT_PATTERN.matcher(groupId).replaceAll(PATH_SEPARATOR);
  }

  boolean isRequestedArtifact(BundleDescriptor descriptor, BundleDescriptor artifact, Supplier<Boolean> onVersionMismatch) {
    return isRequestedArtifact(descriptor, artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(),
                               artifact.getClassifier(), artifact.getType(), onVersionMismatch);
  }

  boolean isRequestedArtifact(BundleDescriptor descriptor, String groupId, String artifactId, String version,
                              Optional<String> classifier, String type, Supplier<Boolean> onVersionMismatch) {
    boolean versionResult = true;
    if (!descriptor.getBaseVersion().equals(version) && !WILDCARD.equals(version)) {
      versionResult = onVersionMismatch.get();
    }
    return descriptor.getGroupId().equals(groupId) && descriptor.getArtifactId().equals(artifactId) && versionResult
        && descriptor.getClassifier().equals(classifier) && descriptor.getType().equals(type);
  }

  @Override
  public URL findInternalResource(String resource) {
    return findResource(resource);
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class<?> clazz = super.loadClass(name, resolve);
    if (!shouldReleaseJdbcReferences && Driver.class.isAssignableFrom(clazz) &&
        !(clazz.equals(Driver.class) || clazz.isInterface() || isAbstract(clazz.getModifiers()))) {
      shouldReleaseJdbcReferences = true;
    }
    if (!shouldReleaseIbmMQResources && name.startsWith("com.ibm.mq")) {
      shouldReleaseIbmMQResources = true;
    }

    if (!shouldReleaseActiveMQReferences && name.startsWith("org.apache.activemq")) {
      shouldReleaseActiveMQReferences = true;
    }

    if (!shouldReleaseGroovyReferences && name.startsWith("org.codehaus.groovy")) {
      shouldReleaseGroovyReferences = true;
    }
    return clazz;
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
    descriptorMapping.forEach((descriptor, classloader) -> {
      try {
        classloader.close();
      } catch (IOException e) {
        reportPossibleLeak(e, descriptor.getArtifactId());
      }
    });
    descriptorMapping.clear();

    if (shouldReleaseGroovyReferences) {
      resourceReleaserExecutor.addResourceReleaser(() -> new GroovyResourceReleaser(this));
    }
    resourceReleaserExecutor.executeResourceReleasers();

    super.dispose();
    shutdownListeners();
  }

  private void reportPossibleLeak(Throwable t) {
    reportPossibleLeak(t, artifactId);
  }

  protected void reportPossibleLeak(Throwable t, String artifactId) {
    LOGGER.error(format("Error disposing classloader for '%s'. This can cause a memory leak", artifactId), t);
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

  @Override
  public void setModuleLayerInformationSupplier(ModuleLayerInformationSupplier moduleLayerInformationSupplier) {
    this.moduleLayerInformation = of(moduleLayerInformationSupplier);
  }

  @Override
  public Optional<ModuleLayerInformationSupplier> getModuleLayerInformation() {
    return moduleLayerInformation;
  }

}
