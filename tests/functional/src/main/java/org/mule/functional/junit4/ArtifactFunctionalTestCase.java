/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4;

import static org.hamcrest.object.IsCompatibleType.typeCompatibleWith;
import static org.junit.Assert.assertThat;
import static org.mule.test.runner.utils.AnnotationUtils.getAnnotationAttributeFrom;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.config.spring.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.service.DefaultServiceDiscoverer;
import org.mule.runtime.module.service.MuleServiceManager;
import org.mule.runtime.module.service.ReflectionServiceProviderResolutionHelper;
import org.mule.runtime.module.service.ReflectionServiceResolver;
import org.mule.test.runner.ArtifactClassLoaderRunner;
import org.mule.test.runner.ContainerClassLoaderAware;
import org.mule.test.runner.PluginClassLoadersAware;
import org.mule.test.runner.RunnerDelegateTo;
import org.mule.test.runner.ServiceClassLoadersAware;
import org.mule.test.runner.api.ClassPathClassifier;
import org.mule.test.runner.api.IsolatedClassLoaderExtensionsManagerConfigurationBuilder;
import org.mule.test.runner.api.IsolatedServiceProviderDiscoverer;

import java.util.List;

import org.junit.runner.RunWith;

/**
 * Base class for running {@link FunctionalTestCase} with class loader isolation using {@link ArtifactClassLoaderRunner}, a JUnit
 * {@link org.junit.runner.Runner}. {@link ArtifactClassLoaderRunner} classifies the classpath provided by
 * IDE/surfire-maven-plugin generates an equivalent {@link ClassLoader} hierarchy as standalone mode.
 * <p/>
 * The classification is based on the maven dependencies declared by the pom maven artifact that the test belongs to. In order to
 * classify the {@link java.net.URL}s from the classphat it will use a dependency tree that holds also duplicates relationship for
 * the dependency graph.
 * <p/>
 * The classification for {@link ClassLoader}s would be based on dependencies scope, provided will go to the container, compile to
 * plugin and test to the application. For more information about the classification process see {@link ClassPathClassifier}.
 * <p/>
 * For plugins it will scan the plugin set of {@link java.net.URL}s to search for classes annotated with
 * {@link org.mule.runtime.extension.api.annotation.Extension}, if a class is annotated it will generate the metadata for the
 * extension in runtime and it will also register it to the {@link org.mule.runtime.extension.api.ExtensionManager}. Non extension
 * plugins will set its filter based on {@code mule-module.properties} file.
 * <p/>
 * By default this test runs internally with a {@link org.junit.runners.BlockJUnit4ClassRunner} runner. On those cases where the
 * test has to be run with another runner the {@link RunnerDelegateTo} should be used to define it.
 * <p/>
 * {@link PluginClassLoadersAware} will define that this class also needs to get access to plugin {@link ArtifactClassLoader} in
 * order to load extension classes (they are not exposed to the application) for registering them to the
 * {@link org.mule.runtime.extension.api.ExtensionManager}.
 * <p/>
 * Due to the cost of reading the classpath, scanning the dependencies and classes to generate the {@link ClassLoader} is high,
 * this runner will hold an static reference to the {@link ClassLoader} created for the first test and will use the same during
 * the whole execution of the tests that are marked with the {@link RunWith} {@link ArtifactClassLoaderRunner}, so be aware that
 * static initializations or things related to this could be an issue and tests should be aware of this.
 *
 * @since 4.0
 */
@RunWith(ArtifactClassLoaderRunner.class)
public abstract class ArtifactFunctionalTestCase extends FunctionalTestCase {

  private static List<ArtifactClassLoader> pluginClassLoaders;
  private static List<ArtifactClassLoader> serviceClassLoaders;
  private static ClassLoader containerClassLoader;

  private static MuleServiceManager serviceRepository;

  /**
   * @return thread context class loader has to be the application {@link ClassLoader} created by the runner.
   */
  @Override
  protected ClassLoader getExecutionClassLoader() {
    return Thread.currentThread().getContextClassLoader();
  }

  @PluginClassLoadersAware
  private static final void setPluginClassLoaders(List<ArtifactClassLoader> artifactClassLoaders) {
    if (artifactClassLoaders == null) {
      throw new IllegalArgumentException("A null value cannot be set as the plugins class loaders");
    }

    if (pluginClassLoaders != null) {
      throw new IllegalStateException("Plugin class loaders were already set, it cannot be set again");
    }
    pluginClassLoaders = artifactClassLoaders;
  }

  @ServiceClassLoadersAware
  private static final void setServiceClassLoaders(List<ArtifactClassLoader> artifactClassLoaders) {
    if (artifactClassLoaders == null) {
      throw new IllegalArgumentException("A null value cannot be set as the services class loaders");
    }

    if (serviceClassLoaders != null) {
      throw new IllegalStateException("Service class loaders were already set, it cannot be set again");
    }
    serviceClassLoaders = artifactClassLoaders;
  }

  @ContainerClassLoaderAware
  private static final void setContainerClassLoader(ClassLoader containerClassLoader) {
    if (containerClassLoader == null) {
      throw new IllegalArgumentException("A null value cannot be set as the container classLoader");
    }

    if (ArtifactFunctionalTestCase.containerClassLoader != null) {
      throw new IllegalStateException("Plugin class loaders were already set, it cannot be set again");
    }

    ArtifactFunctionalTestCase.containerClassLoader = containerClassLoader;
  }

  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    ConfigurationBuilder builder = super.getBuilder();
    assertThat(builder.getClass(), typeCompatibleWith(SpringXmlConfigurationBuilder.class));
    configureSpringXmlConfigurationBuilder((SpringXmlConfigurationBuilder) builder);
    return builder;
  }

  protected void configureSpringXmlConfigurationBuilder(SpringXmlConfigurationBuilder builder) {
    if (serviceRepository == null) {
      serviceRepository =
          new MuleServiceManager(new DefaultServiceDiscoverer(new IsolatedServiceProviderDiscoverer(serviceClassLoaders),
                                                              new ReflectionServiceResolver(new ReflectionServiceProviderResolutionHelper())));
      try {
        serviceRepository.start();
      } catch (MuleException e) {
        throw new IllegalStateException("Couldn't start service manager", e);
      }
    }

    builder.addServiceConfigurator(new TestServicesMuleContextConfigurator(serviceRepository));
  }

  /**
   * Adds a {@link ConfigurationBuilder} that sets the {@link org.mule.runtime.extension.api.ExtensionManager} into the
   * {@link #muleContext}. This {@link ConfigurationBuilder} is set as the first element of the {@code builders} {@link List}
   *
   * @param builders the list of {@link ConfigurationBuilder}s that will be used to initialise the {@link #muleContext}
   */
  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    Class<?> runner = getAnnotationAttributeFrom(this.getClass(), RunWith.class, "value");
    if (runner == null || !runner.equals(ArtifactClassLoaderRunner.class)) {
      throw new IllegalStateException(this.getClass().getName() + " extends " + ArtifactFunctionalTestCase.class.getName()
          + " so it should be annotated to only run with: " + ArtifactClassLoaderRunner.class + ". See " + RunnerDelegateTo.class
          + " for defining a delegate runner to be used.");
    }

    if (pluginClassLoaders != null && !pluginClassLoaders.isEmpty()) {
      builders.add(0, new IsolatedClassLoaderExtensionsManagerConfigurationBuilder(pluginClassLoaders));
    }

    builders.add(0, new TestBootstrapServiceDiscovererConfigurationBuilder(containerClassLoader, getExecutionClassLoader(),
                                                                           pluginClassLoaders));
  }

}
