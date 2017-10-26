/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.io.FileUtils.toFile;
import static org.apache.commons.lang3.StringUtils.endsWithIgnoreCase;
import static org.eclipse.aether.util.artifact.ArtifactIdUtils.toId;
import static org.eclipse.aether.util.artifact.JavaScopes.COMPILE;
import static org.eclipse.aether.util.artifact.JavaScopes.PROVIDED;
import static org.eclipse.aether.util.artifact.JavaScopes.TEST;
import static org.eclipse.aether.util.filter.DependencyFilterUtils.andFilter;
import static org.eclipse.aether.util.filter.DependencyFilterUtils.classpathFilter;
import static org.eclipse.aether.util.filter.DependencyFilterUtils.orFilter;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import static org.mule.runtime.core.api.util.PropertiesUtils.loadProperties;
import static org.mule.test.runner.api.ArtifactClassificationType.APPLICATION;
import static org.mule.test.runner.api.ArtifactClassificationType.MODULE;
import static org.mule.test.runner.api.ArtifactClassificationType.PLUGIN;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.test.runner.classification.PatternExclusionsDependencyFilter;
import org.mule.test.runner.classification.PatternInclusionsDependencyFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the {@link ArtifactUrlClassification} based on the Maven dependencies declared by the rootArtifact using Eclipse
 * Aether. Uses a {@link DependencyResolver} to resolve Maven dependencies.
 * <p/>
 * The classification process classifies the rootArtifact dependencies in three groups: {@code provided}, {@code compile} and
 * {@code test} scopes. It resolves dependencies graph for each group applying filters and exclusions and classifies the list of
 * {@link URL}s that would define each class loader container, plugins and application.
 * <p/>
 * Dependencies resolution uses dependencies management declared by these artifacts while resolving the dependency graph.
 * <p/>
 * Plugins are discovered as {@link Extension} if they do have a annotated a {@link Class}. It generates the {@link Extension}
 * metadata in order to later register it to an {@link org.mule.runtime.core.api.extension.ExtensionManager}.
 *
 * @since 4.0
 */
public class AetherClassPathClassifier implements ClassPathClassifier {

  private static final String POM = "pom";
  private static final String POM_XML = POM + ".xml";
  private static final String POM_EXTENSION = "." + POM;
  private static final String ZIP_EXTENSION = ".zip";

  private static final String MAVEN_COORDINATES_SEPARATOR = ":";
  private static final String JAR_EXTENSION = "jar";
  private static final String SNAPSHOT_WILCARD_FILE_FILTER = "*-SNAPSHOT*.*";
  private static final String TESTS_CLASSIFIER = "tests";
  private static final String TESTS_JAR = "-tests.jar";
  private static final String SERVICE_PROPERTIES_FILE_NAME = "service.properties";
  private static final String SERVICE_PROVIDER_CLASS_NAME = "service.className";
  private static final String MULE_PLUGIN_CLASSIFIER = "mule-plugin";
  private static final String MULE_SERVICE_CLASSIFIER = "mule-service";

  private static final String RUNTIME_GROUP_ID = "org.mule.runtime";
  private static final String LOGGING_ARTIFACT_ID = "mule-module-logging";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final String muleVersion;

  private DependencyResolver dependencyResolver;
  private ArtifactClassificationTypeResolver artifactClassificationTypeResolver;
  private PluginResourcesResolver pluginResourcesResolver = new PluginResourcesResolver();
  private ServiceResourcesResolver serviceResourcesResolver = new ServiceResourcesResolver();

