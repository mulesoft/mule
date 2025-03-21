/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner.classloader;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LOG_VERBOSE_CLASSLOADING;
import static org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory.getArtifactPluginId;
import static org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver.pluginDescriptorResolver;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import static org.mule.runtime.module.service.api.artifact.ServiceClassLoaderFactoryProvider.serviceClassLoaderFactory;
import static org.mule.test.runner.RunnerConfiguration.TEST_RUNNER_ARTIFACT_ID;
import static org.mule.test.runner.classloader.container.TestContainerClassLoaderAssembler.create;

import static java.lang.Boolean.valueOf;
import static java.lang.System.getProperty;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.io.FileUtils.toFile;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.container.api.ContainerDependantArtifactClassLoaderFactory;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleContainerClassLoaderWrapper;
import org.mule.runtime.jpms.api.MuleContainerModule;
import org.mule.runtime.module.artifact.activation.api.classloader.ArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.internal.classloader.DefaultArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.internal.classloader.MuleApplicationClassLoader;
import org.mule.runtime.module.artifact.activation.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFilterFactory;
import org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderFilterFactory;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.DefaultArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.api.classloader.FilteringArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.classloader.exception.ArtifactClassloaderCreationException;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration.ClassLoaderConfigurationBuilder;
import org.mule.runtime.module.artifact.internal.util.FileJarExplorer;
import org.mule.runtime.module.artifact.internal.util.JarExplorer;
import org.mule.runtime.module.artifact.internal.util.JarInfo;
import org.mule.runtime.module.service.api.artifact.ServiceClassLoaderFactory;
import org.mule.runtime.module.service.api.artifact.ServiceClassLoaderFactoryProvider;
import org.mule.runtime.module.service.api.artifact.ServiceDescriptor;
import org.mule.test.runner.api.ArtifactClassLoaderHolder;
import org.mule.test.runner.api.ArtifactsUrlClassification;
import org.mule.test.runner.api.PluginUrlClassification;
import org.mule.test.runner.api.ServiceUrlClassification;
import org.mule.test.runner.classloader.container.TestContainerClassLoaderAssembler;

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
import java.util.TreeSet;

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

  private final ClassLoaderFilterFactory classLoaderFilterFactory = new ArtifactClassLoaderFilterFactory();
  private final PluginLookPolicyFactory pluginLookupPolicyGenerator = new PluginLookPolicyFactory();

  /**
   * Creates a {@link ArtifactClassLoaderHolder} containing the container, plugins and application {@link ArtifactClassLoader}s
   *
   * @param extraBootPackages          {@link List} of {@link String}s of extra boot packages to be appended to the container
   *                                   {@link ClassLoader}
   * @param extraPrivilegedArtifacts   {@link List} of {@link String}s of extra privileged artifacts. Each value needs to have the
   *                                   form groupId:versionId.
   * @param artifactsUrlClassification the {@link ArtifactsUrlClassification} that defines the different {@link URL}s for each
   *                                   {@link ClassLoader}
   * @return a {@link ArtifactClassLoaderHolder} that would be used to run the test
   */
  public ArtifactClassLoaderHolder createArtifactClassLoader(List<String> extraBootPackages,
                                                             Set<String> extraPrivilegedArtifacts,
                                                             ArtifactsUrlClassification artifactsUrlClassification) {
    Map<String, LookupStrategy> appExportedLookupStrategies = new HashMap<>();
    JarInfo testJarInfo = getAppSharedPackages(artifactsUrlClassification.getApplicationSharedLibUrls());
    testJarInfo.getPackages().stream().forEach(p -> appExportedLookupStrategies.put(p, PARENT_FIRST));

    MuleContainerClassLoaderWrapper containerClassLoaderWrapper;
    ClassLoaderLookupPolicy childClassLoaderLookupPolicy;
    RegionClassLoader regionClassLoader;
    final List<ArtifactClassLoader> filteredPluginsArtifactClassLoaders = new ArrayList<>();
    final List<ArtifactClassLoader> pluginsArtifactClassLoaders = new ArrayList<>();
    final List<ArtifactClassLoaderFilter> pluginArtifactClassLoaderFilters = new ArrayList<>();

    try {
      final TestContainerClassLoaderAssembler testContainerClassLoaderAssembler =
          create(extraBootPackages, extraPrivilegedArtifacts,
                 artifactsUrlClassification.getContainerMuleApisOptUrls(),
                 artifactsUrlClassification.getContainerMuleApisUrls(),
                 artifactsUrlClassification.getContainerOptUrls(),
                 artifactsUrlClassification.getContainerMuleUrls());

      final Map<String, LookupStrategy> pluginsLookupStrategies = new HashMap<>();

      final List<PluginUrlClassification> pluginUrlClassifications = artifactsUrlClassification.getPluginUrlClassifications();
      for (PluginUrlClassification pluginUrlClassification : pluginUrlClassifications) {
        pluginUrlClassification.getExportedPackages().forEach(p -> pluginsLookupStrategies.put(p, PARENT_FIRST));
      }

      containerClassLoaderWrapper = testContainerClassLoaderAssembler.createContainerClassLoader();
      ModuleRepository moduleRepository = testContainerClassLoaderAssembler.getModuleRepository();

      ServiceClassLoaderFactoryProvider.setWithinModularizedContainer(true);
      ContainerDependantArtifactClassLoaderFactory<ServiceDescriptor> serviceClassLoaderFactory = serviceClassLoaderFactory();
      serviceClassLoaderFactory.setParentLayerFrom(containerClassLoaderWrapper.getContainerClassLoader().getClassLoader()
          .loadClass(ServiceClassLoaderFactory.class.getName()));
      List<ArtifactClassLoader> serviceArtifactClassLoaders =
          createServiceClassLoaders(serviceClassLoaderFactory, containerClassLoaderWrapper, artifactsUrlClassification);

      childClassLoaderLookupPolicy = containerClassLoaderWrapper.getContainerClassLoaderLookupPolicy();

      regionClassLoader = new TestRegionClassLoader(containerClassLoaderWrapper.getContainerClassLoader().getClassLoader(),
                                                    childClassLoaderLookupPolicy);

      final ClassLoaderLookupPolicy appLookupPolicy = childClassLoaderLookupPolicy.extend(pluginsLookupStrategies);
      MuleApplicationClassLoader appClassLoader =
          createApplicationArtifactClassLoader(regionClassLoader, appLookupPolicy, artifactsUrlClassification);
      regionClassLoader.addClassLoader(appClassLoader,
                                       new DefaultArtifactClassLoaderFilter(testJarInfo.getPackages(),
                                                                            testJarInfo.getResources()));

      final ArtifactClassLoaderResolver artifactClassLoaderResolver =
          new DefaultArtifactClassLoaderResolver(containerClassLoaderWrapper.getContainerClassLoader(), moduleRepository, null);
      Map<String, BundleDescriptor> descriptors = new HashMap<>();

      if (!pluginUrlClassifications.isEmpty()) {
        for (PluginUrlClassification pluginUrlClassification : pluginUrlClassifications) {
          logClassLoaderUrls("PLUGIN (" + pluginUrlClassification.getName() + ")", pluginUrlClassification.getUrls());

          descriptors.put(pluginUrlClassification.getName(), pluginUrlClassification.getPluginBundleDescriptor());

          ArtifactClassLoader pluginCL =
              createPluginClassLoader(artifactClassLoaderResolver, descriptors, pluginUrlClassification, appClassLoader);

          pluginsArtifactClassLoaders.add(pluginCL);

          ArtifactClassLoaderFilter filter =
              createArtifactClassLoaderFilter(pluginUrlClassification, testJarInfo.getPackages(), childClassLoaderLookupPolicy);

          pluginArtifactClassLoaderFilters.add(filter);
          filteredPluginsArtifactClassLoaders.add(new FilteringArtifactClassLoader(pluginCL, filter, emptyList()));
        }

        createTestRunnerPlugin(artifactsUrlClassification, appExportedLookupStrategies, childClassLoaderLookupPolicy,
                               regionClassLoader, filteredPluginsArtifactClassLoaders, pluginsArtifactClassLoaders,
                               pluginArtifactClassLoaderFilters, moduleRepository,
                               containerClassLoaderWrapper,
                               testJarInfo.getPackages());
      }

      for (int i = 0; i < filteredPluginsArtifactClassLoaders.size(); i++) {
        final ArtifactClassLoaderFilter classLoaderFilter = pluginArtifactClassLoaderFilters.get(i);
        regionClassLoader.addClassLoader(filteredPluginsArtifactClassLoaders.get(i), classLoaderFilter);
      }

      return new ArtifactClassLoaderHolder(containerClassLoaderWrapper.getContainerClassLoader(),
                                           serviceArtifactClassLoaders,
                                           pluginsArtifactClassLoaders,
                                           appClassLoader);
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  private ArtifactClassLoader createPluginClassLoader(final ArtifactClassLoaderResolver artifactClassLoaderResolver,
                                                      Map<String, BundleDescriptor> descriptors,
                                                      PluginUrlClassification pluginUrlClassification,
                                                      MuleApplicationClassLoader appClassLoader) {
    final JarInfo pluginLibrariesPackages = getLibraryPackages(pluginUrlClassification.getUrls());

    final ArtifactPluginDescriptor pluginDescriptor = new ArtifactPluginDescriptor(pluginUrlClassification.getName());
    pluginDescriptor.setClassLoaderConfiguration(new ClassLoaderConfigurationBuilder()
        .containing(pluginUrlClassification.getUrls())
        .exportingPackages(pluginUrlClassification.getExportedPackages())
        .exportingResources(pluginUrlClassification.getExportedResources())
        .exportingPrivilegedPackages(pluginUrlClassification.getPrivilegedExportedPackages(),
                                     pluginUrlClassification.getPrivilegedArtifacts())
        .withLocalPackages(pluginLibrariesPackages.getPackages())
        .withLocalResources(pluginLibrariesPackages.getResources())
        .dependingOn(pluginUrlClassification.getPluginDependencies()
            .stream()
            .map(descriptors::get)
            .map(d -> new BundleDependency.Builder()
                .setDescriptor(d)
                .build())
            .collect(toSet()))
        .build());
    pluginDescriptor.setBundleDescriptor(pluginUrlClassification.getPluginBundleDescriptor());

    return artifactClassLoaderResolver
        .createMulePluginClassLoader(appClassLoader, pluginDescriptor, pluginDescriptorResolver());
  }

  private JarInfo getAppSharedPackages(List<URL> pluginSharedLibUrls) {
    List<URL> libraries = newArrayList();
    libraries.addAll(pluginSharedLibUrls);

    return getLibraryPackages(libraries);
  }

  private JarInfo getLibraryPackages(List<URL> libraries) {
    Set<String> packages = new TreeSet<>();
    Set<String> resources = new TreeSet<>();
    final JarExplorer jarExplorer = new FileJarExplorer(false);

    for (URL library : libraries) {
      try {
        JarInfo jarInfo = jarExplorer.explore(library.toURI());
        packages.addAll(jarInfo.getPackages());
        resources.addAll(jarInfo.getResources());
      } catch (URISyntaxException e) {
        throw new MuleRuntimeException(e);
      }
    }

    return new JarInfo(packages, resources, emptyList());
  }

  private void createTestRunnerPlugin(ArtifactsUrlClassification artifactsUrlClassification,
                                      Map<String, LookupStrategy> appExportedLookupStrategies,
                                      ClassLoaderLookupPolicy childClassLoaderLookupPolicy, RegionClassLoader regionClassLoader,
                                      List<ArtifactClassLoader> filteredPluginsArtifactClassLoaders,
                                      List<ArtifactClassLoader> pluginsArtifactClassLoaders,
                                      List<ArtifactClassLoaderFilter> pluginArtifactClassLoaderFilters,
                                      ModuleRepository moduleRepository,
                                      MuleContainerClassLoaderWrapper containerClassLoaderWrapper,
                                      Set<String> parentExportedPackages) {

    JarInfo testRunnerJarInfo = getTestRunnerJarInfo(artifactsUrlClassification);

    String testRunnerArtifactId = getArtifactPluginId(regionClassLoader.getArtifactId(), "test-runner");

    List<String> pluginDependencies =
        artifactsUrlClassification.getPluginUrlClassifications().stream()
            .map(PluginUrlClassification::getName)
            .collect(toList());

    PluginUrlClassification testRunnerPluginClassification =
        new PluginUrlClassification(TEST_RUNNER_ARTIFACT_ID + ":", artifactsUrlClassification.getTestRunnerLibUrls(), emptyList(),
                                    new BundleDescriptor.Builder()
                                        .setGroupId("org.mule.test")
                                        .setArtifactId(testRunnerArtifactId)
                                        .setVersion("0.0.1")
                                        .build(),
                                    pluginDependencies, testRunnerJarInfo.getPackages(), testRunnerJarInfo.getResources(),
                                    emptySet(), emptySet());

    ClassLoaderLookupPolicy pluginLookupPolicy =
        extendLookupPolicyForPrivilegedAccess(childClassLoaderLookupPolicy, moduleRepository,
                                              containerClassLoaderWrapper,
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

    return new JarInfo(exportedPackages, exportedResources, emptyList());
  }

  private ClassLoaderLookupPolicy extendLookupPolicyForPrivilegedAccess(ClassLoaderLookupPolicy childClassLoaderLookupPolicy,
                                                                        ModuleRepository moduleRepository,
                                                                        MuleContainerClassLoaderWrapper containerClassLoaderWrapper,
                                                                        PluginUrlClassification pluginUrlClassification) {
    LookupStrategy containerOnlyLookupStrategy = containerClassLoaderWrapper
        .getContainerClassLoaderLookupPolicy()
        .getClassLookupStrategy(ModuleRepository.class.getName());

    Map<String, LookupStrategy> privilegedLookupStrategies = new HashMap<>();
    for (MuleContainerModule module : moduleRepository.getModules()) {
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

  private boolean hasPrivilegedApiAccess(PluginUrlClassification pluginUrlClassification, MuleContainerModule module) {
    return module.getPrivilegedArtifacts().stream()
        .filter(artifact -> pluginUrlClassification.getName().startsWith(artifact + ":")).findFirst().isPresent();
  }

  /**
   * For each service defined in the classification it creates an {@link ArtifactClassLoader} wit the name defined in
   * classification.
   *
   * @param containerClassLoaderWrapper the container class loader to be used as parent.
   * @param artifactsUrlClassification  the url classifications to get service {@link URL}s
   * @return a list of {@link ArtifactClassLoader} for service class loaders
   */
  protected List<ArtifactClassLoader> createServiceClassLoaders(ContainerDependantArtifactClassLoaderFactory<ServiceDescriptor> serviceClassLoaderFactory,
                                                                MuleContainerClassLoaderWrapper containerClassLoaderWrapper,
                                                                ArtifactsUrlClassification artifactsUrlClassification)
      throws ArtifactClassloaderCreationException {
    List<ArtifactClassLoader> servicesArtifactClassLoaders = newArrayList();
    for (ServiceUrlClassification serviceUrlClassification : artifactsUrlClassification.getServiceUrlClassifications()) {
      logClassLoaderUrls("SERVICE (" + serviceUrlClassification.getArtifactId() + ")", serviceUrlClassification.getUrls());

      ServiceDescriptor descriptor = serviceUrlClassification.getDercriptor();
      ArtifactClassLoader artifactClassLoader = serviceClassLoaderFactory.create(getServiceArtifactId(descriptor),
                                                                                 descriptor,
                                                                                 containerClassLoaderWrapper);
      servicesArtifactClassLoaders.add(artifactClassLoader);
    }
    return servicesArtifactClassLoaders;
  }

  private String getServiceArtifactId(ServiceDescriptor serviceDescriptor) {
    return "service/" + serviceDescriptor.getName();
  }

  /**
   * Creates the {@link JarInfo} for the {@link ArtifactsUrlClassification}.
   *
   * @param artifactsUrlClassification the {@link ArtifactsUrlClassification} that defines the different {@link URL}s for each
   *                                   {@link ClassLoader}
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

    return new JarInfo(testPackages, testJarInfo.getResources(), emptyList());
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
   * @param testPackages       all packages from the module under test's test code.
   * @return sanitized packages to export on the test class loader.
   */
  private Set<String> sanitizeTestExportedPackages(Set<String> productionPackages, Set<String> testPackages) {
    Set<String> sanitizedTestPackages = new TreeSet<>(testPackages);
    removePackagesFromTestClassLoader(sanitizedTestPackages, TestContainerClassLoaderAssembler.getSystemPackages());
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
   * @param parent                       the parent class loader to be assigned to the new one created here
   * @param childClassLoaderLookupPolicy look policy to be used
   * @param artifactsUrlClassification   the url classifications to get plugins urls
   * @return the {@link ArtifactClassLoader} to be used for running the test
   */
  protected MuleApplicationClassLoader createApplicationArtifactClassLoader(ClassLoader parent,
                                                                            ClassLoaderLookupPolicy childClassLoaderLookupPolicy,
                                                                            ArtifactsUrlClassification artifactsUrlClassification) {

    List<URL> applicationUrls = new ArrayList<>();
    applicationUrls.addAll(artifactsUrlClassification.getApplicationLibUrls());
    applicationUrls.addAll(artifactsUrlClassification.getApplicationSharedLibUrls());

    logClassLoaderUrls("APP", applicationUrls);
    ApplicationDescriptor applicationDescriptor = new ApplicationDescriptor(APP_NAME);
    return new MuleApplicationClassLoader(APP_NAME, applicationDescriptor, parent,
                                          new DefaultNativeLibraryFinderFactory()
                                              .create(APP_NAME,
                                                      applicationDescriptor.getLoadedNativeLibrariesFolderName(),
                                                      applicationUrls.toArray(new URL[applicationUrls.size()])),
                                          applicationUrls,
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
   * Logs the message with info severity if {@link MULE_LOG_VERBOSE_CLASSLOADING} is set or trace severity
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
   * @return true if {@link MULE_LOG_VERBOSE_CLASSLOADING} is set to true
   */
  private Boolean isVerboseClassLoading() {
    return valueOf(getProperty(MULE_LOG_VERBOSE_CLASSLOADING));
  }

}
