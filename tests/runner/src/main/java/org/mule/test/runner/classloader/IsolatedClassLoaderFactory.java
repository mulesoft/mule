/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.classloader;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Boolean.valueOf;
import static java.lang.System.getProperty;
import static java.util.stream.Collectors.joining;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_LOG_VERBOSE_CLASSLOADING;
import static org.mule.runtime.deployment.model.internal.AbstractArtifactClassLoaderBuilder.getArtifactPluginId;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.CHILD_ONLY;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_FIRST;
import org.mule.runtime.container.internal.ContainerClassLoaderFilterFactory;
import org.mule.runtime.container.internal.MuleModule;
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
import org.mule.runtime.module.artifact.util.JarExplorer;
import org.mule.runtime.module.artifact.util.JarInfo;
import org.mule.test.runner.api.ArtifactClassLoaderHolder;
import org.mule.test.runner.api.ArtifactUrlClassification;
import org.mule.test.runner.api.ArtifactsUrlClassification;
import org.mule.test.runner.api.PluginUrlClassification;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  /**
   * Creates a {@link ArtifactClassLoaderHolder} containing the container, plugins and application {@link ArtifactClassLoader}s
   *
   * @param extraBootPackages          {@link List} of {@link String}s of extra boot packages to be appended to the container
   *                                   {@link ClassLoader}
   * @param artifactsUrlClassification the {@link ArtifactsUrlClassification} that defines the different {@link URL}s for each
   *                                   {@link ClassLoader}
   * @return a {@link ArtifactClassLoaderHolder} that would be used to run the test
   */
  public ArtifactClassLoaderHolder createArtifactClassLoader(List<String> extraBootPackages,
                                                             ArtifactsUrlClassification artifactsUrlClassification) {
    ArtifactClassLoader containerClassLoader;
    ClassLoaderLookupPolicy childClassLoaderLookupPolicy;
    try (final TestContainerClassLoaderFactory testContainerClassLoaderFactory =
        new TestContainerClassLoaderFactory(extraBootPackages,
                                            artifactsUrlClassification.getContainerUrls().toArray(new URL[0]))) {

      containerClassLoader =
          createContainerArtifactClassLoader(testContainerClassLoaderFactory, artifactsUrlClassification);

      childClassLoaderLookupPolicy =
          testContainerClassLoaderFactory.getContainerClassLoaderLookupPolicy();

    }
    List<ArtifactClassLoader> serviceArtifactClassLoaders =
        createServiceClassLoaders(containerClassLoader.getClassLoader(), childClassLoaderLookupPolicy,
                                  artifactsUrlClassification);

    RegionClassLoader regionClassLoader =
        new RegionClassLoader("Region", new ArtifactDescriptor("Region"), containerClassLoader.getClassLoader(),
                              childClassLoaderLookupPolicy);

    final List<ArtifactClassLoader> filteredPluginsArtifactClassLoaders = new ArrayList<>();
    final List<ArtifactClassLoader> pluginsArtifactClassLoaders = new ArrayList<>();
    final List<ArtifactClassLoaderFilter> pluginArtifactClassLoaderFilters = new ArrayList<>();

    if (!artifactsUrlClassification.getPluginUrlClassifications().isEmpty()) {
      for (PluginUrlClassification pluginUrlClassification : artifactsUrlClassification.getPluginUrlClassifications()) {
        logClassLoaderUrls("PLUGIN (" + pluginUrlClassification.getName() + ")", pluginUrlClassification.getUrls());

        String artifactId = getArtifactPluginId(regionClassLoader.getArtifactId(), pluginUrlClassification.getName());
        MuleArtifactClassLoader pluginCL =
            new MuleArtifactClassLoader(artifactId,
                                        new ArtifactDescriptor(pluginUrlClassification.getName()),
                                        pluginUrlClassification.getUrls().toArray(new URL[0]),
                                        regionClassLoader,
                                        buildLookupPolicyForPlugin(pluginUrlClassification,
                                                                   artifactsUrlClassification.getPluginUrlClassifications(),
                                                                   childClassLoaderLookupPolicy));
        pluginsArtifactClassLoaders.add(pluginCL);

        ArtifactClassLoaderFilter filter = createArtifactClassLoaderFilter(pluginUrlClassification);

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
        createApplicationArtifactClassLoader(regionClassLoader, appLookupPolicy, artifactsUrlClassification);

    JarInfo testJarInfo = getJarInfo(artifactsUrlClassification);

    regionClassLoader.addClassLoader(appClassLoader,
                                     new DefaultArtifactClassLoaderFilter(testJarInfo.getPackages(), testJarInfo.getResources()));

    for (int i = 0; i < filteredPluginsArtifactClassLoaders.size(); i++) {
      final ArtifactClassLoaderFilter classLoaderFilter = pluginArtifactClassLoaderFilters.get(i);
      regionClassLoader.addClassLoader(filteredPluginsArtifactClassLoaders.get(i), classLoaderFilter);
    }

    return new ArtifactClassLoaderHolder(containerClassLoader, serviceArtifactClassLoaders, pluginsArtifactClassLoaders,
                                         appClassLoader);
  }

  /**
   * Extends the parent look up policies by taking into account dependencies between the current plugin being classified and the whole list
   * of plugins that will go the region. If the current plugin declares the dependency to a plugin that is in the region too, the packages
   * exported by the dependent plugin should be added to the lookup policy for the current pluing as {@link ClassLoaderLookupStrategy#PARENT_FIRST},
   * in order to allow resolve them.
   * <p/>
   * Otherwise it should be added as {@link ClassLoaderLookupStrategy#CHILD_FIRST} in order to fail when trying to access classes from other plugins that are not delcared as dependent.
   *
   * @param currentPluginClassification {@link PluginUrlClassification} being classified.
   * @param pluginUrlClassifications list of {@link PluginUrlClassification} that will go to the region, including current being classified too.
   * @param parentLookupPolicies the parent (region) {@link ClassLoaderLookupPolicy}.
   * @return the extended {@link ClassLoaderLookupPolicy} that should be used for the current plugin classified.
   */
  private ClassLoaderLookupPolicy buildLookupPolicyForPlugin(PluginUrlClassification currentPluginClassification,
                                                             List<PluginUrlClassification> pluginUrlClassifications,
                                                             ClassLoaderLookupPolicy parentLookupPolicies) {
    Map<String, ClassLoaderLookupStrategy> pluginsLookupPolicies = new HashMap<>();
    for (PluginUrlClassification dependencyPluginClassification : pluginUrlClassifications) {
      if (dependencyPluginClassification.getArtifactId().equals(currentPluginClassification.getArtifactId())) {
        continue;
      }

      ClassLoaderLookupStrategy lookUpPolicyStrategy =
          getClassLoaderLookupStrategy(currentPluginClassification, dependencyPluginClassification);

      for (String exportedPackage : dependencyPluginClassification.getExportedPackages()) {
        pluginsLookupPolicies.put(exportedPackage, lookUpPolicyStrategy);
      }

    }
    return parentLookupPolicies.extend(pluginsLookupPolicies);
  }

  /**
   * If the plugin declares the dependency the {@link PluginUrlClassification} would be {@link ClassLoaderLookupStrategy#PARENT_FIRST}
   * otherwise {@link ClassLoaderLookupStrategy#CHILD_FIRST}.
   *
   * @param currentPluginClassification {@link PluginUrlClassification} being classified.
   * @param dependencyPluginClassification {@link PluginUrlClassification} from the region.
   * @return {@link ClassLoaderLookupStrategy} to be used by current plugin for the exported packages defined by the dependencyPluginClassification.
   */
  private ClassLoaderLookupStrategy getClassLoaderLookupStrategy(PluginUrlClassification currentPluginClassification,
                                                                 PluginUrlClassification dependencyPluginClassification) {
    final ClassLoaderLookupStrategy parentFirst;
    if (currentPluginClassification.getPluginDependencies().contains(dependencyPluginClassification.getName())) {
      parentFirst = PARENT_FIRST;
    } else {
      parentFirst = CHILD_ONLY;
    }
    return parentFirst;
  }

  /**
   * For each service defined in the classification it creates an {@link ArtifactClassLoader} wit the name defined in
   * classification.
   *
   * @param parent                       the parent class loader to be assigned to the new one created here
   * @param childClassLoaderLookupPolicy look policy to be used
   * @param artifactsUrlClassification   the url classifications to get service {@link URL}s
   * @return a list of {@link ArtifactClassLoader} for service class loaders
   */
  protected List<ArtifactClassLoader> createServiceClassLoaders(ClassLoader parent,
                                                                ClassLoaderLookupPolicy childClassLoaderLookupPolicy,
                                                                ArtifactsUrlClassification artifactsUrlClassification) {
    List<ArtifactClassLoader> servicesArtifactClassLoaders = newArrayList();
    for (ArtifactUrlClassification serviceUrlClassification : artifactsUrlClassification.getServiceUrlClassifications()) {
      logClassLoaderUrls("SERVICE (" + serviceUrlClassification.getArtifactId() + ")", serviceUrlClassification.getUrls());

      MuleArtifactClassLoader artifactClassLoader =
          new MuleArtifactClassLoader(serviceUrlClassification.getName(),
                                      new ArtifactDescriptor(serviceUrlClassification.getName()),
                                      serviceUrlClassification.getUrls().toArray(new URL[0]), parent,
                                      childClassLoaderLookupPolicy);
      servicesArtifactClassLoaders.add(artifactClassLoader);
    }
    return servicesArtifactClassLoaders;
  }

  /**
   * Creates the {@link JarInfo} for the {@link ArtifactsUrlClassification}.
   *
   * @param artifactsUrlClassification the {@link ArtifactsUrlClassification} that defines the different {@link URL}s for each
   *                                   {@link ClassLoader}
   * @return {@link JarInfo} for the classification
   */
  private JarInfo getJarInfo(ArtifactsUrlClassification artifactsUrlClassification) {
    Set<String> packages = new HashSet<>();
    Set<String> resources = new HashSet<>();
    final JarExplorer jarExplorer = new FileJarExplorer();

    List<URL> libraries = newArrayList(artifactsUrlClassification.getApplicationUrls().get(0));
    libraries.addAll(artifactsUrlClassification.getPluginSharedLibUrls());

    for (URL library : libraries) {
      final JarInfo jarInfo = jarExplorer.explore(library);
      packages.addAll(jarInfo.getPackages());
      resources.addAll(jarInfo.getResources());
    }

    return new JarInfo(packages, resources);
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
   *                                        loader
   * @param artifactsUrlClassification      the classifications to get plugins {@link URL}s
   * @return an {@link ArtifactClassLoader} for the container
   */
  protected ArtifactClassLoader createContainerArtifactClassLoader(
                                                                   TestContainerClassLoaderFactory testContainerClassLoaderFactory,
                                                                   ArtifactsUrlClassification artifactsUrlClassification) {
    MuleArtifactClassLoader launcherArtifact = createLauncherArtifactClassLoader();
    final List<MuleModule> muleModules = Collections.<MuleModule>emptyList();
    ClassLoaderFilter filteredClassLoaderLauncher = new ContainerClassLoaderFilterFactory()
        .create(testContainerClassLoaderFactory.getBootPackages(), muleModules);

    logClassLoaderUrls("CONTAINER", artifactsUrlClassification.getContainerUrls());
    return testContainerClassLoaderFactory
        .createContainerClassLoader(new FilteringArtifactClassLoader(launcherArtifact, filteredClassLoaderLauncher));
  }

  /**
   * Creates the launcher application class loader to delegate from container class loader.
   *
   * @return an {@link ArtifactClassLoader} for the launcher, parent of container
   */
  protected MuleArtifactClassLoader createLauncherArtifactClassLoader() {
    ClassLoader launcherClassLoader = IsolatedClassLoaderFactory.class.getClassLoader();

    return new MuleArtifactClassLoader("launcher", new ArtifactDescriptor("launcher"), new URL[0], launcherClassLoader,
                                       new MuleClassLoaderLookupPolicy(Collections.emptyMap(), Collections.emptySet())) {

      @Override
      public URL findResource(String name) {
        URL url = super.findResource(name);
        if (url == null && getParent() != null) {
          url = getParent().getResource(name);
        }
        return url;
      }
    };
  }

  private ArtifactClassLoaderFilter createArtifactClassLoaderFilter(PluginUrlClassification pluginUrlClassification) {
    String exportedPackages = pluginUrlClassification.getExportedPackages().stream().collect(joining(", "));
    final String exportedResources = pluginUrlClassification.getExportedResources().stream().collect(joining(", "));
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
   * @param parent                       the parent class loader to be assigned to the new one created here
   * @param childClassLoaderLookupPolicy look policy to be used
   * @param artifactsUrlClassification   the url classifications to get plugins urls
   * @return the {@link ArtifactClassLoader} to be used for running the test
   */
  protected ArtifactClassLoader createApplicationArtifactClassLoader(ClassLoader parent,
                                                                     ClassLoaderLookupPolicy childClassLoaderLookupPolicy,
                                                                     ArtifactsUrlClassification artifactsUrlClassification) {
    logClassLoaderUrls("APP", artifactsUrlClassification.getApplicationUrls());
    return new MuleArtifactClassLoader("app", new ArtifactDescriptor("app"),
                                       artifactsUrlClassification.getApplicationUrls().toArray(new URL[0]), parent,
                                       childClassLoaderLookupPolicy);
  }

  /**
   * Logs the {@link List} of {@link URL}s for the classLoaderName
   *
   * @param classLoaderName the name of the {@link ClassLoader} to be logged
   * @param urls            {@link List} of {@link URL}s that are going to be used for the {@link ClassLoader}
   */
  protected void logClassLoaderUrls(final String classLoaderName, final List<URL> urls) {
    StringBuilder builder = new StringBuilder(classLoaderName).append(" classloader urls: [");
    urls.stream().forEach(e -> builder.append("\n").append(" ").append(e));
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

}
