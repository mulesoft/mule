/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.of;
import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.maven.client.api.model.MavenConfiguration.newMavenConfigurationBuilder;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.test.runner.RunnerConfiguration.readConfiguration;
import static org.mule.test.runner.utils.AnnotationUtils.getAnnotationAttributeFrom;
import static org.mule.test.runner.utils.RunnerModuleUtils.EXCLUDED_ARTIFACTS;
import static org.mule.test.runner.utils.RunnerModuleUtils.EXCLUDED_PROPERTIES_FILE;
import static org.mule.test.runner.utils.RunnerModuleUtils.EXTRA_BOOT_PACKAGES;
import static org.mule.test.runner.utils.RunnerModuleUtils.getExcludedProperties;

import org.mule.maven.client.api.MavenClientProvider;
import org.mule.maven.client.api.SettingsSupplierFactory;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.test.runner.api.AetherClassPathClassifier;
import org.mule.test.runner.api.ArtifactClassLoaderHolder;
import org.mule.test.runner.api.ArtifactClassificationTypeResolver;
import org.mule.test.runner.api.ArtifactIsolatedClassLoaderBuilder;
import org.mule.test.runner.api.ClassPathClassifier;
import org.mule.test.runner.api.ClassPathUrlProvider;
import org.mule.test.runner.api.DependencyResolver;
import org.mule.test.runner.api.WorkspaceLocationResolver;
import org.mule.test.runner.classification.DefaultWorkspaceReader;
import org.mule.test.runner.maven.AutoDiscoverWorkspaceLocationResolver;

import org.junit.internal.builders.AnnotatedBuilder;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A {@link org.junit.runner.Runner} that mimics the class loading model used in a Mule Standalone distribution. In order to
 * detect early issues related to isolation when building plugins these runner allows you to run your functional test cases using
 * an isolated class loader.
 * <p/>
 * {@link org.mule.functional.junit4.ArtifactFunctionalTestCase} should be extended in order to use this runner, it has already
 * annotated the runner and also has the logic to register {@link org.mule.runtime.extension.api.annotation.Extension} to a
 * {@link org.mule.runtime.core.api.MuleContext}.
 * <p/>
 * See {@link RunnerDelegateTo} for those scenarios where another JUnit runner needs to be used but still the test has to be
 * executed within an isolated class loading model. {@link ArtifactClassLoaderRunnerConfig} allows to define the plugins in order
 * to create the class loaders for them, for each one a plugin class loader would be created. {@link PluginClassLoadersAware}
 * allows the test to be injected with the list of {@link ClassLoader}s that were created for each plugin, mostly used in
 * {@link org.mule.functional.junit4.ArtifactFunctionalTestCase} in order to enable plugins into a
 * {@link org.mule.runtime.core.api.MuleContext}.
 * <p/>
 * The class loading model is built by doing a classification of the class path {@link URL}s loaded by IDEs or Maven.
 * {@link ClassPathClassifier} defines the strategy of classification to be used in order to define the
 * {@link ArtifactClassLoaderHolder}, classification would define three levels of {@link URL}s that would be used for creating a
 * container {@link ArtifactClassLoader}, list of plugins {@link ArtifactClassLoader} and an application
 * {@link ArtifactClassLoader}.
 * <p/>
 * The classification bases its logic by resolving Maven dependency graphs using Eclipse Aether. See
 * {@link AetherClassPathClassifier} for more details about this. In order to allow the classification to resolve Maven artifact
 * from the local Maven repository, if the default location is not used {@code $USER_HOME/.m2/repository}, the following system
 * property has to be to the local Maven repository location when running a test from IDE:
 *
 * <pre>
 * System.getProperty("localRepository")
 * </pre>
 * <p/>
 * Only one instance of the {@link ClassLoader} is created and used to run all the tests that are marked to run with this
 * {@link Runner} due to creating the {@link ClassLoader} requires time and has impact when running tests.
 * <p/>
 * A best practice is to a base abstract class for your module tests that extends
 * {@link org.mule.functional.junit4.ArtifactFunctionalTestCase} and defines if needed anything related to the configuration with
 * this annotation that will be applied to all the tests that are being executed for the same VM.
 *
 * @since 4.0
 */