  static String getMuleVersion() {
    try {
      Properties properties = new Properties();
      properties.load(AetherClassPathClassifier.class.getResourceAsStream("/runner.properties"));
      return properties.getProperty("mule.version");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates an instance of the classifier.
   *
   * @param dependencyResolver {@link DependencyResolver} to resolve dependencies. Non null.
   * @param artifactClassificationTypeResolver {@link ArtifactClassificationTypeResolver} to identify rootArtifact type. Non null.
   */
  public AetherClassPathClassifier(DependencyResolver dependencyResolver,
                                   ArtifactClassificationTypeResolver artifactClassificationTypeResolver) {
    checkNotNull(dependencyResolver, "dependencyResolver cannot be null");
    checkNotNull(artifactClassificationTypeResolver, "artifactClassificationTypeResolver cannot be null");

    this.dependencyResolver = dependencyResolver;
    this.artifactClassificationTypeResolver = artifactClassificationTypeResolver;

    muleVersion = getMuleVersion();
  }

  /**
   * Classifies {@link URL}s and {@link Dependency}s to define how the container, plugins and application class loaders should be
   * created.
   *
   * @param context {@link ClassPathClassifierContext} to be used during the classification. Non null.
   * @return {@link ArtifactsUrlClassification} as result with the classification
   */
  @Override
  public ArtifactsUrlClassification classify(final ClassPathClassifierContext context) {
    checkNotNull(context, "context cannot be null");

    logger.debug("Building class loaders for rootArtifact: {}", context.getRootArtifact());

    List<Dependency> directDependencies;
    try {
      directDependencies = dependencyResolver.getDirectDependencies(context.getRootArtifact());
    } catch (Exception e) {
      throw new IllegalStateException("Couldn't get direct dependencies for rootArtifact: '" + context.getRootArtifact() + "'",
                                      e);
    }

    ArtifactClassificationType rootArtifactType = artifactClassificationTypeResolver
        .resolveArtifactClassificationType(context.getRootArtifact());
    if (rootArtifactType == null) {
      throw new IllegalStateException("Couldn't be identified type for rootArtifact: " + context.getRootArtifact());
    }
    logger.debug("rootArtifact {} identified as {} type", context.getRootArtifact(), rootArtifactType);

    List<RemoteRepository> remoteRepositories = null;
    try {
      remoteRepositories = dependencyResolver
          .readArtifactDescriptor(context.getRootArtifact())
          .getRepositories();
    } catch (ArtifactDescriptorException e) {
      throw new IllegalStateException("Couldn't read rootArtifact descriptor", e);
    }
    List<URL> applicationSharedLibUrls =
        buildApplicationSharedLibUrlClassification(context, directDependencies, remoteRepositories);
    List<URL> applicationLibUrls = buildApplicationUrlClassification(context, directDependencies, remoteRepositories);
    applicationLibUrls.removeAll(applicationSharedLibUrls);

    List<PluginUrlClassification> pluginUrlClassifications =
        buildPluginUrlClassifications(context, directDependencies, rootArtifactType, remoteRepositories);

    List<ArtifactUrlClassification> serviceUrlClassifications =
        buildServicesUrlClassification(context, directDependencies, remoteRepositories);

    List<URL> containerUrls =
        buildContainerUrlClassification(context, directDependencies, serviceUrlClassifications, pluginUrlClassifications,
                                        rootArtifactType, remoteRepositories);
    List<URL> testRunnerLibUrls =
        buildTestRunnerUrlClassification(context, directDependencies, rootArtifactType, remoteRepositories);

    List<URL> testRunnerExportedLibUrls =
        buildTestRunnerExportedLibUrlClassification(context, directDependencies, remoteRepositories);

    return new ArtifactsUrlClassification(containerUrls, serviceUrlClassifications, testRunnerLibUrls, applicationLibUrls,
                                          applicationSharedLibUrls, pluginUrlClassifications, testRunnerExportedLibUrls);
  }

  /**
   * Finds direct dependencies declared with classifier {@value #MULE_SERVICE_CLASSIFIER}. Creates a List of
   * {@link ArtifactUrlClassification} for each service including their {@code compile} scope dependencies.
   * <p/>
   * {@value #SERVICE_PROVIDER_CLASS_NAME} will be used as {@link ArtifactClassLoader#getArtifactId()}
   * <p/>
   * Once identified and classified these Maven artifacts will be excluded from container classification.
   *
   * @param context {@link ClassPathClassifierContext} with settings for the classification process
   * @param directDependencies {@link List} of {@link Dependency} with direct dependencies for the rootArtifact
   * @param rootArtifactRemoteRepositories remote repositories defined at the rootArtifact.
   * @return a {@link List} of {@link ArtifactUrlClassification}s that would be the one used for the plugins class loaders.
   */
  private List<ArtifactUrlClassification> buildServicesUrlClassification(final ClassPathClassifierContext context,
                                                                         final List<Dependency> directDependencies,
                                                                         final List<RemoteRepository> rootArtifactRemoteRepositories) {
    Map<String, ArtifactClassificationNode> servicesClassified = newLinkedHashMap();

    final Predicate<Dependency> muleServiceClassifiedDependencyFilter =
        dependency -> dependency.getArtifact().getClassifier().equals(MULE_SERVICE_CLASSIFIER);
    List<Artifact> serviceArtifactsDeclared = filterArtifacts(directDependencies,
                                                              muleServiceClassifiedDependencyFilter);
    logger.debug("{} services defined to be classified", serviceArtifactsDeclared.size());

    serviceArtifactsDeclared.stream()
        .forEach(serviceArtifact -> buildPluginUrlClassification(serviceArtifact, context,
                                                                 muleServiceClassifiedDependencyFilter, servicesClassified,
                                                                 rootArtifactRemoteRepositories));

    return toServiceUrlClassification(servicesClassified.values());

  }

  /**
   * Classifies the {@link List} of {@link URL}s from {@value org.eclipse.aether.util.artifact.JavaScopes#TEST} scope direct
   * dependencies to be added as plugin runtime shared libraries.
   *
   * @param context {@link ClassPathClassifierContext} with settings for the classification process
   * @param directDependencies {@link List} of {@link Dependency} with direct dependencies for the rootArtifact
   * @param rootArtifactRemoteRepositories remote repositories defined at rootArtifact.
   * @return {@link List} of {@link URL}s to be added to runtime shared libraries.
   */
  private List<URL> buildApplicationSharedLibUrlClassification(final ClassPathClassifierContext context,
                                                               final List<Dependency> directDependencies,
                                                               final List<RemoteRepository> rootArtifactRemoteRepositories) {
    List<URL> pluginSharedLibUrls = newArrayList();

    List<Dependency> pluginSharedLibDependencies = context.getApplicationSharedLibCoordinates().stream()
        .map(sharedPluginLibCoords -> findApplicationSharedLibArtifact(sharedPluginLibCoords, context.getRootArtifact(),
                                                                       directDependencies))
        .collect(toList());

    logger.debug("Plugin application shared lib artifacts matched with versions from direct dependencies declared: {}",
                 pluginSharedLibDependencies);

    pluginSharedLibDependencies.stream()
        .map(pluginSharedLibDependency -> {
          try {
            return dependencyResolver.resolveArtifact(pluginSharedLibDependency.getArtifact(), rootArtifactRemoteRepositories)
                .getArtifact().getFile().toURI().toURL();
          } catch (Exception e) {
            throw new IllegalStateException("Error while resolving dependency '" + pluginSharedLibDependency
                + "' as application shared lib", e);
          }
        })
        .forEach(pluginSharedLibUrls::add);

    resolveSnapshotVersionsToTimestampedFromClassPath(pluginSharedLibUrls, context.getClassPathURLs());

    logger.debug("Classified URLs as application shared libraries: '{}", pluginSharedLibUrls);
    return pluginSharedLibUrls;
  }

  private List<URL> buildTestRunnerExportedLibUrlClassification(final ClassPathClassifierContext context,
                                                                final List<Dependency> directDependencies,
                                                                final List<RemoteRepository> rootArtifactRemoteRepositories) {
    List<URL> pluginSharedLibUrls = newArrayList();

    List<Dependency> testRunnerExportedLibDependencies = context.getTestRunnerExportedLibCoordinates().stream()
        .map(sharedPluginLibCoords -> findTestRunnerExportedLibArtifact(sharedPluginLibCoords, context.getRootArtifact(),
                                                                        directDependencies))
        .collect(toList());

    logger.debug("Test runner exported lib artifacts matched with versions from direct dependencies declared: {}",
                 testRunnerExportedLibDependencies);

    testRunnerExportedLibDependencies.stream()
        .map(testRunnerExportedLibDependency -> {
          try {
            return dependencyResolver
                .resolveArtifact(testRunnerExportedLibDependency.getArtifact(), rootArtifactRemoteRepositories)
                .getArtifact().getFile().toURI().toURL();
          } catch (Exception e) {
            throw new IllegalStateException("Error while resolving dependency '" + testRunnerExportedLibDependency
                + "' as test runner exported lib", e);
          }
        })
        .forEach(pluginSharedLibUrls::add);

    resolveSnapshotVersionsToTimestampedFromClassPath(pluginSharedLibUrls, context.getClassPathURLs());

    logger.debug("Classified URLs as test runner exported libraries: '{}", pluginSharedLibUrls);
    return pluginSharedLibUrls;
  }

  private List<URL> buildApplicationUrlClassification(final ClassPathClassifierContext context,
                                                      final List<Dependency> directDependencies,
                                                      final List<RemoteRepository> rootArtifactRemoteRepositories) {
    List<URL> applicationLibUrls = newArrayList();

    List<Dependency> applicationLibDependencies = context.getApplicationLibCoordinates().stream()
        .map(applicationLibCoords -> findApplicationLibArtifact(applicationLibCoords, context.getRootArtifact(),
                                                                directDependencies))
        .collect(toList());

    logger.debug("Application lib artifacts matched with versions from direct dependencies declared: {}",
                 applicationLibDependencies);

    applicationLibDependencies.stream()
        .map(pluginSharedLibDependency -> {
          try {
            return dependencyResolver.resolveArtifact(pluginSharedLibDependency.getArtifact(), rootArtifactRemoteRepositories)
                .getArtifact().getFile().toURI().toURL();
          } catch (Exception e) {
            throw new IllegalStateException("Error while resolving dependency '" + pluginSharedLibDependency
                + "' as application lib", e);
          }
        })
        .forEach(applicationLibUrls::add);

    resolveSnapshotVersionsToTimestampedFromClassPath(applicationLibUrls, context.getClassPathURLs());

    logger.debug("Classified URLs as application runtime libraries: '{}", applicationLibUrls);
    return applicationLibUrls;
  }

  /**
   * Container classification is being done by resolving the {@value org.eclipse.aether.util.artifact.JavaScopes#PROVIDED} direct
   * dependencies of the rootArtifact. Is uses the exclusions defined in
   * {@link ClassPathClassifierContext#getProvidedExclusions()} to filter the dependency graph plus
   * {@link ClassPathClassifierContext#getExcludedArtifacts()}.
   * <p/>
   * In order to resolve correctly the {@value org.eclipse.aether.util.artifact.JavaScopes#PROVIDED} direct dependencies it will
   * get for each one the manage dependencies and use that list to resolve the graph.
   *
   * @param context {@link ClassPathClassifierContext} with settings for the classification process
   * @param pluginUrlClassifications {@link PluginUrlClassification}s to check if rootArtifact was classified as plugin
   * @param rootArtifactType {@link ArtifactClassificationType} for rootArtifact
   * @param rootArtifactRemoteRepositories remote repositories defined at the rootArtifact
   * @return {@link List} of {@link URL}s for the container class loader
   */
  private List<URL> buildContainerUrlClassification(ClassPathClassifierContext context,
                                                    List<Dependency> directDependencies,
                                                    List<ArtifactUrlClassification> serviceUrlClassifications,
                                                    List<PluginUrlClassification> pluginUrlClassifications,
                                                    ArtifactClassificationType rootArtifactType,
                                                    List<RemoteRepository> rootArtifactRemoteRepositories) {
    directDependencies = directDependencies.stream()
        .filter(getContainerDirectDependenciesFilter(rootArtifactType))
        .filter(dependency -> {
          Artifact artifact = dependency.getArtifact();
          return (!serviceUrlClassifications.stream()
              .filter(artifactUrlClassification -> artifactUrlClassification.getArtifactId().equals(toId(artifact))).findAny()
              .isPresent()
              && !pluginUrlClassifications.stream()
                  .filter(artifactUrlClassification -> artifactUrlClassification.getArtifactId().equals(toId(artifact))).findAny()
                  .isPresent());
        })
        .map(depToTransform -> depToTransform.setScope(COMPILE))
        .collect(toList());

    // Add logging dependencies to avoid every module from having to declare this dependency.
    // This brings the slf4j bridges required by transitive dependencies of the container to its classpath
    // TODO MULE-10837 Externalize this dependency along with the other commonly used container dependencies.
    directDependencies
        .add(new Dependency(new DefaultArtifact(RUNTIME_GROUP_ID, LOGGING_ARTIFACT_ID, JAR_EXTENSION, muleVersion), COMPILE));

    logger.debug("Selected direct dependencies to be used for resolving container dependency graph (changed to compile in " +
        "order to resolve the graph): {}", directDependencies);

    Set<Dependency> managedDependencies =
        selectContainerManagedDependencies(context, directDependencies, rootArtifactType, rootArtifactRemoteRepositories);

    logger.debug("Collected managed dependencies from direct provided dependencies to be used for resolving container "
        + "dependency graph: {}", managedDependencies);

    List<String> excludedFilterPattern = newArrayList(context.getProvidedExclusions());
    excludedFilterPattern.addAll(context.getExcludedArtifacts());
    if (!pluginUrlClassifications.isEmpty()) {
      excludedFilterPattern.addAll(pluginUrlClassifications.stream()
          .map(pluginUrlClassification -> pluginUrlClassification.getArtifactId())
          .collect(toList()));
    }
    if (!serviceUrlClassifications.isEmpty()) {
      excludedFilterPattern.addAll(serviceUrlClassifications.stream()
          .map(serviceUrlClassification -> serviceUrlClassification.getArtifactId())
          .collect(toList()));
    }

    logger.debug("Resolving dependencies for container using exclusion filter patterns: {}", excludedFilterPattern);

    final DependencyFilter dependencyFilter = new PatternExclusionsDependencyFilter(excludedFilterPattern);

    List<URL> containerUrls;
    try {
      containerUrls = toUrl(dependencyResolver.resolveDependencies(null, directDependencies, newArrayList(managedDependencies),
                                                                   dependencyFilter,
                                                                   rootArtifactRemoteRepositories));
    } catch (Exception e) {
      throw new IllegalStateException("Couldn't resolve dependencies for Container", e);
    }
    containerUrls = containerUrls.stream().filter(url -> {
      String file = toFile(url).getAbsolutePath();
      return !(endsWithIgnoreCase(file, POM_XML) || endsWithIgnoreCase(file, POM_EXTENSION) || endsWithIgnoreCase(file,
                                                                                                                  ZIP_EXTENSION));
    }).collect(toList());

    if (MODULE.equals(rootArtifactType)) {
      File rootArtifactOutputFile = resolveRootArtifactFile(context.getRootArtifact());
      if (rootArtifactOutputFile == null) {
        throw new IllegalStateException("rootArtifact (" + context.getRootArtifact()
            + ") identified as MODULE but doesn't have an output");
      }
      containerUrls.add(0, toUrl(rootArtifactOutputFile));
    }

    resolveSnapshotVersionsToTimestampedFromClassPath(containerUrls, context.getClassPathURLs());

    return containerUrls;
  }

  /**
   * Creates the {@link Set} of {@link Dependency} to be used as managed dependencies when resolving Container dependencies. If
   * the rootArtifact is a {@link ArtifactClassificationType#MODULE} it will use its managed dependencies, other case it collects
   * managed dependencies for each direct dependencies selected for Container.
   *
   * @param context {@link ClassPathClassifierContext} with settings for the classification process
   * @param directDependencies {@link List} of {@link Dependency} with direct dependencies for the rootArtifact
   * @param rootArtifactType {@link ArtifactClassificationType} for rootArtifact
   * @param rootArtifactRemoteRepositories repositories to be used when reading artifact descriptors
   * @return {@link Set} of {@link Dependency} to be used as managed dependencies when resolving Container dependencies
   */
  private Set<Dependency> selectContainerManagedDependencies(ClassPathClassifierContext context,
                                                             List<Dependency> directDependencies,
                                                             ArtifactClassificationType rootArtifactType,
                                                             List<RemoteRepository> rootArtifactRemoteRepositories) {
    Set<Dependency> managedDependencies;
    if (!rootArtifactType.equals(MODULE)) {
      managedDependencies = directDependencies.stream()
          .map(directDep -> {
            try {
              ArtifactDescriptorResult readArtifactDescriptor =
                  dependencyResolver.readArtifactDescriptor(directDep.getArtifact(), rootArtifactRemoteRepositories);
              return readArtifactDescriptor == null ? Collections.<Dependency>emptyList()
                  : readArtifactDescriptor.getManagedDependencies();
            } catch (ArtifactDescriptorException e) {
              throw new IllegalStateException("Couldn't read artifact: '" + directDep.getArtifact() +
                  "' while collecting managed dependencies for Container", e);
            }
          })
          .flatMap(l -> l.stream())
          .collect(toSet());
    } else {
      try {
        managedDependencies = newHashSet(dependencyResolver.readArtifactDescriptor(context.getRootArtifact())
            .getManagedDependencies());
      } catch (ArtifactDescriptorException e) {
        throw new IllegalStateException("Couldn't collect managed dependencies for rootArtifact (" + context.getRootArtifact()
            + ")", e);
      }
    }
    return managedDependencies;
  }

  /**
   * Gets the direct dependencies filter to be used when collecting Container dependencies. If the rootArtifact is a
   * {@link ArtifactClassificationType#MODULE} it will include {@value org.eclipse.aether.util.artifact.JavaScopes#COMPILE}
   * dependencies too if not just {@value org.eclipse.aether.util.artifact.JavaScopes#PROVIDED}.
   *
   * @param rootArtifactType the {@link ArtifactClassificationType} for rootArtifact
   * @return {@link Predicate} for selecting direct dependencies for the Container.
   */
  private Predicate<Dependency> getContainerDirectDependenciesFilter(ArtifactClassificationType rootArtifactType) {
    return rootArtifactType.equals(MODULE)
        ? directDep -> directDep.getScope().equals(PROVIDED) || directDep.getScope().equals(COMPILE)
        : directDep -> directDep.getScope().equals(PROVIDED)
            || directDep.getArtifact().getClassifier().equals(MULE_PLUGIN_CLASSIFIER);
  }

  /**
   * Plugin classifications are being done by resolving the dependencies for each plugin coordinates defined by the rootArtifact
   * direct dependencies as {@value #MULE_SERVICE_CLASSIFIER}.
   * <p/>
   * While resolving the dependencies for the plugin artifact, only {@value org.eclipse.aether.util.artifact.JavaScopes#COMPILE}
   * dependencies will be selected. {@link ClassPathClassifierContext#getExcludedArtifacts()} will be exluded too.
   * <p/>
   * The resulting {@link PluginUrlClassification} for each plugin will have as name the Maven artifact id coordinates:
   * {@code <groupId>:<artifactId>:<extension>[:<classifier>]:<version>}.
   *
   * @param context {@link ClassPathClassifierContext} with settings for the classification process
   * @param directDependencies {@link List} of {@link Dependency} with direct dependencies for the rootArtifact
   * @param rootArtifactType {@link ArtifactClassificationType} for rootArtifact
   * @param rootArtifactRemoteRepositories remote repositories defined at the rootArtifact
   * @return {@link List} of {@link PluginUrlClassification}s for plugins class loaders
   */
  private List<PluginUrlClassification> buildPluginUrlClassifications(ClassPathClassifierContext context,
                                                                      List<Dependency> directDependencies,
                                                                      ArtifactClassificationType rootArtifactType,
                                                                      List<RemoteRepository> rootArtifactRemoteRepositories) {
    Map<String, ArtifactClassificationNode> pluginsClassified = newLinkedHashMap();

    Artifact rootArtifact = context.getRootArtifact();

    List<Artifact> pluginsArtifacts = directDependencies.stream()
        .filter(dependency -> dependency.getArtifact().getClassifier().equals(MULE_PLUGIN_CLASSIFIER))
        .map(dependency -> dependency.getArtifact())
        .collect(toList());

    logger.debug("{} plugins defined to be classified", pluginsArtifacts.size());

    Predicate<Dependency> mulePluginDependencyFilter =
        dependency -> dependency.getArtifact().getClassifier().equals(MULE_PLUGIN_CLASSIFIER)
            && dependency.getScope().equals(COMPILE);
    if (PLUGIN.equals(rootArtifactType)) {
      logger.debug("rootArtifact '{}' identified as Mule plugin", rootArtifact);
      buildPluginUrlClassification(rootArtifact, context, mulePluginDependencyFilter, pluginsClassified,
                                   rootArtifactRemoteRepositories);

      pluginsArtifacts = pluginsArtifacts.stream()
          .filter(pluginArtifact -> !(rootArtifact.getGroupId().equals(pluginArtifact.getGroupId())
              && rootArtifact.getArtifactId().equals(pluginArtifact.getArtifactId())))
          .collect(toList());
    }

    pluginsArtifacts.stream()
        .forEach(pluginArtifact -> buildPluginUrlClassification(pluginArtifact, context, mulePluginDependencyFilter,
                                                                pluginsClassified, rootArtifactRemoteRepositories));

    if (context.isExtensionMetadataGenerationEnabled()) {
      ExtensionPluginMetadataGenerator extensionPluginMetadataGenerator =
          new ExtensionPluginMetadataGenerator(context.getPluginResourcesFolder());


      for (ArtifactClassificationNode pluginClassifiedNode : pluginsClassified.values()) {
        List<URL> urls = generateExtensionMetadata(pluginClassifiedNode.getArtifact(), context, extensionPluginMetadataGenerator,
                                                   pluginClassifiedNode.getUrls(), rootArtifactRemoteRepositories);
        pluginClassifiedNode.setUrls(urls);
      }
    }
    return toPluginUrlClassification(pluginsClassified.values());
  }

  /**
   * Transforms the {@link ArtifactClassificationNode} to {@link ArtifactsUrlClassification}.
   *
   * @param classificationNodes the fat object classified that needs to be transformed
   * @return {@link ArtifactsUrlClassification}
   */
  private List<ArtifactUrlClassification> toServiceUrlClassification(Collection<ArtifactClassificationNode> classificationNodes) {
    return classificationNodes.stream().map(node -> {
      InputStream servicePropertiesStream =
          new URLClassLoader(node.getUrls().toArray(new URL[0]), null).getResourceAsStream(SERVICE_PROPERTIES_FILE_NAME);

      // TODO(pablo.kraan): MULE-13281 - remove properties descriptor support once all the services are migrated to the new file
      // format
      if (servicePropertiesStream != null) {
        try {
          Properties serviceProperties = loadProperties(servicePropertiesStream);
          String serviceProviderClassName = serviceProperties.getProperty(SERVICE_PROVIDER_CLASS_NAME);
          logger.debug("Discover serviceProviderClassName: {} for artifact: {}", serviceProviderClassName, node.getArtifact());
          if (node.getExportClasses() != null && !node.getExportClasses().isEmpty()) {
            logger.warn("exportClasses is not supported for services artifacts, they are going to be ignored");
          }
          return new ArtifactUrlClassification(toId(node.getArtifact()), serviceProviderClassName, node.getUrls());
        } catch (IOException e) {
          throw new IllegalArgumentException("Couldn't read " + SERVICE_PROPERTIES_FILE_NAME + " for artifact: "
              + node.getArtifact(), e);
        }
      } else {
        final String classifierLessId = toId(node.getArtifact());
        return serviceResourcesResolver
            .resolveServiceResourcesFor(new ArtifactUrlClassification(classifierLessId, "zaraza", node.getUrls()));
      }
    }).collect(toList());
  }

  /**
   * Transforms the {@link ArtifactClassificationNode} to {@link PluginUrlClassification}.
   *
   * @param classificationNodes the fat object classified that needs to be transformed
   * @return {@link PluginUrlClassification}
   */
  private List<PluginUrlClassification> toPluginUrlClassification(Collection<ArtifactClassificationNode> classificationNodes) {

    Map<String, PluginUrlClassification> classifiedPluginUrls = newLinkedHashMap();

    for (ArtifactClassificationNode node : classificationNodes) {
      final List<String> pluginDependencies = node.getArtifactDependencies().stream()
          .map(dependency -> toId(dependency.getArtifact()))
          .collect(toList());
      final String classifierLessId = toId(node.getArtifact());
      final PluginUrlClassification pluginUrlClassification =
          pluginResourcesResolver.resolvePluginResourcesFor(
                                                            new PluginUrlClassification(classifierLessId, node.getUrls(),
                                                                                        node.getExportClasses(),
                                                                                        pluginDependencies));

      classifiedPluginUrls.put(classifierLessId, pluginUrlClassification);
    }

    for (PluginUrlClassification pluginUrlClassification : classifiedPluginUrls.values()) {
      for (String dependency : pluginUrlClassification.getPluginDependencies()) {
        final PluginUrlClassification dependencyPlugin = classifiedPluginUrls.get(dependency);
        if (dependencyPlugin == null) {
          throw new IllegalStateException("Unable to find a plugin dependency: " + dependency);
        }

        pluginUrlClassification.getExportedPackages().removeAll(dependencyPlugin.getExportedPackages());
      }
    }

    return newArrayList(classifiedPluginUrls.values());
  }

  /**
   * Classifies an {@link Artifact} recursively. {@value org.eclipse.aether.util.artifact.JavaScopes#COMPILE} dependencies will be
   * resolved for building the {@link URL}'s for the class loader. Once classified the node is added to {@link Map} of
   * artifactsClassified.
   *
   * @param artifactToClassify {@link Artifact} that represents the artifact to be classified
   * @param context {@link ClassPathClassifierContext} with settings for the classification process
   * @param artifactsClassified {@link Map} that contains already classified plugins
   * @param rootArtifactRemoteRepositories remote repositories defined at the root artifact.
   */
  private void buildPluginUrlClassification(Artifact artifactToClassify, ClassPathClassifierContext context,
                                            Predicate<Dependency> directDependenciesFilter,
                                            Map<String, ArtifactClassificationNode> artifactsClassified,
                                            List<RemoteRepository> rootArtifactRemoteRepositories) {

    List<URL> urls;
    try {
      final DependencyFilter dependencyFilter = andFilter(classpathFilter(COMPILE),
                                                          new PatternExclusionsDependencyFilter(context.getExcludedArtifacts()),
                                                          orFilter(new PatternExclusionsDependencyFilter("*:*:*:"
                                                              + MULE_PLUGIN_CLASSIFIER + ":*"),
                                                                   new PatternInclusionsDependencyFilter(toId(artifactToClassify))));
      urls = toUrl(dependencyResolver.resolveDependencies(new Dependency(artifactToClassify, COMPILE),
                                                          emptyList(), emptyList(),
                                                          dependencyFilter, rootArtifactRemoteRepositories));
    } catch (Exception e) {
      throw new IllegalStateException("Couldn't resolve dependencies for artifact: '" + artifactToClassify + "' classification",
                                      e);
    }

    List<Dependency> directDependencies;
    List<ArtifactClassificationNode> artifactDependencies = newArrayList();
    try {
      directDependencies = dependencyResolver.getDirectDependencies(artifactToClassify, rootArtifactRemoteRepositories);
    } catch (ArtifactDescriptorException e) {
      throw new IllegalStateException("Couldn't get direct dependencies for artifact: '" + artifactToClassify + "'", e);
    }
    logger.debug("Searching for dependencies on direct dependencies of artifact {}", artifactToClassify);
    List<Artifact> pluginArtifactDependencies = filterArtifacts(directDependencies, directDependenciesFilter);
    logger.debug("Artifacts {} identified a plugin dependencies for plugin {}", pluginArtifactDependencies, artifactToClassify);
    pluginArtifactDependencies.stream()
        .map(artifact -> {
          String artifactClassifierLessId = toId(artifact);
          if (!artifactsClassified.containsKey(artifactClassifierLessId)) {
            buildPluginUrlClassification(artifact, context, directDependenciesFilter, artifactsClassified,
                                         rootArtifactRemoteRepositories);
          }
          return artifactsClassified.get(artifactClassifierLessId);
        })
        .forEach(artifactDependencies::add);

    final List<Class> exportClasses = getArtifactExportedClasses(artifactToClassify, context, rootArtifactRemoteRepositories);

    resolveSnapshotVersionsToTimestampedFromClassPath(urls, context.getClassPathURLs());

    ArtifactClassificationNode artifactUrlClassification = new ArtifactClassificationNode(artifactToClassify,
                                                                                          urls,
                                                                                          exportClasses,
                                                                                          artifactDependencies);

    artifactsClassified.put(toId(artifactToClassify), artifactUrlClassification);
  }

  /**
   * Resolves the exported plugin classes from the given {@link Artifact}
   *
   * @param exporterArtifact {@link Artifact} used to resolve the exported classes
   * @param context {@link ClassPathClassifierContext} with settings for the classification process
   * @param rootArtifactRemoteRepositories remote repositories defined by the rootArtifact
   * @return {@link List} of {@link Class} that the given {@link Artifact} exports
   */
  private List<Class> getArtifactExportedClasses(Artifact exporterArtifact, ClassPathClassifierContext context,
                                                 List<RemoteRepository> rootArtifactRemoteRepositories) {
    final AtomicReference<URL> artifactUrl = new AtomicReference<>();
    try {
      artifactUrl.set(dependencyResolver.resolveArtifact(exporterArtifact, rootArtifactRemoteRepositories).getArtifact().getFile()
          .toURI().toURL());
    } catch (MalformedURLException | ArtifactResolutionException e) {
      throw new IllegalStateException("Unable to resolve artifact URL", e);
    }
    Artifact rootArtifact = context.getRootArtifact();

    return context.getExportPluginClasses().stream()
        .filter(clazz -> {
          boolean isFromCurrentArtifact = clazz.getProtectionDomain().getCodeSource().getLocation().equals(artifactUrl.get());
          if (isFromCurrentArtifact && exporterArtifact != rootArtifact) {
            logger.warn("Exported class '{}' from plugin '{}' is being used from another artifact, {}", clazz.getSimpleName(),
                        exporterArtifact, rootArtifact);
          }
          return isFromCurrentArtifact;
        })
        .collect(toList());
  }

  /**
   * Collects from the list of directDependencies {@link Dependency} those that are classified with classifier specified.
   *
   * @param directDependencies {@link List} of direct {@link Dependency}
   * @return {@link List} of {@link Artifact}s for those dependencies classified as with the give classifier, can be empty.
   */
  private List<Artifact> filterArtifacts(List<Dependency> directDependencies, Predicate<Dependency> filter) {
    return directDependencies.stream()
        .filter(dependency -> filter.test(dependency))
        .map(dependency -> dependency.getArtifact())
        .collect(toList());
  }

  /**
   * If enabled generates the Extension metadata and returns the {@link List} of {@link URL}s with the folder were metadata is
   * generated as first entry in the list.
   *
   * @param plugin plugin {@link Artifact} to generate its Extension metadata
   * @param context {@link ClassPathClassifierContext} with settings for the classification process
   * @param pluginGenerator {@link ExtensionPluginMetadataGenerator} extensions metadata generator
   * @param urls current {@link List} of {@link URL}s classified for the plugin
   * @param rootArtifactRemoteRepositories remote repositories defined at the rootArtifact
   * @return {@link List} of {@link URL}s classified for the plugin
   */
  private List<URL> generateExtensionMetadata(Artifact plugin, ClassPathClassifierContext context,
                                              ExtensionPluginMetadataGenerator pluginGenerator,
                                              List<URL> urls, List<RemoteRepository> rootArtifactRemoteRepositories) {
    Class extensionClass = pluginGenerator.scanForExtensionAnnotatedClasses(plugin, urls);
    if (extensionClass != null) {
      logger.debug("Plugin '{}' has been discovered as Extension", plugin);
      if (context.isExtensionMetadataGenerationEnabled()) {
        File generatedMetadataFolder = pluginGenerator.generateExtensionResources(plugin, extensionClass, dependencyResolver,
                                                                                  rootArtifactRemoteRepositories);
        URL generatedTestResources = toUrl(generatedMetadataFolder);

        List<URL> appendedTestResources = newArrayList(generatedTestResources);
        appendedTestResources.addAll(urls);
        urls = appendedTestResources;
      }
    }
    return urls;
  }

  /**
   * Finds the direct {@link Dependency} from rootArtifact for the given groupId and artifactId.
   *
   * @param groupId of the artifact to be found
   * @param artifactId of the artifact to be found
   * @param directDependencies the rootArtifact direct {@link Dependency}s
   * @param classifier
   * @return {@link Optional} {@link Dependency} to the dependency. Could be empty it if not present in the list of direct
   *         dependencies
   */
  private Optional<Dependency> findDirectDependency(String groupId, String artifactId, List<Dependency> directDependencies,
                                                    Optional<String> classifier) {
    return directDependencies.isEmpty() ? Optional.<Dependency>empty()
        : directDependencies.stream().filter(
                                             dependency -> dependency.getArtifact().getGroupId().equals(groupId)
                                                 && dependency.getArtifact().getArtifactId().equals(artifactId)
                                                 && ((classifier.isPresent()
                                                     && dependency.getArtifact().getClassifier().equals(classifier.get())
                                                     || !classifier.isPresent())))
            .findFirst();
  }

  /**
   * Finds the plugin shared lib {@link Dependency} from the direct dependencies of the rootArtifact.
   *
   * @param pluginSharedLibCoords Maven coordinates that define the plugin shared lib artifact
   * @param rootArtifact {@link Artifact} that defines the current artifact that requested to build this class loaders
   * @param directDependencies {@link List} of {@link Dependency} with direct dependencies for the rootArtifact
   * @return {@link Artifact} representing the plugin shared lib artifact
   */
  private Dependency findApplicationSharedLibArtifact(String pluginSharedLibCoords, Artifact rootArtifact,
                                                      List<Dependency> directDependencies) {
    Optional<Dependency> pluginSharedLibDependency = discoverDependency(pluginSharedLibCoords, rootArtifact, directDependencies);
    if (!pluginSharedLibDependency.isPresent()) {
      throw new IllegalStateException("Application shared lib artifact '" + pluginSharedLibCoords +
          "' in order to be resolved has to be declared as " + TEST + " dependency of your Maven project (" + rootArtifact + ")");
    }

    return pluginSharedLibDependency.get();
  }

  private Dependency findApplicationLibArtifact(String pluginSharedLibCoords, Artifact rootArtifact,
                                                List<Dependency> directDependencies) {
    Optional<Dependency> pluginSharedLibDependency = discoverDependency(pluginSharedLibCoords, rootArtifact, directDependencies);
    if (!pluginSharedLibDependency.isPresent()) {
      throw new IllegalStateException("Application lib artifact '" + pluginSharedLibCoords +
          "' in order to be resolved has to be declared as " + TEST + " dependency of your Maven project (" + rootArtifact + ")");
    }

    return pluginSharedLibDependency.get();
  }

  private Dependency findTestRunnerExportedLibArtifact(String testRunnerExportedLbCoords, Artifact rootArtifact,
                                                       List<Dependency> directDependencies) {
    Optional<Dependency> pluginSharedLibDependency =
        discoverDependency(testRunnerExportedLbCoords, rootArtifact, directDependencies);
    if (!pluginSharedLibDependency.isPresent()) {
      throw new IllegalStateException("Test runner exported lib artifact '" + testRunnerExportedLbCoords +
          "' in order to be resolved has to be declared as " + TEST + " dependency of your Maven project (" + rootArtifact + ")");
    }

    return pluginSharedLibDependency.get();
  }

  /**
   * Discovers the {@link Dependency} from the list of directDependencies using the artifact coordiantes in format of:
   *
   * <pre>
   * groupId:artifactId
   * </pre>
   * <p/>
   * If the coordinates matches to the rootArtifact it will return a {@value org.eclipse.aether.util.artifact.JavaScopes#COMPILE}
   * {@link Dependency}.
   *
   * @param artifactCoords Maven coordinates that define the artifact dependency
   * @param rootArtifact {@link Artifact} that defines the current artifact that requested to build this class loaders
   * @param directDependencies {@link List} of {@link Dependency} with direct dependencies for the rootArtifact
   * @return {@link Dependency} representing the artifact if declared as direct dependency or rootArtifact if they match it or
   *         {@link Optional#EMPTY} if couldn't found the dependency.
   * @throws {@link IllegalArgumentException} if artifactCoords are not in the expected format
   */
  public Optional<Dependency> discoverDependency(String artifactCoords, Artifact rootArtifact,
                                                 List<Dependency> directDependencies) {
    final String[] artifactCoordsSplit = artifactCoords.split(MAVEN_COORDINATES_SEPARATOR);
    if (artifactCoordsSplit.length < 2 || artifactCoordsSplit.length > 3) {
      throw new IllegalArgumentException("Artifact coordinates should be in format of groupId:artifactId or groupId:artifactId:classifier, '"
          + artifactCoords +
          "' is not a valid format");
    }
    String groupId = artifactCoordsSplit[0];
    String artifactId = artifactCoordsSplit[1];
    Optional<String> classifier = artifactCoordsSplit.length > 2 ? of(artifactCoordsSplit[2]) : empty();

    if (rootArtifact.getGroupId().equals(groupId) && rootArtifact.getArtifactId().equals(artifactId)) {
      logger.debug("'{}' artifact coordinates matched with rootArtifact '{}', resolving version from rootArtifact",
                   artifactCoords, rootArtifact);
      final DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, JAR_EXTENSION, rootArtifact.getVersion());
      logger.debug("'{}' artifact coordinates resolved to: '{}'", artifactCoords, artifact);
      return of(new Dependency(artifact, COMPILE));

    } else {
      logger.debug("Resolving version for '{}' from direct dependencies", artifactCoords);
      return findDirectDependency(groupId, artifactId, directDependencies, classifier);
    }

  }

  /**
   * Application classification is being done by resolving the direct dependencies with scope
   * {@value org.eclipse.aether.util.artifact.JavaScopes#TEST} for the rootArtifact. Due to Eclipse Aether resolution excludes by
   * {@value org.eclipse.aether.util.artifact.JavaScopes#TEST} dependencies an imaginary pom will be created with these
   * dependencies as {@value org.eclipse.aether.util.artifact.JavaScopes#COMPILE} so the dependency graph can be resolved (with
   * the same results as it will be obtained from Maven).
   * <p/>
   * If the rootArtifact was classified as plugin its {@value org.eclipse.aether.util.artifact.JavaScopes#COMPILE} will be changed
   * to {@value org.eclipse.aether.util.artifact.JavaScopes#PROVIDED} in order to exclude them from the dependency graph.
   * <p/>
   * Filtering logic includes the following pattern to includes the patterns defined at
   * {@link ClassPathClassifierContext#getTestInclusions()}. It also excludes
   * {@link ClassPathClassifierContext#getExcludedArtifacts()}, {@link ClassPathClassifierContext#getTestExclusions()}.
   * <p/>
   * If the application artifact has not been classified as plugin its going to be resolved as {@value #JAR_EXTENSION} in order to
   * include this its compiled classes classification.
   *
   * @param context {@link ClassPathClassifierContext} with settings for the classification process
   * @param directDependencies {@link List} of {@link Dependency} with direct dependencies for the rootArtifact
   * @param rootArtifactType {@link ArtifactClassificationType} for rootArtifact @return {@link URL}s for application class loader
   * @param rootArtifactRemoteRepositories remote repositories defined at the rootArtifact
   */
  private List<URL> buildTestRunnerUrlClassification(ClassPathClassifierContext context,
                                                     List<Dependency> directDependencies,
                                                     ArtifactClassificationType rootArtifactType,
                                                     List<RemoteRepository> rootArtifactRemoteRepositories) {
    logger.debug("Building application classification");
    Artifact rootArtifact = context.getRootArtifact();

    DependencyFilter dependencyFilter = new PatternInclusionsDependencyFilter(context.getTestInclusions());
    logger.debug("Using filter for dependency graph to include: '{}'", context.getTestInclusions());

    List<File> appFiles = newArrayList();
    List<String> exclusionsPatterns = newArrayList();

    if (APPLICATION.equals(rootArtifactType)) {
      logger.debug("RootArtifact identified as {} so is going to be added to application classification", APPLICATION);
      File rootArtifactOutputFile = resolveRootArtifactFile(rootArtifact);
      if (rootArtifactOutputFile != null) {
        appFiles.add(rootArtifactOutputFile);
      } else {
        logger.warn("rootArtifact '{}' identified as {} but doesn't have an output {} file", rootArtifact, rootArtifactType,
                    JAR_EXTENSION);
      }
    } else {
      logger.debug("RootArtifact already classified as plugin or module, excluding it from application classification");
      exclusionsPatterns.add(rootArtifact.getGroupId() + MAVEN_COORDINATES_SEPARATOR + rootArtifact.getArtifactId() +
          MAVEN_COORDINATES_SEPARATOR + "*" + MAVEN_COORDINATES_SEPARATOR + "*" + MAVEN_COORDINATES_SEPARATOR
          + rootArtifact.getVersion());
    }

    directDependencies = directDependencies.stream()
        .map(toTransform -> {
          if (toTransform.getScope().equals(TEST)) {
            if (TESTS_CLASSIFIER.equals(toTransform.getArtifact().getClassifier())) {
              // Exclude transitive dependencies of test-jar artifacts
              return toTransform.setScope(COMPILE).setExclusions(singleton(new Exclusion("*", "*", "*", "*")));
            } else {
              return toTransform.setScope(COMPILE);
            }
          }
          if ((PLUGIN.equals(rootArtifactType)
              || MULE_PLUGIN_CLASSIFIER.equals(toTransform.getArtifact().getClassifier())
              || MULE_SERVICE_CLASSIFIER.equals(toTransform.getArtifact().getClassifier()))
              && toTransform.getScope().equals(COMPILE)) {
            return toTransform.setScope(PROVIDED);
          }
          if (rootArtifactType == MODULE && toTransform.getScope().equals(COMPILE)) {
            return toTransform.setScope(PROVIDED);
          }
          Artifact artifact = toTransform.getArtifact();
          if (context.getApplicationSharedLibCoordinates().contains(artifact.getGroupId() + ":" + artifact.getArtifactId())) {
            return toTransform.setScope(COMPILE);
          }
          return toTransform;
        })
        .collect(toList());

    logger.debug("OR exclude: {}", context.getExcludedArtifacts());
    exclusionsPatterns.addAll(context.getExcludedArtifacts());

    if (!context.getTestExclusions().isEmpty()) {
      logger.debug("OR exclude application specific artifacts: {}", context.getTestExclusions());
      exclusionsPatterns.addAll(context.getTestExclusions());
    }

    try {
      List<Dependency> managedDependencies =
          newArrayList(dependencyResolver.readArtifactDescriptor(rootArtifact).getManagedDependencies());
      managedDependencies.addAll(directDependencies.stream()
          .filter(directDependency -> !directDependency.getScope().equals(TEST))
          .collect(toList()));
      logger.debug("Resolving dependency graph for '{}' scope direct dependencies: {} and managed dependencies {}",
                   TEST, directDependencies, managedDependencies);

      final Dependency rootTestDependency = new Dependency(new DefaultArtifact(rootArtifact.getGroupId(),
                                                                               rootArtifact.getArtifactId(), TESTS_CLASSIFIER,
                                                                               JAR_EXTENSION,
                                                                               rootArtifact.getVersion()),
                                                           TEST);

      List<File> urls = dependencyResolver
          .resolveDependencies(rootTestDependency, directDependencies, managedDependencies,
                               orFilter(dependencyFilter, new PatternExclusionsDependencyFilter(exclusionsPatterns)),
                               rootArtifactRemoteRepositories);
      appFiles
          .addAll(urls);
    } catch (Exception e) {
      throw new IllegalStateException("Couldn't resolve dependencies for application '" + context.getRootArtifact()
          + "' classification", e);
    }

    List<URL> testRunnerLibUrls = newArrayList(toUrl(appFiles));
    logger.debug("Appending URLs to test runner plugin: {}", context.getTestRunnerPluginUrls());
    testRunnerLibUrls.addAll(context.getTestRunnerPluginUrls());

    resolveSnapshotVersionsToTimestampedFromClassPath(testRunnerLibUrls, context.getClassPathURLs());

    return testRunnerLibUrls;
  }

  /**
   * Resolves the rootArtifact {@value #JAR_EXTENSION} output {@link File}s to be added to class loader.
   *
   * @param rootArtifact {@link Artifact} being classified
   * @return {@link File} to be added to class loader
   */
  private File resolveRootArtifactFile(Artifact rootArtifact) {
    final DefaultArtifact jarArtifact = new DefaultArtifact(rootArtifact.getGroupId(), rootArtifact.getArtifactId(),
                                                            JAR_EXTENSION, JAR_EXTENSION, rootArtifact.getVersion());
    try {
      return dependencyResolver.resolveArtifact(jarArtifact).getArtifact().getFile();
    } catch (ArtifactResolutionException e) {
      logger.warn("'{}' rootArtifact output {} file couldn't be resolved", rootArtifact, JAR_EXTENSION);
      return null;
    }
  }

  /**
   * Converts the {@link List} of {@link File}s to {@link URL}s
   *
   * @param files {@link File} to get {@link URL}s
   * @return {@link List} of {@link URL}s for the files
   */
  private List<URL> toUrl(Collection<File> files) {
    return files.stream().map(this::toUrl).collect(toList());
  }

  /**
   * Converts the {@link File} to {@link URL}
   *
   * @param file {@link File} to get its {@link URL}
   * @return {@link URL} for the file
   */
  private URL toUrl(File file) {
    try {
      return file.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Couldn't get URL", e);
    }
  }

  /**
   * Eclipse Aether is set to work in {@code offline} mode and to ignore artifact descriptors repositories the metadata for
   * SNAPSHOTs versions cannot be read from remote repositories. So, it will always resolve SNAPSHOT dependencies as normalized,
   * meaning that the resolved URL/File will have the SNAPSHOT format instead of timestamped one.
   * <p/>
   * At the same time IDEs or even Maven when running tests will resolve to timestamped versions instead, so we must do this
   * "resolve" operation that matches SNAPSHOTs resolved artifacts to timestamped SNAPSHOT versions from classpath.
   *
   * @param resolvedURLs {@link URL}s resolved from the dependency graph
   * @param classpathURLs {@link URL}s already provided in class path by IDE or Maven
   */
  private void resolveSnapshotVersionsToTimestampedFromClassPath(List<URL> resolvedURLs, List<URL> classpathURLs) {
    logger.debug("Checking if resolved SNAPSHOT URLs had a timestamped version already included in class path URLs");
    Map<File, List<URL>> classpathFolders = groupArtifactUrlsByFolder(classpathURLs);

    FileFilter snapshotFileFilter = new WildcardFileFilter(SNAPSHOT_WILCARD_FILE_FILTER);
    ListIterator<URL> listIterator = resolvedURLs.listIterator();
    while (listIterator.hasNext()) {
      final URL urlResolved = listIterator.next();
      File artifactResolvedFile = toFile(urlResolved);
      if (snapshotFileFilter.accept(artifactResolvedFile)) {
        File artifactResolvedFileParentFile = artifactResolvedFile.getParentFile();
        logger.debug("Checking if resolved SNAPSHOT artifact: '{}' has a timestamped version already in class path",
                     artifactResolvedFile);
        URL urlFromClassPath = null;
        if (classpathFolders.containsKey(artifactResolvedFileParentFile)) {
          urlFromClassPath = findArtifactUrlFromClassPath(classpathFolders, artifactResolvedFile);
        }

        if (urlFromClassPath != null) {
          logger.debug("Replacing resolved URL '{}' from class path URL '{}'", urlResolved, urlFromClassPath);
          listIterator.set(urlFromClassPath);
        } else {
          logger.warn("'{}' resolved SNAPSHOT version couldn't be matched to a class path URL: '{}'", artifactResolvedFile,
                      classpathURLs);
        }
      }
    }
  }

  /**
   * Creates a {@link Map} that has as key the folder that holds the artifact and value a {@link List} of {@link URL}s. For
   * instance, an artifact in class path that only has its jar packaged output:
   *
   * <pre>
   *   key=/Users/jdoe/.m2/repository/org/mule/extensions/mule-extensions-api-xml-dsl/1.0.0-SNAPSHOT/
   *   value=[file:/Users/jdoe/.m2/repository/org/mule/extensions/mule-extensions-api-xml-dsl/1.0.0-SNAPSHOT/mule-extensions-api-xml-dsl-1.0.0-20160823.170911-32.jar]
   * </pre>
   * <p/>
   * Another case is for those artifacts that have both packaged versions, the jar and the -tests.jar. For instance:
   *
   * <pre>
   *   key=/Users/jdoe/Development/mule/extensions/file/target
   *   value=[file:/Users/jdoe/.m2/repository/org/mule/modules/mule-module-file-extension-common/4.0-SNAPSHOT/mule-module-file-extension-common-4.0-SNAPSHOT.jar,
   *          file:/Users/jdoe/.m2/repository/org/mule/modules/mule-module-file-extension-common/4.0-SNAPSHOT/mule-module-file-extension-common-4.0-SNAPSHOT-tests.jar]
   * </pre>
   *
   * @param classpathURLs the class path {@link List} of {@link URL}s to be grouped by folder
   * @return {@link Map} that has as key the folder that holds the artifact and value a {@link List} of {@link URL}s.
   */
  private Map<File, List<URL>> groupArtifactUrlsByFolder(List<URL> classpathURLs) {
    Map<File, List<URL>> classpathFolders = newHashMap();
    classpathURLs.forEach(url -> {
      File folder = toFile(url).getParentFile();
      if (classpathFolders.containsKey(folder)) {
        classpathFolders.get(folder).add(url);
      } else {
        classpathFolders.put(folder, newArrayList(url));
      }
    });
    return classpathFolders;
  }

  /**
   * Finds the corresponding {@link URL} in class path grouped by folder {@link Map} for the given artifact {@link File}.
   *
   * @param classpathFolders a {@link Map} that has as entry the folder of the artifacts from class path and value a {@link List}
   *        with the artifacts (jar, tests.jar, etc).
   * @param artifactResolvedFile the {@link Artifact} resolved from the Maven dependencies and resolved as SNAPSHOT
   * @return {@link URL} for the artifact found in the class path or {@code null}
   */
  private URL findArtifactUrlFromClassPath(Map<File, List<URL>> classpathFolders, File artifactResolvedFile) {
    List<URL> urls = classpathFolders.get(artifactResolvedFile.getParentFile());
    logger.debug("URLs found for '{}' in class path are: {}", artifactResolvedFile, urls);
    if (urls.size() == 1) {
      return urls.get(0);
    }
    // If more than one is found, we have to check for the case of a test-jar...
    Optional<URL> urlOpt;
    if (endsWithIgnoreCase(artifactResolvedFile.getName(), TESTS_JAR)) {
      urlOpt = urls.stream().filter(url -> toFile(url).getAbsolutePath().endsWith(TESTS_JAR)).findFirst();
    } else {
      urlOpt = urls.stream()
          .filter(url -> {
            String filePath = toFile(url).getAbsolutePath();
            return !filePath.endsWith(TESTS_JAR) && filePath.endsWith(JAR_EXTENSION);
          }).findFirst();
    }
    return urlOpt.orElse(null);
  }

}
