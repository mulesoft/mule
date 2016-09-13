/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.classloader;

import static java.lang.Boolean.valueOf;
import static java.lang.System.getProperty;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_LOG_VERBOSE_CLASSLOADING;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_FIRST;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;
import static org.springframework.util.ReflectionUtils.findMethod;
import org.mule.runtime.container.internal.ClasspathModuleDiscoverer;
import org.mule.runtime.container.internal.ContainerClassLoaderFilterFactory;
import org.mule.runtime.container.internal.MuleModule;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilterFactory;
import org.mule.runtime.module.artifact.classloader.ClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.ClassLoaderFilterFactory;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy;
import org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.FilteringArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.util.FileJarExplorer;
import org.mule.runtime.module.artifact.util.JarInfo;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;
import org.mule.test.runner.api.ArtifactClassLoaderHolder;
import org.mule.test.runner.api.ArtifactUrlClassification;
import org.mule.test.runner.api.PluginUrlClassification;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory that creates a class loader hierarchy to emulate the one used in a mule standalone container.
 * <p/>
 * The class loaders created have the following hierarchy:
 * <ul>
 * <li>Container: all the provided scope dependencies plus their dependencies (non test dependencies) and java</li>
 * <li>Plugins (optional): for each plugin a class loader will be created with all the compile scope dependencies and their
 * transitive dependencies (only the ones with scope compile)</li>
 * <li>Application: all the test scope dependencies and their dependencies if they are not defined to be excluded, plus their
 * transitive dependencies (again if they are not excluded).</li>
 * </ul>
 *
 * @since 4.0
 */