public class ArtifactClassLoaderRunner extends Runner implements Filterable {


  private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactClassLoaderRunner.class);

  private static ArtifactClassLoaderHolder artifactClassLoaderHolder;
  private static RunnerConfiguration runnerConfiguration;

  private static Exception errorCreatingClassLoaderTestRunner;
  private static boolean staticFieldsInjected = false;
  private static Throwable errorWhileSettingClassLoaders;

  private final Runner delegate;

  /**
   * Creates a Runner to run {@code klass}
   *
   * @param clazz
   * @param builder
   * @throws Throwable if there was an error while initializing the runner.
   */
  public ArtifactClassLoaderRunner(Class<?> clazz, RunnerBuilder builder) throws Throwable {
    if (errorCreatingClassLoaderTestRunner != null) {
      throw errorCreatingClassLoaderTestRunner;
    }

    if (artifactClassLoaderHolder == null) {
      try {
        runnerConfiguration = readConfiguration(clazz);
        artifactClassLoaderHolder = createClassLoaderTestRunner(clazz, runnerConfiguration);
      } catch (Exception e) {
        errorCreatingClassLoaderTestRunner = e;
        throw e;
      }
    } else {
      checkConfiguration(clazz);
    }

    final Class<?> isolatedTestClass = getTestClass(clazz);

    final Class<? extends Annotation> runnerDelegateToClass = (Class<? extends Annotation>) artifactClassLoaderHolder
        .loadClassWithATestRunnerClassLoader(RunnerDelegateTo.class.getName());

    delegate = new AnnotatedBuilder(builder)
        .buildRunner(getAnnotationAttributeFrom(isolatedTestClass, runnerDelegateToClass, "value"), isolatedTestClass);

    if (staticFieldsInjected && errorWhileSettingClassLoaders != null) {
      throw Throwables.propagate(errorWhileSettingClassLoaders);
    }
    withContextClassLoader(artifactClassLoaderHolder.getTestRunnerPluginClassLoader().getClassLoader(), () -> {
      try {
        if (!staticFieldsInjected) {

          injectPluginsClassLoaders(artifactClassLoaderHolder, isolatedTestClass);
          injectServicesClassLoaders(artifactClassLoaderHolder, isolatedTestClass);
          injectContainerClassLoader(artifactClassLoaderHolder, isolatedTestClass);
          injectApplicationClassLoader(artifactClassLoaderHolder, isolatedTestClass);
        }
      } catch (Throwable t) {
        errorWhileSettingClassLoaders = t;
        throw Throwables.propagate(t);
      } finally {
        staticFieldsInjected = true;
      }
    });
  }

  private void checkConfiguration(Class<?> klass) {
    RunnerConfiguration testRunnerConfiguration = readConfiguration(klass);
    if (!runnerConfiguration.equals(testRunnerConfiguration)) {
      throw new IllegalArgumentException("Invalid configuration defined for test: " + klass
          + " . Is not supported to have multiple configurations of" +
          " the runner because class loaders are created only once for all the tests in the module. Current configuration loaded was: "
          + runnerConfiguration + " but configuration obtained from test class was: " + testRunnerConfiguration);
    }
  }

  /**
   * Creates the {@link ArtifactClassLoaderHolder} with the isolated class loaders.
   *
   * @param klass the test class being executed
   * @param runnerConfiguration {@link RunnerConfiguration} based on annotated test class.
   * @return creates a {@link ArtifactClassLoaderHolder} that would be used to run the test. This way the test will be isolated
   *         and it will behave similar as an application running in a Mule standalone container.
   */
  private static synchronized ArtifactClassLoaderHolder createClassLoaderTestRunner(Class<?> klass,
                                                                                    RunnerConfiguration runnerConfiguration) {
    final File targetTestClassesFolder = new File(klass.getProtectionDomain().getCodeSource().getLocation().getPath());

    ArtifactIsolatedClassLoaderBuilder builder = new ArtifactIsolatedClassLoaderBuilder();
    final File rootArtifactClassesFolder = new File(targetTestClassesFolder.getParentFile(), "classes");
    builder.setRootArtifactClassesFolder(rootArtifactClassesFolder);
    builder.setPluginResourcesFolder(targetTestClassesFolder.getParentFile());

    builder.setProvidedExclusions(runnerConfiguration.getProvidedExclusions());
    builder.setTestExclusions(runnerConfiguration.getTestExclusions());
    builder.setTestInclusions(runnerConfiguration.getTestInclusions());

    builder.setExportPluginClasses(runnerConfiguration.getExportPluginClasses());

    builder.setApplicationSharedLibCoordinates(runnerConfiguration.getSharedApplicationRuntimeLibs());
    builder.setApplicationLibCoordinates(runnerConfiguration.getApplicationRuntimeLibs());
    builder.setTestRunnerExportedLibCoordinates(runnerConfiguration.getTestRunnerExportedRuntimeLibs());
    builder.setExtensionMetadataGeneration(true);

    Properties excludedProperties;
    try {
      excludedProperties = getExcludedProperties();
    } catch (IOException e) {
      throw new RuntimeException("Error while reading excluded properties", e);
    }
    Set<String> excludedArtifactsList = getExcludedArtifacts(excludedProperties);
    builder.setExcludedArtifacts(excludedArtifactsList);
    builder.setExtraBootPackages(getExtraBootPackages(excludedProperties));
    builder.setExtraPrivilegedArtifacts(runnerConfiguration.getExtraPrivilegedArtifacts());


    final ClassPathUrlProvider classPathUrlProvider = new ClassPathUrlProvider();
    List<URL> classPath = classPathUrlProvider.getURLs();

    builder.setClassPathUrlProvider(classPathUrlProvider);

    WorkspaceLocationResolver workspaceLocationResolver = new AutoDiscoverWorkspaceLocationResolver(classPath,
                                                                                                    rootArtifactClassesFolder);

    final MavenClientProvider mavenClientProvider = discoverProvider(ArtifactClassLoaderRunner.class.getClassLoader());
    final Supplier<File> localMavenRepository =
        mavenClientProvider.getLocalRepositorySuppliers().environmentMavenRepositorySupplier();

    final SettingsSupplierFactory settingsSupplierFactory = mavenClientProvider.getSettingsSupplierFactory();

    final Optional<File> globalSettings = settingsSupplierFactory.environmentGlobalSettingsSupplier();
    final Optional<File> userSettings = settingsSupplierFactory.environmentUserSettingsSupplier();

    final MavenConfiguration.MavenConfigurationBuilder mavenConfigurationBuilder = newMavenConfigurationBuilder()
        .forcePolicyUpdateNever(true)
        .localMavenRepositoryLocation(localMavenRepository.get());

    if (globalSettings.isPresent()) {
      mavenConfigurationBuilder.globalSettingsLocation(globalSettings.get());
    } else {
      LOGGER
          .info("Maven global settings couldn't be found, M2_HOME environment variable has to be set in order to use global settings (if needed)");
    }

    if (userSettings.isPresent()) {
      mavenConfigurationBuilder.userSettingsLocation(userSettings.get());
    } else {
      LOGGER.info("Maven user settings couldn't be found, this could cause a wrong resolution for dependencies");
    }

    final MavenConfiguration mavenConfiguration = mavenConfigurationBuilder.build();
    LOGGER.info("Using MavenConfiguration: {}", mavenConfiguration);

    final DependencyResolver dependencyResolver =
        new DependencyResolver(mavenConfiguration, of(new DefaultWorkspaceReader(classPath, workspaceLocationResolver)));
    builder.setClassPathClassifier(new AetherClassPathClassifier(dependencyResolver,
                                                                 new ArtifactClassificationTypeResolver(
                                                                                                        dependencyResolver)));

    return builder.build();
  }

  /**
   * Gets the {@link List} of {@link String}s of Maven artifacts to be excluded due to they are going to be added later as boot
   * package.
   *
   * @param excludedProperties {@link Properties }that has the list of extra boot packages definitions
   * @return a {@link List} of {@link String}s with the excluded artifacts
   */
  private static Set<String> getExcludedArtifacts(Properties excludedProperties) {
    String excludedArtifacts = excludedProperties.getProperty(EXCLUDED_ARTIFACTS);
    Set<String> excludedArtifactsList = new HashSet<>();
    if (excludedArtifacts != null) {
      for (String exclusion : excludedArtifacts.split(",")) {
        excludedArtifactsList.add(exclusion);
      }
    }
    return excludedArtifactsList;
  }

  /**
   * Gets the {@link List} of {@link String}s of packages to be added to the container {@link ClassLoader} in addition to the ones
   * already pre-defined by the mule container.
   *
   * @param excludedProperties {@link Properties }that has the list of extra boot packages definitions
   * @return a {@link List} of {@link String}s with the extra boot packages to be appended
   */
  private static List<String> getExtraBootPackages(final Properties excludedProperties) {
    Set<String> packages = Sets.newHashSet();

    String excludedExtraBootPackages = excludedProperties.getProperty(EXTRA_BOOT_PACKAGES);
    if (excludedExtraBootPackages != null) {
      for (String extraBootPackage : excludedExtraBootPackages.split(",")) {
        packages.add(extraBootPackage);
      }
    } else {
      LOGGER.warn(EXCLUDED_PROPERTIES_FILE
          + " found but there is no list of extra boot packages defined to be added to container, this could be the reason why the test may fail later due to JUnit classes are not found");
    }
    return newArrayList(packages);
  }

  /**
   * Invokes the method to inject the plugin class loaders as the test is annotated with {@link PluginClassLoadersAware}.
   *
   * @param artifactClassLoaderHolder the result {@link ArtifactClassLoader}s defined for container, plugins and application
   * @param isolatedTestClass the test {@link Class} loaded with the isolated {@link ClassLoader}
   * @throws IllegalStateException if the test doesn't have an annotated method to inject plugin class loaders or if it has more
   *         than one method annotated.
   * @throws Throwable if an error ocurrs while setting the list of {@link ArtifactClassLoader}s for plugins.
   */
  private static void injectPluginsClassLoaders(ArtifactClassLoaderHolder artifactClassLoaderHolder, Class<?> isolatedTestClass)
      throws Throwable {
    final Class<PluginClassLoadersAware> pluginClassLoadersAwareClass = PluginClassLoadersAware.class;
    final String expectedParamType = "List<" + ArtifactClassLoader.class + ">";
    final FrameworkMethod method =
        getAnnotatedMethod(artifactClassLoaderHolder, isolatedTestClass, pluginClassLoadersAwareClass, expectedParamType);

    final Object valueToInject = artifactClassLoaderHolder.getPluginsClassLoaders();
    doFieldInjection(pluginClassLoadersAwareClass, method, valueToInject, expectedParamType);
  }

  private static void injectServicesClassLoaders(ArtifactClassLoaderHolder artifactClassLoaderHolder, Class<?> isolatedTestClass)
      throws Throwable {
    final Class<ServiceClassLoadersAware> serviceClassLoadersAwareClass = ServiceClassLoadersAware.class;
    final String expectedParamType = "List<" + ArtifactClassLoader.class + ">";
    final FrameworkMethod method =
        getAnnotatedMethod(artifactClassLoaderHolder, isolatedTestClass, serviceClassLoadersAwareClass, expectedParamType);

    final Object valueToInject = artifactClassLoaderHolder.getServicesClassLoaders();
    doFieldInjection(serviceClassLoadersAwareClass, method, valueToInject, expectedParamType);
  }

  private static void injectContainerClassLoader(ArtifactClassLoaderHolder artifactClassLoaderHolder, Class<?> isolatedTestClass)
      throws Throwable {
    final Class<ContainerClassLoaderAware> containerClassLoaderAwareClass = ContainerClassLoaderAware.class;
    final String expectedParamType = ArtifactClassLoader.class.getName();
    final FrameworkMethod method = getAnnotatedMethod(artifactClassLoaderHolder, isolatedTestClass,
                                                      containerClassLoaderAwareClass, expectedParamType);
    final Object containerClassLoader = artifactClassLoaderHolder.getContainerClassLoader();
    final Field artifactClassLoaderField =
        containerClassLoader.getClass().getSuperclass().getDeclaredField("artifactClassLoader");
    artifactClassLoaderField.setAccessible(true);
    final Object valueToInject = artifactClassLoaderField.get(containerClassLoader);
    doFieldInjection(containerClassLoaderAwareClass, method, valueToInject, expectedParamType);
  }

  private static void injectApplicationClassLoader(ArtifactClassLoaderHolder artifactClassLoaderHolder,
                                                   Class<?> isolatedTestClass)
      throws Throwable {
    final Class<ApplicationClassLoaderAware> applicationClassLoaderAwareClass = ApplicationClassLoaderAware.class;
    final String expectedParamType = ArtifactClassLoader.class.getName();
    final FrameworkMethod method = getAnnotatedMethod(artifactClassLoaderHolder, isolatedTestClass,
                                                      applicationClassLoaderAwareClass, expectedParamType);
    final Object applicationClassLoader = artifactClassLoaderHolder.getApplicationClassLoader();
    doFieldInjection(applicationClassLoaderAwareClass, method, applicationClassLoader, expectedParamType);
  }

  private static void doFieldInjection(Class<? extends Annotation> containerClassLoaderAwareClass, FrameworkMethod method,
                                       Object value, String expectedParamType)
      throws Throwable {
    method.getMethod().setAccessible(true);
    try {
      method.invokeExplosively(null, value);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("Method marked with annotation " + containerClassLoaderAwareClass.getName()
          + " should receive a parameter of type " + expectedParamType);
    } finally {
      method.getMethod().setAccessible(false);
    }
  }

  private static FrameworkMethod getAnnotatedMethod(ArtifactClassLoaderHolder artifactClassLoaderHolder,
                                                    Class<?> isolatedTestClass,
                                                    Class<? extends Annotation> annotationClass, String expectedParamType)
      throws ClassNotFoundException {
    TestClass testClass = new TestClass(isolatedTestClass);
    Class<? extends Annotation> artifactContextAwareAnn = (Class<? extends Annotation>) artifactClassLoaderHolder
        .loadClassWithATestRunnerClassLoader(annotationClass.getName());
    List<FrameworkMethod> contextAwareMethods = testClass.getAnnotatedMethods(artifactContextAwareAnn);
    if (contextAwareMethods.size() != 1) {
      throw new IllegalStateException("Isolation tests need to have one method marked with annotation "
          + annotationClass.getName());
    }
    final FrameworkMethod method = contextAwareMethods.get(0);
    if (!method.isStatic() || method.isPublic()) {
      throw new IllegalStateException("Method marked with annotation " + annotationClass.getName()
          + " should be private static and receive a parameter of type " + expectedParamType);
    }
    return method;
  }

  private Class<?> getTestClass(Class<?> clazz) throws InitializationError {
    try {
      return artifactClassLoaderHolder.loadClassWithATestRunnerClassLoader(clazz.getName());
    } catch (Exception e) {
      throw new InitializationError(e);
    }
  }

  /**
   * @return delegates to the internal runner to get the description needed by JUnit.
   */
  @Override
  public Description getDescription() {
    return delegate.getDescription();
  }

  /**
   * When the test is about to be executed the ThreadContextClassLoader is changed to use the application class loader that was
   * created so the execution of the test will be done using an isolated class loader that mimics the standalone container.
   *
   * @param notifier the {@link RunNotifier} from JUnit that will be notified about the results of the test methods invoked.
   */
  @Override
  public void run(RunNotifier notifier) {
    withContextClassLoader(artifactClassLoaderHolder.getTestRunnerPluginClassLoader().getClassLoader(),
                           () -> delegate.run(notifier));
  }

  /**
   * Delegates to the inner runner to filter.
   *
   * @param filter the {@link Filter} from JUnit to select a single test.
   * @throws NoTestsRemainException
   */
  @Override
  public void filter(Filter filter) throws NoTestsRemainException {
    if (delegate instanceof Filterable) {
      ((Filterable) delegate).filter(filter);
    }
  }
}
