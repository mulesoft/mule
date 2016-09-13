/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.api.classloading.isolation;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.StringUtils.endsWithIgnoreCase;
import static org.eclipse.aether.util.artifact.ArtifactIdUtils.toId;
import static org.eclipse.aether.util.artifact.JavaScopes.COMPILE;
import static org.eclipse.aether.util.artifact.JavaScopes.PROVIDED;
import static org.eclipse.aether.util.artifact.JavaScopes.TEST;
import static org.eclipse.aether.util.filter.DependencyFilterUtils.classpathFilter;
import static org.eclipse.aether.util.filter.DependencyFilterUtils.orFilter;
import static org.mule.runtime.core.util.Preconditions.checkNotNull;
import org.mule.functional.classloading.isolation.classification.PatternInclusionsDependencyFilter;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;
import org.mule.runtime.module.extension.internal.runtime.operation.IllegalSourceException;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.filter.PatternExclusionsDependencyFilter;
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
 * metadata in order to later register it to an {@link org.mule.runtime.extension.api.ExtensionManager}.
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
  private static final String MULE_PLUGIN_CLASSIFIER = "mule-plugin";
  private static final String MULE_EXTESION_CLASSIFIER = "mule-extension";
  private static final String TESTS_CLASSIFIER = "tests";
  private static final String TESTS_JAR = "-tests.jar";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private DependencyResolver dependencyResolver;
  private DefaultExtensionManager extensionManager = new DefaultExtensionManager();
  private PluginResourcesResolver pluginResourcesResolver = new PluginResourcesResolver(extensionManager);

  /**
   * Creates an instance of the classifier.
   *
   * @param dependencyResolver {@link DependencyResolver} to resolve dependencies
   */
  public AetherClassPathClassifier(DependencyResolver dependencyResolver) {
    checkNotNull(dependencyResolver, "dependencyResolver cannot be null");
    this.dependencyResolver = dependencyResolver;
  }

  /**
   * Classifies {@link URL}s and {@link Dependency}s to define how the container, plugins and application class loaders should be
   * created.
   *
   * @param context {@link ClassPathClassifierContext} to be used during the classification
   * @return {@link ArtifactUrlClassification} as result with the classification
   */
  @Override
  public ArtifactUrlClassification classify(ClassPathClassifierContext context) {
    logger.debug("Building class loaders for rootArtifact: {}", context.getRootArtifact());

    List<Dependency> directDependencies;
    try {
      directDependencies = dependencyResolver.getDirectDependencies(context.getRootArtifact());
    } catch (Exception e) {
      throw new IllegalStateException("Couldn't get direct dependencies for rootArtifact: '" + context.getRootArtifact() + "'",
                                      e);
    }

    List<PluginUrlClassification> pluginUrlClassifications = buildPluginUrlClassifications(context, directDependencies);

    List<URL> containerUrls = buildContainerUrlClassification(context, directDependencies, pluginUrlClassifications);
    List<URL> applicationUrls = buildApplicationUrlClassification(context, directDependencies, pluginUrlClassifications);

    return new ArtifactUrlClassification(containerUrls, pluginUrlClassifications, applicationUrls);
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
   * @return {@link List} of {@link URL}s for the container class loader
   */
  private List<URL> buildContainerUrlClassification(ClassPathClassifierContext context,
                                                    List<Dependency> directDependencies,
                                                    List<PluginUrlClassification> pluginUrlClassifications) {
    directDependencies = directDependencies.stream()
        .filter(directDep -> directDep.getScope().equals(PROVIDED))
        .map(depToTransform -> depToTransform.setScope(COMPILE))
        .collect(toList());

    logger.debug("Selected direct dependencies to be used for resolving container dependency graph (changed to compile in " +
        "order to resolve the graph): {}", directDependencies);


    Set<Dependency> managedDependencies = directDependencies.stream()
        .map(directDep -> {
          try {
            return dependencyResolver.readArtifactDescriptor(directDep.getArtifact()).getManagedDependencies();
          } catch (ArtifactDescriptorException e) {
            throw new IllegalStateException("Couldn't read artifact: '" + directDep.getArtifact() + "'", e);
          }
        })
        .flatMap(l -> l.stream())
        .collect(toSet());

    logger.debug("Collected managed dependencies from direct provided dependencies to be used for resolving container "
        + "dependency graph: {}", managedDependencies);

    List<String> excludedFilterPattern = newArrayList(context.getProvidedExclusions());
    excludedFilterPattern.addAll(context.getExcludedArtifacts());
    if (!pluginUrlClassifications.isEmpty()) {
      excludedFilterPattern.addAll(pluginUrlClassifications.stream()
          .map(pluginUrlClassification -> pluginUrlClassification.getName())
          .collect(toList()));
    }

    logger.debug("Resolving dependencies for container using exclusion filter patterns: {}", excludedFilterPattern);
    if (!context.getProvidedInclusions().isEmpty()) {
      logger.debug("Resolving dependencies for container using inclusion filter patterns: {}", context.getProvidedInclusions());
    }

    final DependencyFilter dependencyFilter = orFilter(new PatternInclusionsDependencyFilter(context.getProvidedInclusions()),
                                                       new PatternExclusionsDependencyFilter(excludedFilterPattern));

    List<URL> containerUrls;
    try {
      containerUrls = toUrl(dependencyResolver.resolveDependencies(null, directDependencies, newArrayList(managedDependencies),
                                                                   dependencyFilter));
    } catch (Exception e) {
      throw new IllegalStateException("Couldn't resolve dependencies for container", e);
    }
    containerUrls = containerUrls.stream().filter(url -> {
      String file = url.getFile();
      return !(endsWithIgnoreCase(file, POM_XML) || endsWithIgnoreCase(file, POM_EXTENSION) || endsWithIgnoreCase(file,
                                                                                                                  ZIP_EXTENSION));
    }).collect(toList());

    resolveSnapshotVersionsToTimestampedFromClassPath(containerUrls, context.getClassPathURLs());

    return containerUrls;
  }

  /**
   * Plugin classifications are being done by resolving the dependencies for each plugin coordinates defined at
   * {@link ClassPathClassifierContext#getPluginCoordinates()}. These artifacts should be defined as
   * {@value org.eclipse.aether.util.artifact.JavaScopes#PROVIDED} in the rootArtifact and if these coordinates don't have a
   * version the rootArtifact version would be used to look for the Maven plugin artifact.
   * <p/>
   * While resolving the dependencies for the plugin artifact, only {@value org.eclipse.aether.util.artifact.JavaScopes#COMPILE}
   * dependencies will be selected. {@link ClassPathClassifierContext#getExcludedArtifacts()} will be exluded too.
   * <p/>
   * The resulting {@link PluginUrlClassification} for each plugin will have as name the Maven artifact id coordinates:
   * {@code <groupId>:<artifactId>:<extension>[:<classifier>]:<version>}.
   *
   * @param context {@link ClassPathClassifierContext} with settings for the classification process
   * @param directDependencies {@link List} of {@link Dependency} with direct dependencies for the rootArtifact
   * @return {@link List} of {@link PluginUrlClassification}s for plugins class loaders
   */
  private List<PluginUrlClassification> buildPluginUrlClassifications(ClassPathClassifierContext context,
                                                                      List<Dependency> directDependencies) {
    Map<Artifact, PluginClassificationNode> pluginsClassified = newHashMap();

    ExtensionPluginMetadataGenerator extensionPluginMetadataGenerator =
        new ExtensionPluginMetadataGenerator(context.getPluginResourcesFolder());

    Artifact rootArtifact = context.getRootArtifact();

    List<Artifact> pluginsArtifacts = context.getPluginCoordinates().stream()
        .map(pluginCoords -> createPluginArtifact(pluginCoords, rootArtifact, directDependencies))
        .collect(toList());

    logger.debug("{} plugins defined to be classified", pluginsArtifacts.size());

    if (isMulePlugin(rootArtifact)) {
      logger.debug("rootArtifact '{}' identified as Mule plugin", rootArtifact);
      //TODO(pablo.kraan): isoaltion - ac� est� bien que pase el context posta
      buildPluginUrlClassification(rootArtifact, context, extensionPluginMetadataGenerator, pluginsClassified);

      pluginsArtifacts = pluginsArtifacts.stream()
          .filter(pluginArtifact -> !(rootArtifact.getGroupId().equals(pluginArtifact.getGroupId())
              && rootArtifact.getArtifactId().equals(pluginArtifact.getArtifactId())))
          .collect(toList());
    }

    pluginsArtifacts.stream()
        .forEach(pluginArtifact -> buildPluginUrlClassification(pluginArtifact, context, extensionPluginMetadataGenerator,
                                                                pluginsClassified));

    extensionPluginMetadataGenerator.generateDslResources();

    return toPluginUrlClassification(pluginsClassified.values());
  }

  /**
   * Transforms the {@link PluginClassificationNode} to {@link PluginUrlClassification}.
   *
   * @param classificationNodes the fat object classified that needs to be transformed
   * @return {@link PluginUrlClassification}
   */
  private List<PluginUrlClassification> toPluginUrlClassification(Collection<PluginClassificationNode> classificationNodes) {

    Map<String, PluginUrlClassification> classifiedPluginUrls = new HashMap<>();

    for (PluginClassificationNode node : classificationNodes) {
      final PluginUrlClassification pluginUrlClassification =
          pluginResourcesResolver.resolvePluginResourcesFor(
                                                            new PluginUrlClassification(node.getName(), node.getUrls(),
                                                                                        node.getExportClasses(),
                                                                                        node.getPluginDependencies().stream().map(
                                                                                                                                  dependency -> dependency
                                                                                                                                      .getName())
                                                                                            .collect(toList())));

      classifiedPluginUrls.put(node.getName(), pluginUrlClassification);
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

    return new ArrayList<>(classifiedPluginUrls.values());
  }

  private boolean isMulePlugin(Artifact artifact) {
    return artifact.getExtension().equals(MULE_PLUGIN_CLASSIFIER) || artifact.getExtension().equals(MULE_EXTESION_CLASSIFIER);
  }

  /**
   * Classifies a plugin {@link Artifact}. {@value org.eclipse.aether.util.artifact.JavaScopes#COMPILE} dependencies will be
   * resolved for building the {@link URL}'s for the class loader. For {@link Extension} annotated classes it will also generate
   * its metadata. Once classified the node is added to {@link Map} of pluginsClassified.
   *
   * @param pluginArtifact {@link Artifact} that represents the plugin to be classified
   * @param context {@link ClassPathClassifierContext} with settings for the classification process
   * @param extensionPluginGenerator {@link ExtensionPluginMetadataGenerator} extensions metadata generator
   * @param pluginsClassified {@link Map} that contains already classified plugins
   */
  private void buildPluginUrlClassification(Artifact pluginArtifact, ClassPathClassifierContext context,
                                            ExtensionPluginMetadataGenerator extensionPluginGenerator,
                                            Map<Artifact, PluginClassificationNode> pluginsClassified) {
    List<URL> urls;
    try {
      List<Dependency> managedDependencies = dependencyResolver.readArtifactDescriptor(pluginArtifact).getManagedDependencies();

      final DependencyFilter dependencyFilter = orFilter(classpathFilter(COMPILE),
                                                         new PatternExclusionsDependencyFilter(context.getExcludedArtifacts()));
      urls = toUrl(dependencyResolver.resolveDependencies(new Dependency(pluginArtifact, COMPILE),
                                                          Collections.<Dependency>emptyList(), managedDependencies,
                                                          dependencyFilter));
    } catch (Exception e) {
      throw new IllegalStateException("Couldn't resolve dependencies for plugin: '" + pluginArtifact + "' classification", e);
    }

    Class extensionClass = extensionPluginGenerator.scanForExtensionAnnotatedClasses(pluginArtifact, urls);
    List<PluginClassificationNode> pluginDependencies = newArrayList();
    if (extensionClass != null) {
      logger.debug("Plugin '{}' has been discovered as Extension", pluginArtifact);
      if (context.isExtensionMetadataGenerationEnabled()) {
        File generatedMetadataFolder = extensionPluginGenerator.generateExtensionManifest(pluginArtifact, extensionClass);
        if (generatedMetadataFolder != null) {
          URL generatedTestResources = toUrl(generatedMetadataFolder);

          List<URL> appendedTestResources = newArrayList(generatedTestResources);
          appendedTestResources.addAll(urls);
          urls = appendedTestResources;
        }
      }
    }

    List<Dependency> directDependencies;
    try {
      directDependencies = dependencyResolver.getDirectDependencies(pluginArtifact);
    } catch (ArtifactDescriptorException e) {
      throw new IllegalStateException("Couldn't get direct dependencies for plugin: '" + pluginArtifact + "'", e);
    }
    logger.debug("Searching for plugin dependencies on direct dependencies of plugin {}", pluginArtifact);
    List<Artifact> pluginArtifactDependencies = directDependencies.stream()
        .filter(dependency -> dependency.getArtifact().getClassifier().equals(MULE_PLUGIN_CLASSIFIER))
        .map(dependency -> dependency.getArtifact())
        .collect(toList());
    logger.debug("Artifacts {} identified a plugin dependencies for plugin {}", pluginArtifactDependencies, pluginArtifact);
    pluginArtifactDependencies.stream()
        .map(artifact -> {
          if (!pluginsClassified.containsKey(artifact)) {
            buildPluginUrlClassification(artifact, context, extensionPluginGenerator, pluginsClassified);
          }
          return pluginsClassified.get(artifact);
        })
        .forEach(pluginDependencies::add);

    // TODO(gfernandes): MULE-10484 How could I check if exported classes belong to this plugin?
    PluginClassificationNode pluginUrlClassification = new PluginClassificationNode(toClassifierLessId(pluginArtifact),
                                                                                    urls,
                                                                                    newArrayList(context
                                                                                        .getExportPluginClasses(pluginArtifact)),
                                                                                    pluginDependencies);
    pluginsClassified.put(pluginArtifact, pluginUrlClassification);
  }

  private String toClassifierLessId(Artifact pluginArtifact) {
    return toId(pluginArtifact.getGroupId(), pluginArtifact.getArtifactId(), pluginArtifact.getExtension(), null,
                pluginArtifact.getVersion());
  }

  /**
   * Creates the plugin {@link Artifact}, if no version is {@value org.eclipse.aether.util.artifact.JavaScopes#PROVIDED} it will
   * be obtained from the direct dependencies for the rootArtifact or if the same rootArtifact is the plugin declared it will take
   * its version.
   *
   * @param pluginCoords Maven coordinates that define the plugin
   * @param rootArtifact {@link Artifact} that defines the current artifact that requested to build this class loaders
   * @param directDependencies {@link List} of {@link Dependency} with direct dependencies for the rootArtifact
   * @return {@link Artifact} representing the plugin
   */
  private Artifact createPluginArtifact(String pluginCoords, Artifact rootArtifact, List<Dependency> directDependencies) {
    final String[] pluginSplitCoords = pluginCoords.split(MAVEN_COORDINATES_SEPARATOR);
    String pluginGroupId = pluginSplitCoords[0];
    String pluginArtifactId = pluginSplitCoords[1];
    String pluginVersion;

    if (rootArtifact.getGroupId().equals(pluginGroupId) && rootArtifact.getArtifactId().equals(pluginArtifactId)) {
      logger.debug("'{}' declared as plugin, resolving version from pom file", rootArtifact);
      pluginVersion = rootArtifact.getVersion();
    } else {
      logger.debug("Resolving version for '{}' from direct dependencies", pluginCoords);
      Optional<Dependency> pluginDependencyOp = directDependencies.isEmpty() ? Optional.<Dependency>empty()
          : directDependencies.stream().filter(dependency -> dependency.getArtifact().getGroupId().equals(pluginGroupId)
              && dependency.getArtifact().getArtifactId().equals(pluginArtifactId)).findFirst();

      if (!pluginDependencyOp.isPresent() || !pluginDependencyOp.get().getScope().equals(PROVIDED)) {
        throw new IllegalStateException("Plugin '" + pluginCoords
            + " in order to be resolved has to be declared as " + PROVIDED + " dependency of your Maven project");
      }
      Dependency pluginDependency = pluginDependencyOp.get();
      pluginVersion = pluginDependency.getArtifact().getVersion();
    }

    final DefaultArtifact artifact = new DefaultArtifact(pluginGroupId, pluginArtifactId, JAR_EXTENSION, pluginVersion);
    logger.debug("'{}' plugin coordinates resolved to: '{}'", pluginCoords, artifact);
    return artifact;
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
   * @param pluginUrlClassifications {@link PluginUrlClassification}s to check if rootArtifact was classified as plugin
   * @return {@link URL}s for application class loader
   */
  private List<URL> buildApplicationUrlClassification(ClassPathClassifierContext context,
                                                      List<Dependency> directDependencies,
                                                      List<PluginUrlClassification> pluginUrlClassifications) {
    logger.debug("Building application classification");
    Artifact rootArtifact = context.getRootArtifact();

    DependencyFilter dependencyFilter = new PatternInclusionsDependencyFilter(context.getTestInclusions());
    logger.debug("Using filter for dependency graph to include: '{}'", context.getTestInclusions());

    boolean isRootArtifactPlugin = !pluginUrlClassifications.isEmpty()
        && pluginUrlClassifications.stream().filter(p -> {
          Artifact plugin = new DefaultArtifact(p.getName());
          return plugin.getGroupId().equals(rootArtifact.getGroupId())
              && plugin.getArtifactId().equals(rootArtifact.getArtifactId());
        }).findFirst().isPresent();

    List<File> appFiles = newArrayList();
    List<String> exclusionsPatterns = newArrayList();

    if (!isRootArtifactPlugin) {
      logger.debug("RootArtifact is not a plugin so is going to be added to application classification");
      final DefaultArtifact rootJarArtifact = new DefaultArtifact(rootArtifact.getGroupId(), rootArtifact.getArtifactId(),
                                                                  JAR_EXTENSION, JAR_EXTENSION, rootArtifact.getVersion());
      try {
        appFiles.addAll(dependencyResolver.resolveDependencies(new Dependency(rootJarArtifact, COMPILE), null, null,
                                                               new PatternInclusionsDependencyFilter(toId(rootJarArtifact))));
      } catch (DependencyCollectionException | DependencyResolutionException e) {
        logger.warn("'{}' rootArtifact is not a plugin but it doesn't a target/classes due to it couldn't be resolved",
                    rootArtifact);
      }
    } else {
      logger.debug("RootArtifact is a plugin or it doesn't have a target/classes folder (it is the case of a test artifact)");
      exclusionsPatterns.add(rootArtifact.getGroupId() + MAVEN_COORDINATES_SEPARATOR + rootArtifact.getArtifactId() +
          MAVEN_COORDINATES_SEPARATOR + "*" + MAVEN_COORDINATES_SEPARATOR + rootArtifact.getVersion());
    }

    directDependencies = directDependencies.stream()
        .map(toTransform -> {
          if (toTransform.getScope().equals(TEST)) {
            return new Dependency(toTransform.getArtifact(), COMPILE);
          }
          if (isRootArtifactPlugin && toTransform.getScope().equals(COMPILE)) {
            return new Dependency(toTransform.getArtifact(), PROVIDED);
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

      appFiles
          .addAll(dependencyResolver
              .resolveDependencies(rootTestDependency, directDependencies, managedDependencies,
                                   orFilter(dependencyFilter,
                                            new PatternExclusionsDependencyFilter(exclusionsPatterns))));
    } catch (Exception e) {
      throw new IllegalStateException("Couldn't resolve dependencies for application '" + context.getRootArtifact()
          + "' classification", e);
    }
    return toUrl(appFiles);
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
      File artifactResolvedFile = new File(urlResolved.getFile());
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
          throw new IllegalSourceException(artifactResolvedFile
              + " resolved SNAPSHOT version couldn't be matched to a class path URL");
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
      File folder = new File(url.getFile()).getParentFile();
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
      urlOpt = urls.stream().filter(url -> url.getFile().endsWith(TESTS_JAR)).findFirst();
    } else {
      urlOpt = urls.stream()
          .filter(url -> !url.getFile().endsWith(TESTS_JAR) && url.getFile().endsWith(JAR_EXTENSION)).findFirst();
    }
    return urlOpt.orElse(null);
  }

}
