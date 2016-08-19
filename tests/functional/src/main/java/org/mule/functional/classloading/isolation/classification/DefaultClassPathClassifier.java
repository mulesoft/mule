/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.classification;

import static com.google.common.collect.Lists.newArrayList;
import static java.io.File.separator;
import static java.lang.Class.forName;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.core.util.PropertiesUtils.loadProperties;
import static org.springframework.core.io.support.ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX;
import static org.springframework.util.ResourceUtils.extractJarFileURL;
import org.mule.functional.api.classloading.isolation.ArtifactUrlClassification;
import org.mule.functional.api.classloading.isolation.ArtifactsUrlClassification;
import org.mule.functional.api.classloading.isolation.ClassPathClassifier;
import org.mule.functional.api.classloading.isolation.ClassPathClassifierContext;
import org.mule.functional.api.classloading.isolation.Configuration;
import org.mule.functional.api.classloading.isolation.DependenciesFilter;
import org.mule.functional.api.classloading.isolation.DependencyResolver;
import org.mule.functional.api.classloading.isolation.MavenMultiModuleArtifactMapping;
import org.mule.functional.api.classloading.isolation.PluginUrlClassification;
import org.mule.functional.api.classloading.isolation.TransitiveDependenciesFilter;
import org.mule.functional.classloading.isolation.classpath.MavenArtifactToClassPathUrlsResolver;
import org.mule.functional.junit4.ExtensionsTestInfrastructureDiscoverer;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.registry.DefaultRegistryBroker;
import org.mule.runtime.core.registry.MuleRegistryHelper;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.introspection.declaration.spi.Describer;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * Builds a {@link ArtifactsUrlClassification} similar to what Mule Runtime does by taking into account the Maven dependencies of
 * the given tested artifact.
 * <p/>
 * Basically it creates a {@link ArtifactsUrlClassification} hierarchy with:
 * <ul>
 * <li>{@code provided} scope (plus JDK stuff)</li>
 * <li>Composite ClassLoader(that includes a class loader for each extension (if discovered) and/or plugin if the current artifact
 * has target/classes folder, for any case its {@code compile} scope dependencies are also added (plus its target/classes)</li>
 * <li>Test Scope (plus target/test-classes and all the {@code test} scope dependencies including transitives)</li>
 * </ul>
 *
 * <p/>
 * Just for testing cases were {@link Class} from the plugin has to be visible it will also export {@link Class}es that were
 * defined by the {@link ClassPathClassifierContext#getExportClasses()}.
 *
 * @since 4.0
 */
public class DefaultClassPathClassifier implements ClassPathClassifier {

  public static final String GENERATED_TEST_SOURCES = "generated-test-sources";
  public static final String JAR = "jar";
  public static final String FILE = "file";
  public static final String POM_PROPERTIES_FILE_NAME = "pom.properties";
  public static final String ARTIFACT_ID = "artifactId";
  public static final String SERVICE_PROPERTIES_FILE_NAME = "service.properties";
  public static final String SERVICE_PROVIDER_CLASS_NAME = "service.className";

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * {@inheritDoc}
   */
  @Override
  public ArtifactsUrlClassification classify(ClassPathClassifierContext context) {
    if (logger.isDebugEnabled()) {
      logger.debug("Classification based on '{}'", context.getDependencyGraph().getRootArtifact());
    }

    MavenArtifactToClassPathUrlsResolver artifactToClassPathUrlResolver =
        new MavenArtifactToClassPathUrlsResolver(context.getMavenMultiModuleArtifactMapping());

    ExtendedClassPathClassifierContext extendedClassPathClassifierContext =
        new ExtendedClassPathClassifierContext(context, artifactToClassPathUrlResolver);

    List<URL> appUrls = buildAppUrls(extendedClassPathClassifierContext);
    List<ArtifactUrlClassification> serviceUrlClassifications =
        buildServicesUrlClassification(extendedClassPathClassifierContext);
    List<PluginUrlClassification> pluginUrlClassifications = buildPluginsUrlClassification(extendedClassPathClassifierContext);
    List<URL> containerUrls = buildContainerUrls(extendedClassPathClassifierContext, appUrls, pluginUrlClassifications);

    return new ArtifactsUrlClassification(containerUrls, serviceUrlClassifications, pluginUrlClassifications, appUrls);
  }

  /**
   * Builds the list of {@link URL}s for the application classification. Test scope is what mostly drives this classification.
   *
   * @param extendedContext {@link ExtendedClassPathClassifierContext} that holds the data needed for classifying the artifacts
   * @return a {@link List} of {@link URL}s that would be the one used for the application class loader.
   */
  protected List<URL> buildAppUrls(final ExtendedClassPathClassifierContext extendedContext) {
    // rootArtifactTestClassesFolder is not present in the dependency graph, so here is the only place were we must add it
    // manually breaking the original order that came from class path
    List<URL> appURLs = extendedContext.getClassificationContext().getClassPathURLs().stream()
        .filter(url -> url.getFile()
            .equals(extendedContext.getClassificationContext().getRootArtifactTestClassesFolder().getAbsolutePath() + separator))
        .collect(toList());
    new DependencyResolver(new Configuration()
        .setMavenDependencyGraph(extendedContext.getClassificationContext().getDependencyGraph())
        .selectDependencies(new DependenciesFilter().match(dependency -> dependency.isTestScope()
            && !extendedContext.getClassificationContext().getExclusions().test(dependency)))
        .collectTransitiveDependencies(new TransitiveDependenciesFilter()
            .match(transitiveDependency -> transitiveDependency.isTestScope()
                && !extendedContext.getClassificationContext().getExclusions().test(transitiveDependency))
            .evaluateTransitiveDependenciesWhenPredicateFails())).resolveDependencies().stream()
                .filter(d -> !d.isPomType())
                .map(dependency -> extendedContext.getArtifactToClassPathURLResolver()
                    .resolveURL(dependency, extendedContext.getClassificationContext().getClassPathURLs()))
                .collect(toCollection(() -> appURLs));
    return appURLs;
  }

  /**
   * Builds the list of {@link URL}s for the plugins classification. Compile scope is what mostly drives this classification.
   *
   * @param extendedContext {@link ExtendedClassPathClassifierContext} that holds the data needed for classifying the artifacts
   * @return a {@link List} of {@link URL}s that would be the one used for the plugins class loaders.
   */
  protected List<PluginUrlClassification> buildPluginsUrlClassification(final ExtendedClassPathClassifierContext extendedContext) {
    List<PluginUrlClassification> pluginClassifications = new ArrayList<>();
    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
    scanner.addIncludeFilter(new AnnotationTypeFilter(Extension.class));
    // TODO(gfernandes): MULE-10081
    Optional<String> extensionsBasePackage = extendedContext.getClassificationContext().getExtensionBasePackages().stream()
        .filter(v -> isNotBlank(v)).findFirst();
    if (!extensionsBasePackage.isPresent() || extensionsBasePackage.get().isEmpty()) {
      throw new IllegalArgumentException("Base package for discovering Extensions cannot be empty, it will take too much time to "
          + "discover all the classes, please set a reasonable package to be scanned in order to discover Extensions.");
    }
    Set<BeanDefinition> extensionsAnnotatedClasses = scanner.findCandidateComponents(extensionsBasePackage.get());

    boolean isRootArtifactIdAnExtension = false;
    if (!extensionsAnnotatedClasses.isEmpty()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Extensions found, plugin class loaders would be created for each extension");
      }
      Set<String> extensionsAnnotatedClassesNoDups =
          extensionsAnnotatedClasses.stream().map(beanDefinition -> beanDefinition.getBeanClassName()).collect(toSet());
      for (String extensionClassName : extensionsAnnotatedClassesNoDups) {
        if (logger.isDebugEnabled()) {
          logger.debug("Classifying classpath for extension class: '{}'", extensionClassName);
        }
        Class extensionClass;
        try {
          extensionClass = forName(extensionClassName);
        } catch (ClassNotFoundException e) {
          throw new IllegalArgumentException("Cannot create plugin/Extension class loader classification due to extension class not found",
                                             e);
        }

        File extensionSourceCodeLocation = new File(extensionClass.getProtectionDomain().getCodeSource().getLocation().getPath());
        if (logger.isDebugEnabled()) {
          logger.debug("Extension: '{}' loaded from path: '{}'", extensionClass.getName(), extensionSourceCodeLocation);
        }

        String extensionMavenArtifactId =
            getMavenArtifactId(extensionSourceCodeLocation,
                               extendedContext.getClassificationContext().getMavenMultiModuleArtifactMapping());
        isRootArtifactIdAnExtension |= extendedContext.getRootArtifact().getArtifactId().equals(extensionMavenArtifactId);

        pluginClassifications.add(extensionClassPathClassification(extensionClass, extensionMavenArtifactId, extendedContext));
      }
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("There are no Extensions in the classpath, exportClasses would be ignored");
      }
    }

    if (!isRootArtifactIdAnExtension && extendedContext.getClassificationContext().getRootArtifactClassesFolder().exists()) {
      if (logger.isDebugEnabled()) {
        logger
            .debug(
                   "Current maven artifact that holds the test class is not an extension, so a plugin class loader would be create with its compile dependencies");
      }
      pluginClassifications.add(pluginClassPathClassification(extendedContext));
    }
    return pluginClassifications;
  }

  /**
   * Builds the list of {@link URL}s for the container classification. Provided scope is what mostly drives this classification
   * minus application {@link URL}s and plugins {@link URL}s.
   *
   * @param extendedContext {@link ExtendedClassPathClassifierContext} that holds the data needed for classifying the artifacts
   * @return a {@link List} of {@link URL}s that would be the one used for the container class loader.
   */
  protected List<URL> buildContainerUrls(final ExtendedClassPathClassifierContext extendedContext, final List<URL> appURLs,
                                         List<PluginUrlClassification> pluginUrlClassifications) {
    // The container contains anything that is not application either extension class loader urls
    Set<URL> containerURLs = new LinkedHashSet<>();
    containerURLs.addAll(extendedContext.getClassificationContext().getClassPathURLs());
    containerURLs.removeAll(appURLs);
    pluginUrlClassifications.stream()
        .forEach(pluginUrlClassification -> containerURLs.removeAll(pluginUrlClassification.getUrls()));

    // If a provided dependency was removed due to there is only one URL in class path for the same dependency, doesn't have the
    // cardinality that maven has
    new DependencyResolver(new Configuration()
        .setMavenDependencyGraph(extendedContext.getClassificationContext().getDependencyGraph())
        .selectDependencies(new DependenciesFilter().match(dependency -> dependency.isProvidedScope()))
        .collectTransitiveDependencies(new TransitiveDependenciesFilter()
            .match(transitiveDependency -> transitiveDependency.isProvidedScope()
                || transitiveDependency.isCompileScope())
            .evaluateTransitiveDependenciesWhenPredicateFails())).resolveDependencies().stream()
                .filter(d -> !d.isPomType())
                .map(dependency -> extendedContext.getArtifactToClassPathURLResolver()
                    .resolveURL(dependency, extendedContext.getClassificationContext().getClassPathURLs()))
                .forEach(containerURLs::add);

    return newArrayList(containerURLs);
  }

  /**
   * Finds if there are {@value #SERVICE_PROPERTIES_FILE_NAME} in classpath in order to build a {@link ArtifactUrlClassification}
   * for each artifact that contains the {@value #SERVICE_PROPERTIES_FILE_NAME} file, {@cvalue #SERVICE_PROVIDER_CLASS_NAME} will
   * be used as {@link ArtifactClassLoader#getArtifactName()}. Once an artifact is identified the dependencies are going to be
   * collected from the graph using Maven {@code provided} scope.
   *
   * @param extendedContext {@link ExtendedClassPathClassifierContext} that holds the data needed for classifying the artifacts
   * @return a {@link List} of {@link ArtifactUrlClassification}s that would be the one used for the plugins class loaders.
   */
  protected List<ArtifactUrlClassification> buildServicesUrlClassification(final ExtendedClassPathClassifierContext extendedContext) {
    URLClassLoader classLoader = extendedContext.getClassificationContext().getClassPathClassLoader();
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
    List<ArtifactUrlClassification> serviceUrlClassifications = newArrayList();
    Resource[] resources;
    try {
      resources = resolver.getResources(CLASSPATH_ALL_URL_PREFIX + "/META-INF/" + SERVICE_PROPERTIES_FILE_NAME);
      if (logger.isDebugEnabled()) {
        logger.debug("Discovered {} services, processing them to build the URLs list for each one", resources.length);
      }
      for (Resource resource : resources) {
        File artifactFile = getServiceMavenArtifactFile(resource);

        Properties serviceProperties = loadProperties(resource.getInputStream());
        String serviceProviderClassName = serviceProperties.getProperty(SERVICE_PROVIDER_CLASS_NAME);

        String serviceMavenArtifactId =
            getMavenArtifactId(artifactFile,
                               extendedContext.getClassificationContext().getMavenMultiModuleArtifactMapping());
        checkArgument(!extendedContext.getRootArtifact().getArtifactId().equals(serviceMavenArtifactId),
                      "RootArtifact: '" + extendedContext.getRootArtifact() + "' cannot be a service. It is not supported");

        if (!extendedContext.getClassificationContext().getServicesExclusion().contains(serviceMavenArtifactId)) {
          if (logger.isDebugEnabled()) {
            logger.debug("Service: '{}' from artifactId: '{}' found and being classified", serviceProviderClassName,
                         serviceMavenArtifactId);
          }
          List<URL> serviceURLs = serviceClassPathClassification(extendedContext, serviceMavenArtifactId);

          serviceUrlClassifications.add(new ArtifactUrlClassification(serviceProviderClassName, serviceURLs));
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug("Service: '{}' from artifactId: '{}' found and ignored as it is excluded", serviceProviderClassName,
                         serviceMavenArtifactId);
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Error while getting resources for services", e);
    }
    return serviceUrlClassifications;
  }

  /**
   * Classifies the classpath to get the {@link List} of {@link URL}s for the serviceMavenArtifactId using Maven {@code provided}
   * scope.
   *
   * @param extendedContext {@link ExtendedClassPathClassifierContext} that holds the data needed for classifying the artifacts
   * @param serviceMavenArtifactId the Maven artifactId for the current service being classified
   * @return {@link List} of {@link URL}s that define the class loader for the service.
   */
  private List<URL> serviceClassPathClassification(ExtendedClassPathClassifierContext extendedContext,
                                                   String serviceMavenArtifactId) {
    return new DependencyResolver(new Configuration()
        .setMavenDependencyGraph(extendedContext.getClassificationContext().getDependencyGraph())
        .selectDependencies(new DependenciesFilter().match(dependency -> dependency.getArtifactId()
            .equals(serviceMavenArtifactId)))
        .collectTransitiveDependencies(new TransitiveDependenciesFilter()
            .match(transitiveDependency -> transitiveDependency.isProvidedScope()
                && !extendedContext.getClassificationContext().getExclusions()
                    .test(transitiveDependency))))
                        .resolveDependencies()
                        .stream().filter(d -> !d.isPomType())
                        .map(dependency -> extendedContext.getArtifactToClassPathURLResolver()
                            .resolveURL(dependency, extendedContext.getClassificationContext().getClassPathURLs()))
                        .collect(toList());
  }

  /**
   * Gets the service artifact {@link File} where the {@link Resource} is contained. It could be the case of a {@value #JAR}
   * or in the case of a multi-module project a folder.
   *
   * @param resource the {@link Resource} where the service was discovered
   * @return a {@link File} of the container for the resource
   * @throws IOException if an error ocurred while getting the {@link File}
   */
  private File getServiceMavenArtifactFile(Resource resource) throws IOException {
    File artifactFile;
    String protocol = resource.getURL().getProtocol();
    if (protocol.equals(JAR)) {
      if (logger.isDebugEnabled()) {
        logger.debug("Service discovered from JAR file: {} ", resource.getFile());
      }
      artifactFile = new File(extractJarFileURL(resource.getURL()).getFile());
    } else if (protocol.equals(FILE) && resource.getFile().getParentFile().getParentFile().isDirectory()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Service discovered from directory, most likely a multi-module project: {}", resource.getFile());
      }
      artifactFile = resource.getFile().getParentFile().getParentFile();
    } else {
      throw new IllegalStateException("A " + SERVICE_PROPERTIES_FILE_NAME + " was found in a resource that is not a " + JAR +
          " neither a folder (Maven multi-module), instead it is in a resource: '" +
          resource.getFile() + "' that cannot be handled by the classification process");
    }
    return artifactFile;
  }

  /**
   * @return an {@link ExtensionManagerAdapter} that would be used to register the extensions, later it would be discarded.
   */
  private ExtensionManagerAdapter createExtensionManager() {
    DefaultExtensionManager extensionManager = new DefaultExtensionManager();
    extensionManager.setMuleContext(new DefaultMuleContext() {

      @Override
      public MuleRegistry getRegistry() {
        return new MuleRegistryHelper(new DefaultRegistryBroker(this), this);
      }
    });
    try {
      extensionManager.initialise();
    } catch (InitialisationException e) {
      throw new RuntimeException("Error while initialising the extension manager", e);
    }
    return extensionManager;
  }

  /**
   * For the given artifact {@link File} it will lookup for the maven artifact id using the
   * {@link MavenMultiModuleArtifactMapping}.
   *
   * @param artifactFile the artifact {@link File}
   * @param mavenMultiModuleMapping the maven multi module mapping
   * @return the maven artifact id mapped to the given file
   */
  private String getMavenArtifactId(final File artifactFile,
                                    final MavenMultiModuleArtifactMapping mavenMultiModuleMapping) {
    String mavenArtifactId;

    // If it is a file it means that it is an installed artifact
    if (artifactFile.isFile()) {
      // It is a jar file, we just get it from the pom.properties file (jars must contain it in order to be classified)
      mavenArtifactId = readArtifactIdFromPomProperties(artifactFile);
    } else {
      mavenArtifactId = mavenMultiModuleMapping
          .getArtifactId(artifactFile.getParentFile().getParentFile().getAbsolutePath() + separator);
    }

    return mavenArtifactId;
  }

  /**
   * Reads from the jar {@link File} the maven {@code artifactId} entry from its {@value #POM_PROPERTIES_FILE_NAME} file.
   *
   * @param jarFile to read the {@value #POM_PROPERTIES_FILE_NAME} file
   * @return the {@code artifactId} for the give jarFile
   */
  private String readArtifactIdFromPomProperties(File jarFile) {
    checkArgument(getExtension(jarFile.getPath()).equals(JAR),
                  "It should be a JAR file in order to read '" + POM_PROPERTIES_FILE_NAME + "' file from: " + jarFile);

    try (ZipFile zipFile = new ZipFile(jarFile)) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();

      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (entry.getName().endsWith(File.separator + POM_PROPERTIES_FILE_NAME)) {
          return loadProperties(zipFile.getInputStream(entry)).getProperty(ARTIFACT_ID);
        }
      }

      throw new IllegalStateException("Couldn't get artifactId property from '" + POM_PROPERTIES_FILE_NAME
          + "' file from jar file: " + jarFile);
    } catch (IOException e) {
      throw new RuntimeException("Error while reading '" + POM_PROPERTIES_FILE_NAME + "' from jar file: " + jarFile);
    }
  }

  /**
   * It creates the resources for the given extension and does the classification of dependencies for the given extension and its
   * artifactId in order to collect the URLs to be used for the plugin {@link ClassLoader} for the extension.
   *
   * @param extension the extension {@link Class} that is annotated with {@link Extension}
   * @param extensionMavenArtifactId the maven artifactId for the current extension being classified
   * @param extendedContext {@link ExtendedClassPathClassifierContext} that holds the data needed for classifying the artifacts
   * @return a {@link PluginUrlClassification} with the list of {@link URL}s defined to be included in this extension
   *         {@link ClassLoader}
   */
  private PluginUrlClassification extensionClassPathClassification(final Class extension, final String extensionMavenArtifactId,
                                                                   final ExtendedClassPathClassifierContext extendedContext) {
    if (logger.isDebugEnabled()) {
      logger.debug("Extension classification for extension class: '{}', using artifactId: '{}'", extension.getName(),
                   extensionMavenArtifactId);
    }
    List<URL> extensionURLs = new ArrayList<>();

    // First we need to add META-INF folder for generated resources due to they may be already created by another mvn install goal
    // by the extension maven plugin
    File generatedResourcesDirectory =
        new File(extendedContext.getClassificationContext().getRootArtifactTestClassesFolder(),
                 GENERATED_TEST_SOURCES + separator + extensionMavenArtifactId + separator + "META-INF");
    generatedResourcesDirectory.mkdirs();
    ExtensionsTestInfrastructureDiscoverer extensionDiscoverer =
        new ExtensionsTestInfrastructureDiscoverer(createExtensionManager(), generatedResourcesDirectory);
    extensionDiscoverer.discoverExtensions(new Describer[0], new Class[] {extension});
    try {
      // Registering parent file as resource to be used from the configuration
      extensionURLs.add(generatedResourcesDirectory.getParentFile().toURI().toURL());
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Error while building resource URL for directory: "
          + generatedResourcesDirectory.getPath(), e);
    }

    int sizeBeforeDepResolver = extensionURLs.size();

    new DependencyResolver(new Configuration()
        .setMavenDependencyGraph(extendedContext.getClassificationContext().getDependencyGraph())
        .includeRootArtifact(rootArtifact -> rootArtifact.getArtifactId().equals(extensionMavenArtifactId))
        .selectDependencies(new DependenciesFilter().match(dependency -> dependency.getArtifactId()
            .equals(extensionMavenArtifactId) || (extendedContext.getRootArtifact().getArtifactId()
                .equals(extensionMavenArtifactId) && dependency.isCompileScope()
                && !extendedContext.getClassificationContext().getExclusions().test(dependency))))
        .collectTransitiveDependencies(new TransitiveDependenciesFilter()
            .match(transitiveDependency -> transitiveDependency.isCompileScope()
                && !extendedContext.getClassificationContext().getExclusions().test(transitiveDependency)))).resolveDependencies()
                    .stream().filter(d -> !d.isPomType())
                    .map(dependency -> extendedContext.getArtifactToClassPathURLResolver()
                        .resolveURL(dependency, extendedContext.getClassificationContext().getClassPathURLs()))
                    .forEach(extensionURLs::add);

    if (extensionURLs.size() == sizeBeforeDepResolver) {
      throw new IllegalStateException("No dependencies found or resolved with the given classpath for the extension: '"
          + extension.getName() + "' using artifactId: '" + extensionMavenArtifactId
          + "'. Be aware that compile is the scope used by the classification process for selecting URLs to be added into the plugin class loader"
          + ". Check if the '" + extensionMavenArtifactId
          + "' has an URL entry in the classpath. If this is failing in your IDE, please reimport your" + " Maven project.");
    }

    List<Class> exportClassesForExtension =
        extendedContext
            .getClassificationContext().getExportClasses().stream().filter(c -> c.getProtectionDomain().getCodeSource()
                .getLocation().getPath().equals(extension.getProtectionDomain().getCodeSource().getLocation().getPath()))
            .collect(toList());

    return new PluginUrlClassification(extension.getName(), extensionURLs, exportClassesForExtension);
  }

  /**
   * Classifies URLs for a plugin that is not an extension where its compile dependencies and transitive dependencies should go to
   * a plugin {@link ClassLoader}.
   *
   * @param extendedContext {@link ExtendedClassPathClassifierContext} that holds the data needed for classifying the artifacts
   * @return a {@link PluginUrlClassification} with the list of {@link URL}s defined to be included in this plugin
   *         {@link ClassLoader}
   */
  private PluginUrlClassification pluginClassPathClassification(final ExtendedClassPathClassifierContext extendedContext) {
    Set<URL> urls =
        new DependencyResolver(new Configuration()
            .setMavenDependencyGraph(extendedContext.getClassificationContext().getDependencyGraph()).includeRootArtifact()
            .selectDependencies(new DependenciesFilter().match(dependency -> dependency.isCompileScope()
                && !extendedContext.getClassificationContext().getExclusions().test(dependency)))
            .collectTransitiveDependencies(new TransitiveDependenciesFilter()
                .match(transitiveDependency -> transitiveDependency.isCompileScope()
                    && !extendedContext.getClassificationContext().getExclusions().test(transitiveDependency))))
                        .resolveDependencies().stream().filter(d -> !d.isPomType()).map(dependency -> extendedContext
                            .getArtifactToClassPathURLResolver()
                            .resolveURL(dependency,
                                        extendedContext.getClassificationContext().getClassPathURLs()))
                        .collect(toSet());

    List<Class> exportClasses = extendedContext
        .getClassificationContext().getExportClasses().stream().filter(c -> c.getProtectionDomain().getCodeSource().getLocation()
            .getPath().startsWith(extendedContext.getClassificationContext().getRootArtifactClassesFolder().getPath()))
        .collect(toList());

    return new PluginUrlClassification(extendedContext.getRootArtifact().getArtifactId(), newArrayList(urls),
                                       exportClasses);
  }

}
