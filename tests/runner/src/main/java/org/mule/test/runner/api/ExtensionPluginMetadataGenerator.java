/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner.api;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.test.runner.api.MulePluginBasedLoaderFinder.META_INF_MULE_PLUGIN;
import static org.mule.test.runner.utils.RunnerModuleUtils.assureSdkApiInClassLoader;
import static org.mule.test.runner.utils.TroubleshootingUtils.getLastModifiedDateFromUrl;
import static org.mule.test.runner.utils.TroubleshootingUtils.getMD5FromFile;

import static java.io.File.separator;
import static java.lang.System.getProperty;
import static java.util.stream.Collectors.toList;

import static com.google.common.collect.Lists.newArrayList;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.lifecycle.MuleLifecycleInterceptor;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistryHelper;
import org.mule.runtime.core.internal.registry.SimpleRegistry;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;
import org.mule.test.runner.infrastructure.ExtensionsTestInfrastructureDiscoverer;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates the {@link Extension} manifest and DSL resources.
 *
 * @since 4.0
 */
class ExtensionPluginMetadataGenerator {

  private static final String GENERATED_TEST_RESOURCES = "generated-test-resources";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final ExtensionsTestInfrastructureDiscoverer extensionsInfrastructure;
  private final File generatedResourcesBase;
  private final File extensionMulePluginJson;
  private final ExtensionModelLoaderFinder extensionModelLoaderFinder;

  private final List<ExtensionGeneratorEntry> extensionGeneratorEntries = newArrayList();

  /**
   * Creates an instance that will generated metadata for extensions on the baseResourcesFolder
   *
   * @param baseResourcesFolder {@link File} folder to write resources generated for each extension
   */
  ExtensionPluginMetadataGenerator(File baseResourcesFolder) {
    this(baseResourcesFolder, new ExtensionModelLoaderFinder());
  }

  ExtensionPluginMetadataGenerator(File baseResourcesFolder, ExtensionModelLoaderFinder loaderFinder) {
    this.extensionsInfrastructure = new ExtensionsTestInfrastructureDiscoverer(createExtensionManager());
    this.generatedResourcesBase = getGeneratedResourcesBase(baseResourcesFolder);
    this.extensionMulePluginJson = getExtensionMulePluginJsonFile(baseResourcesFolder);
    this.extensionModelLoaderFinder = loaderFinder;
  }

  private File getExtensionMulePluginJsonFile(File baseResourcesFolder) {
    return Paths.get(baseResourcesFolder.getPath(), "classes", META_INF_MULE_PLUGIN).toFile();
  }

  /**
   * Creates the {@value #GENERATED_TEST_RESOURCES} inside the target folder to put metadata files for extensions. If no exists,
   * it will create it.
   *
   * @return {@link File} baseResourcesFolder to write extensions metadata.
   */
  private File getGeneratedResourcesBase(File baseResourcesFolder) {
    File generatedResourcesBase = new File(baseResourcesFolder, GENERATED_TEST_RESOURCES);
    generatedResourcesBase.mkdir();
    return generatedResourcesBase;
  }

  /**
   * Creates a {@link ExtensionManager} needed for generating the metadata for an extension. It would be later discarded due to
   * the manager would have references to classes loaded with the launcher class loader instead of the hierarchical class loaders
   * created as result of the classification process.
   *
   * @return an {@link ExtensionManager} that would be used to register the extensions.
   */
  private ExtensionManager createExtensionManager() {
    DefaultExtensionManager extensionManager = new DefaultExtensionManager();
    final DefaultMuleContext muleContext = new DefaultMuleContext() {

      private final LazyValue<SimpleRegistry> registryCreator =
          new LazyValue<>(() -> new SimpleRegistry(this, new MuleLifecycleInterceptor()));

      @Override
      public MuleRegistry getRegistry() {
        return new MuleRegistryHelper(registryCreator.get(), this);
      }

      @Override
      public Injector getInjector() {
        return registryCreator.get();
      }

    };
    DefaultMuleConfiguration muleConfiguration = new DefaultMuleConfiguration();
    muleConfiguration.setMinMuleVersion(new MuleVersion(getProperty("maven.projectVersion")));
    muleContext.setMuleConfiguration(muleConfiguration);
    try {
      initialiseIfNeeded(extensionManager, muleContext);
    } catch (InitialisationException e) {
      throw new RuntimeException("Error while initialising the extension manager", e);
    }
    return extensionManager;
  }

