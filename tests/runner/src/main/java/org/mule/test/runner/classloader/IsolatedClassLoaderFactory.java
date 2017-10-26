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
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.io.FileUtils.toFile;
import static org.mule.runtime.container.internal.ContainerClassLoaderFactory.SYSTEM_PACKAGES;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_LOG_VERBOSE_CLASSLOADING;
import static org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory.getArtifactPluginId;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import static org.mule.test.runner.RunnerConfiguration.TEST_RUNNER_ARTIFACT_ID;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.container.internal.ContainerClassLoaderFactory;
import org.mule.runtime.container.internal.ContainerClassLoaderFilterFactory;
import org.mule.runtime.container.internal.ContainerOnlyLookupStrategy;
import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.runtime.container.internal.MuleClassLoaderLookupPolicy;
import org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoader;
import org.mule.runtime.deployment.model.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFilterFactory;
import org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderFilter;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderFilterFactory;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.DefaultArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.api.classloader.FilteringArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.internal.util.FileJarExplorer;
import org.mule.runtime.module.artifact.internal.util.JarExplorer;
import org.mule.runtime.module.artifact.internal.util.JarInfo;
import org.mule.test.runner.api.ArtifactClassLoaderHolder;
import org.mule.test.runner.api.ArtifactUrlClassification;
import org.mule.test.runner.api.ArtifactsUrlClassification;
import org.mule.test.runner.api.PluginUrlClassification;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

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

  private static final Logger LOGGER = getLogger(IsolatedClassLoaderFactory.class);

  private static final String APP_NAME = "app";

  private ClassLoaderFilterFactory classLoaderFilterFactory = new ArtifactClassLoaderFilterFactory();
  private PluginLookPolicyFactory pluginLookupPolicyGenerator = new PluginLookPolicyFactory();

  /**
   * Creates a {@link ArtifactClassLoaderHolder} containing the container, plugins and application {@link ArtifactClassLoader}s
   *
   * @param extraBootPackages {@link List} of {@link String}s of extra boot packages to be appended to the container
   *        {@link ClassLoader}
   * @param extraPrivilegedArtifacts {@link List} of {@link String}s of extra privileged artifacts. Each value needs to have the
   *        form groupId:versionId.
   * @param artifactsUrlClassification the {@link ArtifactsUrlClassification} that defines the different {@link URL}s for each
   *        {@link ClassLoader}
   * @return a {@link ArtifactClassLoaderHolder} that would be used to run the test
   */
  public ArtifactClassLoaderHolder createArtifactClassLoader(List<String> extraBootPackages,
                                                             Set<String> extraPrivilegedArtifacts,
                                                             ArtifactsUrlClassification artifactsUrlClassification) {
    Map<String, LookupStrategy> appExportedLookupStrategies = new HashMap<>();
    JarInfo testJarInfo = getAppSharedPackages(artifactsUrlClassification.getApplicationSharedLibUrls());
    testJarInfo.getPackages().stream().forEach(p -> appExportedLookupStrategies.put(p, PARENT_FIRST));

    ArtifactClassLoader containerClassLoader;
    ClassLoaderLookupPolicy childClassLoaderLookupPolicy;
    RegionClassLoader regionClassLoader;
    final List<ArtifactClassLoader> filteredPluginsArtifactClassLoaders = new ArrayList<>();
    final List<ArtifactClassLoader> pluginsArtifactClassLoaders = new ArrayList<>();
    final List<ArtifactClassLoaderFilter> pluginArtifactClassLoaderFilters = new ArrayList<>();
    List<ArtifactClassLoader> serviceArtifactClassLoaders;

    DefaultModuleRepository moduleRepository =
        new DefaultModuleRepository(new TestModuleDiscoverer(extraPrivilegedArtifacts,
                                                             new TestContainerModuleDiscoverer(ContainerClassLoaderFactory.class
                                                                 .getClassLoader())));

    try (final TestContainerClassLoaderFactory testContainerClassLoaderFactory =
        new TestContainerClassLoaderFactory(extraBootPackages, artifactsUrlClassification.getContainerUrls().toArray(new URL[0]),
                                            moduleRepository)) {

      final Map<String, LookupStrategy> pluginsLookupStrategies = new HashMap<>();

      for (PluginUrlClassification pluginUrlClassification : artifactsUrlClassification.getPluginUrlClassifications()) {
        pluginUrlClassification.getExportedPackages().forEach(p -> pluginsLookupStrategies.put(p, PARENT_FIRST));
      }

      containerClassLoader =
          createContainerArtifactClassLoader(testContainerClassLoaderFactory, artifactsUrlClassification);

      childClassLoaderLookupPolicy =
          testContainerClassLoaderFactory.getContainerClassLoaderLookupPolicy(containerClassLoader.getClassLoader());
      final ClassLoaderLookupPolicy appLookupPolicy = childClassLoaderLookupPolicy.extend(pluginsLookupStrategies);

      serviceArtifactClassLoaders = createServiceClassLoaders(containerClassLoader.getClassLoader(), childClassLoaderLookupPolicy,
                                                              artifactsUrlClassification);

      regionClassLoader =
          new RegionClassLoader("Region", new ArtifactDescriptor("Region"), containerClassLoader.getClassLoader(),
                                childClassLoaderLookupPolicy);

      if (!artifactsUrlClassification.getPluginUrlClassifications().isEmpty()) {
        for (PluginUrlClassification pluginUrlClassification : artifactsUrlClassification.getPluginUrlClassifications()) {
          logClassLoaderUrls("PLUGIN (" + pluginUrlClassification.getName() + ")", pluginUrlClassification.getUrls());

          String artifactId = getArtifactPluginId(regionClassLoader.getArtifactId(), pluginUrlClassification.getName());

          ClassLoaderLookupPolicy pluginLookupPolicy =
              extendLookupPolicyForPrivilegedAccess(childClassLoaderLookupPolicy, moduleRepository,
                                                    testContainerClassLoaderFactory,
                                                    pluginUrlClassification);
          pluginLookupPolicy = pluginLookupPolicy.extend(appExportedLookupStrategies);

          MuleArtifactClassLoader pluginCL =
              new MuleArtifactClassLoader(artifactId,
                                          new ArtifactDescriptor(pluginUrlClassification.getName()),
                                          pluginUrlClassification.getUrls().toArray(new URL[0]),
                                          regionClassLoader,
                                          pluginLookupPolicyGenerator.createLookupPolicy(pluginUrlClassification,
                                                                                         artifactsUrlClassification
                                                                                             .getPluginUrlClassifications(),
                                                                                         pluginLookupPolicy,
                                                                                         pluginsArtifactClassLoaders));
          pluginsArtifactClassLoaders.add(pluginCL);

          ArtifactClassLoaderFilter filter =
              createArtifactClassLoaderFilter(pluginUrlClassification, testJarInfo.getPackages(), childClassLoaderLookupPolicy);

          pluginArtifactClassLoaderFilters.add(filter);
          filteredPluginsArtifactClassLoaders.add(new FilteringArtifactClassLoader(pluginCL, filter, emptyList()));
        }

        createTestRunnerPlugin(artifactsUrlClassification, appExportedLookupStrategies, childClassLoaderLookupPolicy,
                               regionClassLoader, filteredPluginsArtifactClassLoaders, pluginsArtifactClassLoaders,
                               pluginArtifactClassLoaderFilters, moduleRepository, testContainerClassLoaderFactory,
                               testJarInfo.getPackages());
      }

      ArtifactClassLoader appClassLoader =
          createApplicationArtifactClassLoader(regionClassLoader, appLookupPolicy, artifactsUrlClassification,
                                               pluginsArtifactClassLoaders);

      regionClassLoader.addClassLoader(appClassLoader,
                                       new DefaultArtifactClassLoaderFilter(testJarInfo.getPackages(),
                                                                            testJarInfo.getResources()));

      for (int i = 0; i < filteredPluginsArtifactClassLoaders.size(); i++) {
        final ArtifactClassLoaderFilter classLoaderFilter = pluginArtifactClassLoaderFilters.get(i);
        regionClassLoader.addClassLoader(filteredPluginsArtifactClassLoaders.get(i), classLoaderFilter);
      }

      return new ArtifactClassLoaderHolder(containerClassLoader, serviceArtifactClassLoaders, pluginsArtifactClassLoaders,
                                           appClassLoader);
    }
  }

  private JarInfo getAppSharedPackages(List<URL> pluginSharedLibUrls) {
    List<URL> libraries = newArrayList();
    libraries.addAll(pluginSharedLibUrls);

    return getLibraryPackages(libraries);
  }

  private JarInfo getLibraryPackages(List<URL> libraries) {
    Set<String> packages = new HashSet<>();
    Set<String> resources = new HashSet<>();
    final JarExplorer jarExplorer = new FileJarExplorer();

    for (URL library : libraries) {
      try {
        JarInfo jarInfo = jarExplorer.explore(library.toURI());
        packages.addAll(jarInfo.getPackages());
        resources.addAll(jarInfo.getResources());
      } catch (URISyntaxException e) {
        throw new MuleRuntimeException(e);
      }
    }

    return new JarInfo(packages, resources);
  }

  private void createTestRunnerPlugin(ArtifactsUrlClassification artifactsUrlClassification,
                                      Map<String, LookupStrategy> appExportedLookupStrategies,
                                      ClassLoaderLookupPolicy childClassLoaderLookupPolicy, RegionClassLoader regionClassLoader,
                                      List<ArtifactClassLoader> filteredPluginsArtifactClassLoaders,
                                      List<ArtifactClassLoader> pluginsArtifactClassLoaders,
                                      List<ArtifactClassLoaderFilter> pluginArtifactClassLoaderFilters,
                                      DefaultModuleRepository moduleRepository,
                                      TestContainerClassLoaderFactory testContainerClassLoaderFactory,
                                      Set<String> parentExportedPackages) {

    JarInfo testRunnerJarInfo = getTestRunnerJarInfo(artifactsUrlClassification);

    String testRunnerArtifactId = getArtifactPluginId(regionClassLoader.getArtifactId(), "test-runner");


    List<String> pluginDependencies =
        artifactsUrlClassification.getPluginUrlClassifications().stream().map(p -> p.getName()).collect(toList());



    PluginUrlClassification testRunnerPluginClassification =
        new PluginUrlClassification(TEST_RUNNER_ARTIFACT_ID + ":", artifactsUrlClassification.getTestRunnerLibUrls(), emptyList(),
                                    pluginDependencies, testRunnerJarInfo.getPackages(), testRunnerJarInfo.getResources(),
                                    emptySet(), emptySet());

    ClassLoaderLookupPolicy pluginLookupPolicy =
        extendLookupPolicyForPrivilegedAccess(childClassLoaderLookupPolicy, moduleRepository,
                                              testContainerClassLoaderFactory,
                                              testRunnerPluginClassification);
    pluginLookupPolicy = pluginLookupPolicy.extend(appExportedLookupStrategies);

    MuleArtifactClassLoader pluginCL =
        new MuleArtifactClassLoader(testRunnerArtifactId,
                                    new ArtifactDescriptor(testRunnerPluginClassification.getName()),
                                    testRunnerPluginClassification.getUrls().toArray(new URL[0]),
                                    regionClassLoader,
                                    pluginLookupPolicyGenerator.createLookupPolicy(testRunnerPluginClassification,
                                                                                   artifactsUrlClassification
                                                                                       .getPluginUrlClassifications(),
                                                                                   pluginLookupPolicy,
                                                                                   pluginsArtifactClassLoaders));
    pluginsArtifactClassLoaders.add(pluginCL);

    ArtifactClassLoaderFilter filter =
        createArtifactClassLoaderFilter(testRunnerPluginClassification, parentExportedPackages, childClassLoaderLookupPolicy);

    pluginArtifactClassLoaderFilters.add(filter);
    filteredPluginsArtifactClassLoaders.add(new FilteringArtifactClassLoader(pluginCL, filter, emptyList()));

    logClassLoaderUrls("PLUGIN (" + testRunnerPluginClassification.getName() + ")", testRunnerPluginClassification.getUrls());
  }

  private JarInfo getTestRunnerJarInfo(ArtifactsUrlClassification artifactsUrlClassification) {
    JarInfo testJarInfo = getTestJarInfo(artifactsUrlClassification);
    Set<String> exportedPackages = testJarInfo.getPackages();
    Set<String> exportedResources = testJarInfo.getResources();

    artifactsUrlClassification.getTestRunnerExportedLibUrls().forEach(
                                                                      url -> {
                                                                        JarInfo jarInfo = getTestCodePackages(url);
                                                                        exportedPackages.addAll(jarInfo.getPackages());
                                                                        exportedResources.addAll(jarInfo.getResources());
                                                                      });

    return new JarInfo(exportedPackages, exportedResources);
  }

  private ClassLoaderLookupPolicy extendLookupPolicyForPrivilegedAccess(ClassLoaderLookupPolicy childClassLoaderLookupPolicy,
                                                                        ModuleRepository moduleRepository,
                                                                        TestContainerClassLoaderFactory testContainerClassLoaderFactory,
                                                                        PluginUrlClassification pluginUrlClassification) {
    ContainerOnlyLookupStrategy containerOnlyLookupStrategy =
        new ContainerOnlyLookupStrategy(testContainerClassLoaderFactory.getContainerClassLoader().getClassLoader());

    Map<String, LookupStrategy> privilegedLookupStrategies = new HashMap<>();
    for (MuleModule module : moduleRepository.getModules()) {
      if (hasPrivilegedApiAccess(pluginUrlClassification, module)) {
        for (String packageName : module.getPrivilegedExportedPackages()) {
          privilegedLookupStrategies.put(packageName, containerOnlyLookupStrategy);
        }
      }
    }

    if (privilegedLookupStrategies.isEmpty()) {
      return childClassLoaderLookupPolicy;
    } else {
      return childClassLoaderLookupPolicy.extend(privilegedLookupStrategies);
    }
  }

  private boolean hasPrivilegedApiAccess(PluginUrlClassification pluginUrlClassification, MuleModule module) {
    return module.getPrivilegedArtifacts().stream()
        .filter(artifact -> pluginUrlClassification.getName().startsWith(artifact + ":")).findFirst().isPresent();
  }

  /**
   * For each service defined in the classification it creates an {@link ArtifactClassLoader} wit the name defined in
   * classification.
   *
   * @param parent the parent class loader to be assigned to the new one created here
   * @param childClassLoaderLookupPolicy look policy to be used
   * @param artifactsUrlClassification the url classifications to get service {@link URL}s
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
   *        {@link ClassLoader}
   * @return {@link JarInfo} for the classification
   */
  private JarInfo getTestJarInfo(ArtifactsUrlClassification artifactsUrlClassification) {
    URL testCodeUrl = artifactsUrlClassification.getTestRunnerLibUrls().get(0);
    // sometimes the test-classes URL is the second one.
    if (!toFile(testCodeUrl).getPath().contains("test-classes") && artifactsUrlClassification.getTestRunnerLibUrls().size() > 1) {
      testCodeUrl = artifactsUrlClassification.getTestRunnerLibUrls().get(1);
    }
    Set<String> productionPackages = getProductionCodePackages(testCodeUrl);
    JarInfo testJarInfo = getTestCodePackages(testCodeUrl);

    Set<String> testPackages = sanitizeTestExportedPackages(productionPackages, testJarInfo.getPackages());

    return new JarInfo(testPackages, testJarInfo.getResources());
  }

  /**
   * Sanitizes packages exported on the test class loader.
   * <p/>
   * Test runner exports test packages to the plugins used during the test, to enable the usage of test classes to configure them.
   * A problem is that test code usually contains a mix of unit and integration tests. This causes that packages from the
   * production code are also used to write unit tests for them. The test runner cannot export those production packages as that
   * will cause an error when creating the class loader for the test. To avoid this, every production code package will not be
   * exported on the test.
   * <p/>
   * A similar sanitization is done for packages that are system packages, as child artifacts cannot redefine them.
   *
   * @param productionPackages all packages from the module under test's production code.
   * @param testPackages all packages from the module under test's test code
   * @return sanitized packages to export on the test class loader.
   */
  private Set<String> sanitizeTestExportedPackages(Set<String> productionPackages, Set<String> testPackages) {
    Set<String> sanitizedTestPackages = new HashSet<>(testPackages);
    removePackagesFromTestClassLoader(sanitizedTestPackages, SYSTEM_PACKAGES);
    removePackagesFromTestClassLoader(sanitizedTestPackages, productionPackages);

    return sanitizedTestPackages;
  }

  private JarInfo getTestCodePackages(URL testCodeUrl) {
    List<URL> libraries = newArrayList(testCodeUrl);

    return getLibraryPackages(libraries);
  }

  private Set<String> getProductionCodePackages(URL testCodeUrl) {
    int index = testCodeUrl.toString().lastIndexOf("test-classes");
    try {
      final URI productionCodeUri = new URL(testCodeUrl.toString().substring(0, index) + "classes").toURI();
      if (new File(productionCodeUri).exists()) {
        final JarExplorer jarExplorer = new FileJarExplorer();

        return jarExplorer.explore(productionCodeUri).getPackages();
      } else {
        return emptySet();
      }
    } catch (MalformedURLException | URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private void removePackagesFromTestClassLoader(Set<String> packages, Collection<String> systemPackages) {
    Set<String> packagesToRemove = new HashSet<>();
    systemPackages.stream().forEach(systemPackage -> packages.stream().filter(p -> p.startsWith(systemPackage))
        .forEach(p -> packagesToRemove.add(p)));
    packages.removeAll(packagesToRemove);
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
   * @param artifactsUrlClassification the classifications to get plugins {@link URL}s
   * @return an {@link ArtifactClassLoader} for the container
   */
  protected ArtifactClassLoader createContainerArtifactClassLoader(TestContainerClassLoaderFactory testContainerClassLoaderFactory,
                                                                   ArtifactsUrlClassification artifactsUrlClassification) {
    MuleArtifactClassLoader launcherArtifact = createLauncherArtifactClassLoader();
    final List<MuleModule> muleModules = emptyList();
    ClassLoaderFilter filteredClassLoaderLauncher = new ContainerClassLoaderFilterFactory()
        .create(testContainerClassLoaderFactory.getBootPackages(), muleModules);

    logClassLoaderUrls("CONTAINER", artifactsUrlClassification.getContainerUrls());
    ArtifactClassLoader containerClassLoader = testContainerClassLoaderFactory
        .createContainerClassLoader(new FilteringArtifactClassLoader(launcherArtifact, filteredClassLoaderLauncher, emptyList()));
    return containerClassLoader;
  }

  /**
   * Creates the launcher application class loader to delegate from container class loader.
   *
   * @return an {@link ArtifactClassLoader} for the launcher, parent of container
   */
  protected MuleArtifactClassLoader createLauncherArtifactClassLoader() {
    ClassLoader launcherClassLoader = IsolatedClassLoaderFactory.class.getClassLoader();

    return new MuleArtifactClassLoader("mule", new ArtifactDescriptor("mule"), new URL[0], launcherClassLoader,
                                       new MuleClassLoaderLookupPolicy(emptyMap(), emptySet())) {

      @Override
      public URL findResource(String name) {
        URL url = super.findResource(name);
        if (url == null && getParent() != null) {
          url = getParent().getResource(name);
          // Filter if it is not a resource from the jre
          if (url.getFile().matches(".*?\\/jre\\/lib\\/\\w+\\.jar\\!.*")) {
            return url;
          } else {
            return null;
          }
        }
        return url;
      }
    };
  }

  private ArtifactClassLoaderFilter createArtifactClassLoaderFilter(PluginUrlClassification pluginUrlClassification,
                                                                    Set<String> parentExportedPackages,
                                                                    ClassLoaderLookupPolicy childClassLoaderLookupPolicy) {
    Set<String> sanitizedExportedPackages =
        sanitizePluginExportedPackages(pluginUrlClassification, parentExportedPackages, childClassLoaderLookupPolicy);
    String exportedPackages = sanitizedExportedPackages.stream().collect(joining(", "));

    final String exportedResources = pluginUrlClassification.getExportedResources().stream().collect(joining(", "));
    ArtifactClassLoaderFilter artifactClassLoaderFilter =
        classLoaderFilterFactory.create(exportedPackages, exportedResources);

    if (!pluginUrlClassification.getExportClasses().isEmpty()) {
      artifactClassLoaderFilter =
          new TestArtifactClassLoaderFilter(artifactClassLoaderFilter, pluginUrlClassification.getExportClasses());
    }
    return artifactClassLoaderFilter;
  }

  private Set<String> sanitizePluginExportedPackages(PluginUrlClassification pluginUrlClassification,
                                                     Set<String> parentExportedPackages,
                                                     ClassLoaderLookupPolicy childClassLoaderLookupPolicy) {
    Set<String> exportedPackages = new HashSet<>(pluginUrlClassification.getExportedPackages());

    Set<String> containerProvidedPackages = exportedPackages.stream().filter(p -> {
      LookupStrategy lookupStrategy = childClassLoaderLookupPolicy.getPackageLookupStrategy(p);
      return !(lookupStrategy instanceof ChildFirstLookupStrategy);
    }).collect(toSet());
    if (!containerProvidedPackages.isEmpty()) {
      exportedPackages.removeAll(containerProvidedPackages);
      LOGGER
          .warn("Exported packages from plugin '" + pluginUrlClassification.getName() + "' are provided by parent class loader: "
              + containerProvidedPackages);
    }

    Set<String> appProvidedPackages =
        parentExportedPackages.stream().filter(p -> exportedPackages.contains(p)).collect(toSet());
    if (!appProvidedPackages.isEmpty()) {
      exportedPackages.removeAll(appProvidedPackages);
      LOGGER.warn("Exported packages from plugin '" + pluginUrlClassification.getName() + "' are provided by the artifact owner: "
          + appProvidedPackages);
    }
    return exportedPackages;
  }

  /**
   * Creates an {@link ArtifactClassLoader} for the application.
   *
   * @param parent the parent class loader to be assigned to the new one created here
   * @param childClassLoaderLookupPolicy look policy to be used
   * @param artifactsUrlClassification the url classifications to get plugins urls
   * @param pluginsArtifactClassLoaders the classloaders of the plugins used by the application
   * @return the {@link ArtifactClassLoader} to be used for running the test
   */
  protected ArtifactClassLoader createApplicationArtifactClassLoader(ClassLoader parent,
                                                                     ClassLoaderLookupPolicy childClassLoaderLookupPolicy,
                                                                     ArtifactsUrlClassification artifactsUrlClassification,
                                                                     List<ArtifactClassLoader> pluginsArtifactClassLoaders) {

    List<URL> applicationUrls = new ArrayList<>();
    applicationUrls.addAll(artifactsUrlClassification.getApplicationLibUrls());
    applicationUrls.addAll(artifactsUrlClassification.getApplicationSharedLibUrls());

    logClassLoaderUrls("APP", applicationUrls);
    return new MuleApplicationClassLoader(APP_NAME, new ArtifactDescriptor(APP_NAME), parent,
                                          new DefaultNativeLibraryFinderFactory()
                                              .create(APP_NAME, applicationUrls.toArray(new URL[applicationUrls.size()])),
                                          applicationUrls,
                                          childClassLoaderLookupPolicy, pluginsArtifactClassLoaders);
  }

  /**
   * Logs the {@link List} of {@link URL}s for the classLoaderName
   *
   * @param classLoaderName the name of the {@link ClassLoader} to be logged
   * @param urls {@link List} of {@link URL}s that are going to be used for the {@link ClassLoader}
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
      LOGGER.info(message);
    } else {
      LOGGER.debug(message);
    }
  }

  /**
   * @return true if {@link org.mule.runtime.core.api.config.MuleProperties#MULE_LOG_VERBOSE_CLASSLOADING} is set to true
   */
  private Boolean isVerboseClassLoading() {
    return valueOf(getProperty(MULE_LOG_VERBOSE_CLASSLOADING));
  }

}