public class IsolatedClassLoaderFactory {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());
  private ClassLoaderFilterFactory classLoaderFilterFactory = new ArtifactClassLoaderFilterFactory();
  private DefaultExtensionManager extensionManager = new DefaultExtensionManager();

  /**
   * Creates a {@link ArtifactClassLoaderHolder} containing the container, plugins and application {@link ArtifactClassLoader}s
   *
   * @param extraBootPackages {@link List} of {@link String}s of extra boot packages to be appended to the container
   *        {@link ClassLoader}
   * @param artifactUrlClassification the {@link ArtifactUrlClassification} that defines the different {@link URL}s for each
   *        {@link ClassLoader}
   * @return a {@link ArtifactClassLoaderHolder} that would be used to run the test
   */
  public ArtifactClassLoaderHolder createArtifactClassLoader(List<String> extraBootPackages,
                                                             ArtifactUrlClassification artifactUrlClassification) {
    final TestContainerClassLoaderFactory testContainerClassLoaderFactory =
        new TestContainerClassLoaderFactory(extraBootPackages, artifactUrlClassification.getContainerUrls().toArray(new URL[0]));

    ArtifactClassLoader containerClassLoader =
        createContainerArtifactClassLoader(testContainerClassLoaderFactory, artifactUrlClassification);

    ClassLoaderLookupPolicy childClassLoaderLookupPolicy = testContainerClassLoaderFactory.getContainerClassLoaderLookupPolicy();

    RegionClassLoader regionClassLoader =
        new RegionClassLoader(new ArtifactDescriptor("Region"), containerClassLoader.getClassLoader(),
                              childClassLoaderLookupPolicy);

    final List<ArtifactClassLoader> filteredPluginsArtifactClassLoaders = new ArrayList<>();
    final List<ArtifactClassLoader> pluginsArtifactClassLoaders = new ArrayList<>();
    final List<ArtifactClassLoaderFilter> pluginArtifactClassLoaderFilters = new ArrayList<>();

    if (!artifactUrlClassification.getPluginClassificationUrls().isEmpty()) {
      for (PluginUrlClassification pluginUrlClassification : artifactUrlClassification.getPluginClassificationUrls()) {
        logClassLoaderUrls("PLUGIN (" + pluginUrlClassification.getName() + ")", pluginUrlClassification.getUrls());

        MuleArtifactClassLoader pluginCL = new MuleArtifactClassLoader(new ArtifactDescriptor(pluginUrlClassification.getName()),
                                                                       pluginUrlClassification.getUrls().toArray(new URL[0]),
                                                                       regionClassLoader, childClassLoaderLookupPolicy);
        pluginsArtifactClassLoaders.add(pluginCL);

        ArtifactClassLoaderFilter filter = createArtifactClassLoaderFilter(pluginUrlClassification, pluginCL);

        pluginArtifactClassLoaderFilters.add(filter);
        filteredPluginsArtifactClassLoaders.add(new FilteringArtifactClassLoader(pluginCL, filter));
      }
    }

    final Map<String, ClassLoaderLookupStrategy> pluginsLookupStrategies = new HashMap<>();
    for (int i = 0; i < filteredPluginsArtifactClassLoaders.size(); i++) {
      final ArtifactClassLoaderFilter classLoaderFilter = pluginArtifactClassLoaderFilters.get(i);
      classLoaderFilter.getExportedClassPackages()
          .forEach(p -> pluginsLookupStrategies.put(p, PARENT_FIRST));
    }
    final ClassLoaderLookupPolicy appLookupPolicy = childClassLoaderLookupPolicy.extend(pluginsLookupStrategies);

    ArtifactClassLoader appClassLoader =
        createApplicationArtifactClassLoader(regionClassLoader, appLookupPolicy, artifactUrlClassification);


    final URL url = artifactUrlClassification.getApplicationUrls().get(0);
    final JarInfo testJarInfo = new FileJarExplorer().explore(url);

    regionClassLoader.addClassLoader(appClassLoader,
                                     new DefaultArtifactClassLoaderFilter(testJarInfo.getPackages(), testJarInfo.getResources()));

    for (int i = 0; i < filteredPluginsArtifactClassLoaders.size(); i++) {
      final ArtifactClassLoaderFilter classLoaderFilter = pluginArtifactClassLoaderFilters.get(i);
      regionClassLoader.addClassLoader(filteredPluginsArtifactClassLoaders.get(i), classLoaderFilter);
    }

    return new ArtifactClassLoaderHolder(containerClassLoader, pluginsArtifactClassLoaders, appClassLoader);
  }

  /**
   * Creates an {@link ArtifactClassLoader} for the container. The difference between a mule container {@link ArtifactClassLoader}
   * in standalone mode and this one is that it has to be aware that the parent class loader has all the URLs loaded in launcher
   * app class loader so it has to create a particular look policy to resolve classes as CHILD_FIRST.
   * <p/>
   * In order to do that a {@link FilteringArtifactClassLoader} resolve is created with and empty look policy (meaning that
   * CHILD_FIRST strategy will be used) for the {@link URL}s that are going to be exposed from the container class loader. This
   * would be the parent class loader for the container so instead of going directly the launcher application class loader that
   * has access to the whole classpath this filtering class loader will resolve only the classes for the {@link URL}s defined to
   * be in the container.
   *
   * @param testContainerClassLoaderFactory {@link TestContainerClassLoaderFactory} that has the logic to create a container class
   *        loader
   * @param artifactUrlClassification the classifications to get plugins {@link URL}s
   * @return an {@link ArtifactClassLoader} for the container
   */
  protected ArtifactClassLoader createContainerArtifactClassLoader(TestContainerClassLoaderFactory testContainerClassLoaderFactory,
                                                                   ArtifactUrlClassification artifactUrlClassification) {
    MuleArtifactClassLoader launcherArtifact = createLauncherArtifactClassLoader(artifactUrlClassification);
    final List<MuleModule> muleModules = Collections.<MuleModule>emptyList();
    ClassLoaderFilter filteredClassLoaderLauncher = new ContainerClassLoaderFilterFactory()
        .create(testContainerClassLoaderFactory.getBootPackages(), muleModules);

    logClassLoaderUrls("CONTAINER", artifactUrlClassification.getContainerUrls());
    return testContainerClassLoaderFactory
        .createContainerClassLoader(new FilteringArtifactClassLoader(launcherArtifact, filteredClassLoaderLauncher));
  }

  /**
   * Creates the launcher application class loader to delegate from container class loader. It adds the {@link URL}s discovered
   * for the container class loader and boot/launcher class loader that are not already present. This is needed due to while
   * resolving the container class loader artifacts could be discovered that are not present in classpath due to they are not
   * defined as dependencies.
   *
   * @param artifactUrlClassification
   * @return an {@link ArtifactClassLoader} for the launcher, parent of container
   */
  protected MuleArtifactClassLoader createLauncherArtifactClassLoader(ArtifactUrlClassification artifactUrlClassification) {
    ClassLoader launcherClassLoader = IsolatedClassLoaderFactory.class.getClassLoader();

    Method method = findMethod(launcherClassLoader.getClass(), "addURL", URL.class);
    method.setAccessible(true);

    try {
      for (URL url : artifactUrlClassification.getContainerUrls()) {
        method.invoke(launcherClassLoader, url);
      }
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("Error while appending URLs to launcher class loader", e);
    }

    return new MuleArtifactClassLoader(new ArtifactDescriptor("launcher"), new URL[0], launcherClassLoader,
                                       new MuleClassLoaderLookupPolicy(Collections.emptyMap(), Collections.<String>emptySet()));
  }

  private ArtifactClassLoaderFilter createArtifactClassLoaderFilter(PluginUrlClassification pluginUrlClassification,
                                                                    MuleArtifactClassLoader pluginCL) {
    Collection<String> exportedPackagesProperty;
    Collection<String> exportedResourcesProperty;
    URL manifestUrl = pluginCL.findResource("META-INF/" + EXTENSION_MANIFEST_FILE_NAME);
    if (manifestUrl != null) {
      logger.debug("Plugin '{}' has extension descriptor therefore it will be handled as an extension",
                   pluginUrlClassification.getName());
      ExtensionManifest extensionManifest = extensionManager.parseExtensionManifestXml(manifestUrl);
      exportedPackagesProperty = extensionManifest.getExportedPackages();
      exportedResourcesProperty = extensionManifest.getExportedResources();
    } else {
      logger.debug("Plugin '{}' will be handled as standard plugin, it is not an extension", pluginUrlClassification.getName());
      ClassLoader pluginArtifactClassLoaderToDiscoverModules =
          new URLClassLoader(pluginUrlClassification.getUrls().toArray(new URL[0]), null);
      List<MuleModule> modules =
          withContextClassLoader(pluginArtifactClassLoaderToDiscoverModules,
                                 () -> new ClasspathModuleDiscoverer(pluginArtifactClassLoaderToDiscoverModules).discover());
      MuleModule module = validatePluginModule(pluginUrlClassification.getName(), modules);

      exportedPackagesProperty = module.getExportedPackages();
      exportedResourcesProperty = module.getExportedPackages();
    }
    String exportedPackages = exportedPackagesProperty.stream().collect(Collectors.joining(", "));
    final String exportedResources = exportedResourcesProperty.stream().collect(Collectors.joining(", "));
    ArtifactClassLoaderFilter artifactClassLoaderFilter =
        classLoaderFilterFactory.create(exportedPackages, exportedResources);

    if (!pluginUrlClassification.getExportClasses().isEmpty()) {
      artifactClassLoaderFilter =
          new TestArtifactClassLoaderFilter(artifactClassLoaderFilter, pluginUrlClassification.getExportClasses());
    }
    return artifactClassLoaderFilter;
  }

  /**
   * Creates an {@link ArtifactClassLoader} for the application.
   *
   * @param parent the parent class loader to be assigned to the new one created here
   * @param childClassLoaderLookupPolicy look policy to be used
   * @param artifactUrlClassification the url classifications to get plugins urls
   * @return the {@link ArtifactClassLoader} to be used for running the test
   */
  protected ArtifactClassLoader createApplicationArtifactClassLoader(ClassLoader parent,
                                                                     ClassLoaderLookupPolicy childClassLoaderLookupPolicy,
                                                                     ArtifactUrlClassification artifactUrlClassification) {
    logClassLoaderUrls("APP", artifactUrlClassification.getApplicationUrls());
    return new MuleArtifactClassLoader(new ArtifactDescriptor("app"),
                                       artifactUrlClassification.getApplicationUrls().toArray(new URL[0]), parent,
                                       childClassLoaderLookupPolicy);
  }

  /**
   * Logs the {@link List} of {@link URL}s for the classLoaderName
   *
   * @param classLoaderName the name of the {@link ClassLoader} to be logged
   * @param urls {@link List} of {@link URL}s that are going to be used for the {@link ClassLoader}
   */
  protected void logClassLoaderUrls(final String classLoaderName, final List<URL> urls) {
    StringBuilder builder = new StringBuilder(classLoaderName).append(" classloader urls: [");
    urls.stream().forEach(e -> builder.append("\n").append(" ").append(e.getFile()));
    builder.append("\n]");
    logClassLoadingTrace(builder.toString());
  }

  /**
   * Logs the message with info severity if {@link org.mule.runtime.core.api.config.MuleProperties#MULE_LOG_VERBOSE_CLASSLOADING}
   * is set or trace severity
   *
   * @param message the message to be logged
   */
  private void logClassLoadingTrace(String message) {
    if (isVerboseClassLoading()) {
      logger.info(message);
    } else {
      logger.debug(message);
    }
  }

  /**
   * @return true if {@link org.mule.runtime.core.api.config.MuleProperties#MULE_LOG_VERBOSE_CLASSLOADING} is set to true
   */
  private Boolean isVerboseClassLoading() {
    return valueOf(getProperty(MULE_LOG_VERBOSE_CLASSLOADING));
  }

  /**
   * Validates that only one module should be discovered. A plugin cannot have inside another plugin that holds a
   * {@code mule-module.properties} for the time being.
   *
   * @param pluginName the plugin name
   * @param discoveredModules {@link MuleModule} discovered
   * @return the first Module from the list due to there should be only one module.
   */
  private MuleModule validatePluginModule(String pluginName, List<MuleModule> discoveredModules) {
    if (discoveredModules.size() == 0) {
      throw new IllegalStateException(pluginName
          + " doesn't have in its classpath a mule-module.properties to define what packages and resources should expose");
    }
    if (discoveredModules.size() > 1) {
      throw new IllegalStateException(pluginName + " has more than one mule-module.properties, composing plugins is not allowed");
    }
    return discoveredModules.get(0);
  }

}
