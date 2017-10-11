/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static com.google.common.collect.Lists.newArrayList;
import static java.io.File.separator;
import static org.mule.runtime.core.internal.exception.ErrorTypeLocatorFactory.createDefaultErrorTypeLocator;
import static org.mule.runtime.core.internal.exception.ErrorTypeRepositoryFactory.createDefaultErrorTypeRepository;
import static org.mule.test.runner.api.MulePluginBasedLoaderFinder.META_INF_MULE_PLUGIN;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.lifecycle.MuleLifecycleInterceptor;
import org.mule.runtime.core.internal.registry.DefaultRegistryBroker;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistryHelper;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.filter.AnnotationTypeFilter;

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

  private List<ExtensionGeneratorEntry> extensionGeneratorEntries = newArrayList();

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
    extensionManager.setMuleContext(new DefaultMuleContext() {

      private ErrorTypeRepository errorTypeRepository = createDefaultErrorTypeRepository();
      private ErrorTypeLocator errorTypeLocator = createDefaultErrorTypeLocator(errorTypeRepository);

      @Override
      public MuleRegistry getRegistry() {
        return new MuleRegistryHelper(new DefaultRegistryBroker(this, new MuleLifecycleInterceptor()),
                                      this);
      }

      @Override
      public ErrorTypeLocator getErrorTypeLocator() {
        return errorTypeLocator;
      }

      @Override
      public ErrorTypeRepository getErrorTypeRepository() {
        return errorTypeRepository;
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
   * Scans for a {@link Class} annotated with {@link Extension} annotation and return the {@link Class} or {@code null} if there
   * is no annotated {@link Class}. It would only look for classes when the URLs for the plugin have an artifact not packaged
   * (target/classes/ or target-test/classes).
   *
   * @param plugin the {@link Artifact} to generate its extension manifest if it is an extension.
   * @param urls {@link URL}s to use for discovering {@link Class}es annotated with {@link Extension}
   * @return {@link Class} annotated with {@link Extension} or {@code null}
   */
  Class scanForExtensionAnnotatedClasses(Artifact plugin, List<URL> urls) {
    final URL firstURL = urls.stream().findFirst().get();
    logger.debug("Scanning plugin '{}' for annotated Extension class from {}", plugin, firstURL);
    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(Extension.class));
    try (URLClassLoader classLoader = new URLClassLoader(new URL[] {firstURL}, null)) {
      scanner.setResourceLoader(new PathMatchingResourcePatternResolver(classLoader));
      Set<BeanDefinition> extensionsAnnotatedClasses = scanner.findCandidateComponents("");
      if (!extensionsAnnotatedClasses.isEmpty()) {
        if (extensionsAnnotatedClasses.size() > 1) {
          logger
              .warn("While scanning class loader on plugin '{}' for discovering @Extension classes annotated, more than one " +
                  "found. It will pick up the first one, found: {}", plugin, extensionsAnnotatedClasses);
        }
        String extensionClassName = extensionsAnnotatedClasses.iterator().next().getBeanClassName();
        try {
          return Class.forName(extensionClassName);
        } catch (ClassNotFoundException e) {
          throw new IllegalArgumentException("Cannot load Extension class '" + extensionClassName + "'", e);
        }
      }
      logger.debug("No class found annotated with {}", Extension.class.getName());
      return null;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Discovers the extension and builds the {@link ExtensionModel}.
   *
   * @param plugin the extension {@link Artifact} plugin
   * @param extensionClass the {@link Class} annotated with {@link Extension}
   * @param dependencyResolver the dependency resolver used to introspect the artifact pom.xml
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
   * @param plugin the {@link Artifact} to generate its extension manifest if it is an extension.
   * @param extensionClass {@link Class} annotated with {@link Extension}
   * @param dependencyResolver the dependency resolver used to discover test extensions poms to find which loader to use
   * @param rootArtifactRemoteRepositories remote repositories defined at the rootArtifact
   * @return {@link File} folder where extension manifest resources were generated
   */
  File generateExtensionResources(Artifact plugin, Class extensionClass, DependencyResolver dependencyResolver,
                                  List<RemoteRepository> rootArtifactRemoteRepositories) {
    logger.debug("Generating Extension metadata for extension class: '{}'", extensionClass);
    final ExtensionModel extensionModel =
        getExtensionModel(plugin, extensionClass, dependencyResolver, rootArtifactRemoteRepositories);
    File generatedResourcesDirectory = new File(generatedResourcesBase, plugin.getArtifactId() + separator + "META-INF");
    generatedResourcesDirectory.mkdirs();
    extensionsInfrastructure.generateLoaderResources(extensionModel, generatedResourcesDirectory);
    extensionsInfrastructure.generateSchemaTestResource(extensionModel, generatedResourcesDirectory);
    extensionGeneratorEntries.add(new ExtensionGeneratorEntry(extensionModel, generatedResourcesDirectory));
    return generatedResourcesDirectory.getParentFile();
  }

  /**
   * Entry class for generating resources for an Extension.
   */
  class ExtensionGeneratorEntry {

    private ExtensionModel runtimeExtensionModel;
    private File resourcesFolder;

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