  /**
   * Scans for a {@link Class} annotated with {@link Extension} annotation and return the {@link Class} or {@code null} if there
   * is no annotated {@link Class}. It would only look for classes when the URLs for the plugin have an artifact not packaged
   * (target/classes/ or target-test/classes).
   *
   * @param plugin the {@link Artifact} to generate its extension manifest if it is an extension.
   * @param urls   {@link URL}s to use for discovering {@link Class}es annotated with {@link Extension}
   * @return {@link Class} annotated with {@link Extension} or {@code null}
   */
  Class scanForExtensionAnnotatedClasses(Artifact plugin, List<URL> urls) {
    final URL firstURL = urls.stream().findFirst().get();
    logger.warn("Scanning plugin '{}' for annotated Extension class from {}", plugin, firstURL);
    logger.warn("Available URLS: {}", urls);

    try {
      Set<String> urlClassNames = findUrlClassNames(firstURL);

      List<Class> extensionsAnnotatedClasses = urlClassNames
          .stream()
          .map(urlClassName -> {
            try {
              return Class.forName(urlClassName);
            } catch (ClassNotFoundException e) {
              List<URL> classpath = new ClassPathUrlProvider().getURLs();
              logger.warn("CLASSPATH URLs:");
              classpath.forEach(url -> logger.warn(url.toString()));
              throw new IllegalArgumentException("Cannot load Extension class '" + urlClassName + " obtained from: '" + firstURL
                  + "' with MD5 '" + getMD5FromFile(firstURL) + "' with last modification on '"
                  + getLastModifiedDateFromUrl(firstURL) + "' using classpath: "
                  + classpath, e);
            }
          })
          .filter(urlClass -> urlClass.getAnnotation(Extension.class) != null
              || urlClass.getAnnotation(org.mule.sdk.api.annotation.Extension.class) != null)
          .collect(toList());

      if (extensionsAnnotatedClasses.isEmpty()) {
        return null;
      }

      if (extensionsAnnotatedClasses.size() > 1) {
        logger
            .warn("While scanning class loader on plugin '{}' for discovering @Extension classes annotated, more than one " +
                "found. It will pick up the first one, found: {}", plugin, extensionsAnnotatedClasses);
      }

      return extensionsAnnotatedClasses.iterator().next();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Set<String> findUrlClassNames(URL firstURL) throws IOException {
    try (final URLClassLoader classLoader = new URLClassLoader(new URL[] {firstURL}, null)) {
      final ConfigurationBuilder reflectionsConfigBuilder = new ConfigurationBuilder()
          .setUrls(firstURL)
          .setScanners(new SubTypesScanner(false), new TypeAnnotationsScanner());
      reflectionsConfigBuilder.setClassLoaders(new ClassLoader[] {classLoader});
      Reflections reflections = new Reflections(reflectionsConfigBuilder);

      return reflections.getAllTypes();
    }
  }

  /**
   * Discovers the extension and builds the {@link ExtensionModel}.
   *
   * @param plugin                         the extension {@link Artifact} plugin
   * @param extensionClass                 the {@link Class} annotated with {@link Extension}
   * @param dependencyResolver             the dependency resolver used to introspect the artifact pom.xml
   * @param rootArtifactRemoteRepositories
   * @return {@link ExtensionModel} for the extensionClass
   */
  private ExtensionModel getExtensionModel(Artifact plugin, Class extensionClass, DependencyResolver dependencyResolver,
                                           List<RemoteRepository> rootArtifactRemoteRepositories) {
    ExtensionModelLoader loader =
        extensionModelLoaderFinder.findLoaderByProperty(plugin, dependencyResolver, rootArtifactRemoteRepositories)
            .orElse(extensionModelLoaderFinder.findLoaderFromMulePlugin(extensionMulePluginJson));
    return extensionsInfrastructure.discoverExtension(extensionClass, loader);
  }

  /**
   * Generates the extension resources for the {@link Artifact} plugin with the {@link Extension}.
   *
   * @param plugin                         the {@link Artifact} to generate its extension manifest if it is an extension.
   * @param extensionClass                 {@link Class} annotated with {@link Extension}
   * @param dependencyResolver             the dependency resolver used to discover test extensions poms to find which loader to
   *                                       use
   * @param rootArtifactRemoteRepositories remote repositories defined at the rootArtifact
   * @return {@link File} folder where extension manifest resources were generated
   */
  File generateExtensionResources(Artifact plugin, Class extensionClass, DependencyResolver dependencyResolver,
                                  List<RemoteRepository> rootArtifactRemoteRepositories) {
    logger.debug("Generating Extension metadata for extension class: '{}'", extensionClass);

    assureSdkApiInClassLoader(extensionClass.getClassLoader(), dependencyResolver, rootArtifactRemoteRepositories);

    final ExtensionModel extensionModel =
        getExtensionModel(plugin, extensionClass, dependencyResolver, rootArtifactRemoteRepositories);
    File generatedResourcesDirectory = new File(generatedResourcesBase, plugin.getArtifactId() + separator + "META-INF");
    generatedResourcesDirectory.mkdirs();
    extensionsInfrastructure.generateLoaderResources(extensionModel, generatedResourcesDirectory);
    extensionGeneratorEntries.add(new ExtensionGeneratorEntry(extensionModel, generatedResourcesDirectory));
    return generatedResourcesDirectory.getParentFile();
  }

  /**
   * Entry class for generating resources for an Extension.
   */
  class ExtensionGeneratorEntry {

    private final ExtensionModel runtimeExtensionModel;
    private final File resourcesFolder;

    ExtensionGeneratorEntry(ExtensionModel runtimeExtensionModel, File resourcesFolder) {
      this.runtimeExtensionModel = runtimeExtensionModel;
      this.resourcesFolder = resourcesFolder;
    }

    public ExtensionModel getExtensionModel() {
      return runtimeExtensionModel;
    }

    File getResourcesFolder() {
      return resourcesFolder;
    }
  }
}
