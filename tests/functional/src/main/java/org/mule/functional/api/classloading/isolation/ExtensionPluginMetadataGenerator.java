/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.api.classloading.isolation;

import static com.google.common.collect.Lists.newArrayList;
import static java.io.File.separator;
import org.mule.functional.junit4.infrastructure.ExtensionsTestInfrastructureDiscoverer;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.registry.DefaultRegistryBroker;
import org.mule.runtime.core.registry.MuleRegistryHelper;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;

import org.eclipse.aether.artifact.Artifact;
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
public class ExtensionPluginMetadataGenerator {

  private static final String GENERATED_TEST_RESOURCES = "generated-test-resources";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final ExtensionsTestInfrastructureDiscoverer extensionsInfrastructure;
  private final File baseResourcesFolder;
  private final File generatedResourcesBase;

  private List<File> extensionsResourcesFolders = newArrayList();

  /**
   * Creates an instance that will generated metadata for extensions on the baseResourcesFolder
   *
   * @param baseResourcesFolder {@link File} folder to write resources generated for each extension
   */
  public ExtensionPluginMetadataGenerator(File baseResourcesFolder) {
    this.extensionsInfrastructure = new ExtensionsTestInfrastructureDiscoverer(createExtensionManager());
    this.baseResourcesFolder = baseResourcesFolder;
    generatedResourcesBase = getGeneratedResourcesBase(baseResourcesFolder);
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
   * Creates a {@link ExtensionManagerAdapter} needed for generating the metadata for an extension. It would be later discarded
   * due to the manager would have references to classes loaded with the launcher class loader instead of the hierarchical class
   * loaders created as result of the classification process.
   *
   * @return an {@link ExtensionManagerAdapter} that would be used to register the extensions.
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
   * Scans for a {@link Class} annotated with {@link Extension} annotation and return the {@link Class} or {@code null} if there
   * is no annotated {@link Class}.
   *
   * @param plugin the {@link Artifact} to generate its extension manifest if it is an extension.
   * @param urls {@link URL}s to use for discovering {@link Class}es annotated with {@link Extension}
   * @return {@link Class} annotated with {@link Extension} or {@code null}
   */
  public Class scanForExtensionAnnotatedClasses(Artifact plugin, List<URL> urls) {
    logger.debug("Scanning plugin '{}' for annotated Extension class", plugin);
    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
    scanner.addIncludeFilter(new AnnotationTypeFilter(Extension.class));
    scanner.setResourceLoader(new PathMatchingResourcePatternResolver(new URLClassLoader(urls.toArray(new URL[0]), null)));
    Set<BeanDefinition> extensionsAnnotatedClasses = scanner.findCandidateComponents("");
    if (extensionsAnnotatedClasses.size() > 1) {
      throw new IllegalStateException("While scanning class loader on plugin '" + plugin
          + "' for discovering @Extension classes annotated, more than one found. Only one should be discovered, found: "
          + extensionsAnnotatedClasses);
    } else if (extensionsAnnotatedClasses.size() == 1) {
      String extensionClassName = extensionsAnnotatedClasses.iterator().next().getBeanClassName();

      try {
        return Class.forName(extensionClassName);
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException("Cannot load Extension class '" + extensionClassName + "'", e);
      }
    }
    return null;
  }

  /**
   * Discovers the extension and builds the {@link RuntimeExtensionModel}.
   *
   * @param plugin the extension {@link Artifact} plugin
   * @param extensionClass the {@link Class} annotated with {@link Extension}
   * @return {@link RuntimeExtensionModel} for the extensionClass
   */
  public RuntimeExtensionModel getExtensionModel(Artifact plugin, Class extensionClass) {
    final StaticVersionResolver versionResolver = new StaticVersionResolver(plugin.getVersion());
    return extensionsInfrastructure.discoverExtension(extensionClass, versionResolver);
  }

  /**
   * Generates the extension manifest for the {@link Artifact} plugin with the {@link Extension}.
   *
   * @param plugin the {@link Artifact} to generate its extension manifest if it is an extension.
   * @param extensionClass {@link Class} annotated with {@link Extension}
   * @return {@link File} folder where extension manifest resources were generated
   */
  public File generateExtensionManifest(Artifact plugin, Class extensionClass) {
    logger.debug("Generating Extension metadata for extension class: '{}'", extensionClass);

    File generatedResourcesDirectory = new File(generatedResourcesBase, plugin.getArtifactId() + separator + "META-INF");
    generatedResourcesDirectory.mkdirs();
    extensionsInfrastructure
        .generateLoaderResources(getExtensionModel(plugin, extensionClass), generatedResourcesDirectory);

    extensionsResourcesFolders.add(generatedResourcesDirectory);

    return generatedResourcesDirectory.getParentFile();
  }

  /**
   * Generates DSL resources for the plugins where extension manifest were generated. This method should be called after all
   * extensions manifest where generated.
   * 
   * <pre>
   *   spring.schemas
   *   spring.handlers
   *   extension.xsd
   * </pre>
   * <p/>
   * These files are going to be generated for each extension registered here.
   */
  // TODO (gfernandes) MULE-10513 Check with AMarra how to generate only the DSL resources for one extension only
  public void generateDslResources() {
    extensionsResourcesFolders.stream()
        .forEach(resourcesFolder -> extensionsInfrastructure.generateDslResources(resourcesFolder));
  }

}
