/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4;

import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CLASSLOADER_REPOSITORY;
import static org.mule.test.runner.utils.AnnotationUtils.getAnnotationAttributeFrom;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.SimpleConfigurationBuilder;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.net.MuleArtifactUrlStreamHandler;
import org.mule.runtime.module.artifact.api.classloader.net.MuleUrlStreamHandlerFactory;
import org.mule.runtime.module.artifact.api.serializer.ArtifactObjectSerializer;
import org.mule.runtime.module.service.api.discoverer.ServiceDiscoverer;
import org.mule.runtime.module.service.api.manager.ServiceManager;
import org.mule.test.runner.ApplicationClassLoaderAware;
import org.mule.test.runner.ArtifactClassLoaderRunner;
import org.mule.test.runner.ContainerClassLoaderAware;
import org.mule.test.runner.PluginClassLoadersAware;
import org.mule.test.runner.RunnerDelegateTo;
import org.mule.test.runner.ServiceClassLoadersAware;
import org.mule.test.runner.api.ClassPathClassifier;
import org.mule.test.runner.api.IsolatedClassLoaderExtensionsManagerConfigurationBuilder;
import org.mule.test.runner.api.IsolatedServiceProviderDiscoverer;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

/**
 * Base class for running {@link FunctionalTestCase} with class loader isolation using {@link ArtifactClassLoaderRunner}, a JUnit
 * {@link org.junit.runner.Runner}. {@link ArtifactClassLoaderRunner} classifies the classpath provided by
 * IDE/surfire-maven-plugin generates an equivalent {@link ClassLoader} hierarchy as standalone mode.
 * <p/>
 * The classification is based on the maven dependencies declared by the pom maven artifact that the test belongs to. In order to
 * classify the {@link java.net.URL}s from the classpath it will use a dependency tree that holds also duplicates relationship for
 * the dependency graph.
 * <p/>
 * The classification for {@link ClassLoader}s would be based on dependencies scope, provided will go to the container, compile to
 * plugin and test to the application. For more information about the classification process see {@link ClassPathClassifier}.
 * <p/>
 * For plugins it will scan the plugin set of {@link java.net.URL}s to search for classes annotated with
 * {@link org.mule.runtime.extension.api.annotation.Extension}, if a class is annotated it will generate the metadata for the
 * extension in runtime and it will also register it to the {@link org.mule.runtime.core.api.extension.ExtensionManager}. Non
 * extension plugins will set its filter based on {@code mule-module.properties} file.
 * <p/>
 * By default this test runs internally with a {@link org.junit.runners.BlockJUnit4ClassRunner} runner. On those cases where the
 * test has to be run with another runner the {@link RunnerDelegateTo} should be used to define it.
 * <p/>
 * {@link PluginClassLoadersAware} will define that this class also needs to get access to plugin {@link ArtifactClassLoader} in
 * order to load extension classes (they are not exposed to the application) for registering them to the
 * {@link org.mule.runtime.core.api.extension.ExtensionManager}.
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

  /**
   * As part of providing support for handling different artifacts without unzipping them, the factory for URL must be registered
   * and then the current protocol for mule artifacts {@link MuleArtifactUrlStreamHandler#PROTOCOL}.
   */
  static {
    // register the custom UrlStreamHandlerFactory.
    MuleUrlStreamHandlerFactory.installUrlStreamHandlerFactory();
    MuleArtifactUrlStreamHandler.register();
  }

  public static final String SPRING_CONFIG_FILES_PROPERTIES = "spring.config.files";
  public static final String MULE_SPRING_CONFIG_FILE = "mule-spring-config.xml";

  private static List<ArtifactClassLoader> pluginClassLoaders;
  private static List<ArtifactClassLoader> serviceClassLoaders;
  private static ClassLoader containerClassLoader;
  private static ClassLoader applicationClassLoader;
  private static ServiceManager serviceRepository;
  private static ClassLoaderRepository classLoaderRepository;
  private static IsolatedClassLoaderExtensionsManagerConfigurationBuilder extensionsManagerConfigurationBuilder;

  private static TestServicesMuleContextConfigurator serviceConfigurator;

  @BeforeClass
  public static void configureClassLoaderRepository() {
    classLoaderRepository = new TestClassLoaderRepository();
  }

  @Override
  @After
  public final void clearRequestContext() {
    // Nothing to do.
  }

  @Override
  protected ObjectSerializer getObjectSerializer() {
    return new ArtifactObjectSerializer(classLoaderRepository);
  }

  /**
   * @return thread context class loader has to be the application {@link ClassLoader} created by the runner.
   */
  @Override
  protected ClassLoader getExecutionClassLoader() {
    return applicationClassLoader;
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
    if (!pluginClassLoaders.isEmpty()) {
      extensionsManagerConfigurationBuilder =
          new IsolatedClassLoaderExtensionsManagerConfigurationBuilder(pluginClassLoaders);
      extensionsManagerConfigurationBuilder.loadExtensionModels();
    }
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
    createServiceManager();
  }

  @ContainerClassLoaderAware
  private static final void setContainerClassLoader(ClassLoader containerClassLoader) {
    if (containerClassLoader == null) {
      throw new IllegalArgumentException("A null value cannot be set as the container classLoader");
    }

    if (ArtifactFunctionalTestCase.containerClassLoader != null) {
      throw new IllegalStateException("Container classloader was already set, it cannot be set again");
    }

    ArtifactFunctionalTestCase.containerClassLoader = containerClassLoader;
  }

  @ApplicationClassLoaderAware
  private static final void setApplicationClassLoader(ClassLoader applicationClassLoader) {
    if (applicationClassLoader == null) {
      throw new IllegalArgumentException("A null value cannot be set as the application classLoader");
    }

    if (ArtifactFunctionalTestCase.applicationClassLoader != null) {
      throw new IllegalStateException("Application classloader was already set, it cannot be set again");
    }

    ArtifactFunctionalTestCase.applicationClassLoader = applicationClassLoader;
  }

  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    ConfigurationBuilder builder = super.getBuilder();
    assertThat(builder.getClass().getName(), is("org.mule.runtime.config.internal.SpringXmlConfigurationBuilder"));
    configureSpringXmlConfigurationBuilder(builder);
    return builder;
  }

  /**
   * Returns an instance of a given service if available
   *
   * @param serviceClass class of service to look for. Non null.
   * @param <T> service class
   * @return an instance of the provided service type if it was declared as a dependency on the test, null otherwise.
   */
  protected <T extends Service> T getService(Class<T> serviceClass) {
    Optional<Service> service =
        serviceRepository.getServices().stream().filter(s -> serviceClass.isAssignableFrom(s.getClass())).findFirst();

    return service.isPresent() ? (T) service.get() : null;
  }

  protected void configureSpringXmlConfigurationBuilder(ConfigurationBuilder builder) {
    builder.addServiceConfigurator(serviceConfigurator);
  }

  private static void createServiceManager() {
    serviceRepository =
        ServiceManager.create(ServiceDiscoverer.create(new IsolatedServiceProviderDiscoverer(serviceClassLoaders)));
    try {
      serviceRepository.start();
    } catch (MuleException e) {
      throw new IllegalStateException("Couldn't start service manager", e);
    }
    serviceConfigurator = new TestServicesMuleContextConfigurator(serviceRepository);
  }

  /**
   * Adds a {@link ConfigurationBuilder} that sets the {@link org.mule.runtime.core.api.extension.ExtensionManager} into the
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

    if (extensionsManagerConfigurationBuilder != null) {
      builders.add(0, extensionsManagerConfigurationBuilder);
    }

    builders.add(0, new TestBootstrapServiceDiscovererConfigurationBuilder(containerClassLoader, getExecutionClassLoader(),
                                                                           pluginClassLoaders));

    builders.add(0, new SimpleConfigurationBuilder(singletonMap(OBJECT_CLASSLOADER_REPOSITORY, classLoaderRepository)));
  }

  /**
   * Defines a {@link ClassLoaderRepository} with all the class loaders configured in the {@link ArtifactFunctionalTestCase}
   * class.
   */
  protected static class TestClassLoaderRepository implements ClassLoaderRepository {

    private Map<String, ClassLoader> classLoaders = new HashMap<>();

    public TestClassLoaderRepository() {
      registerClassLoader(Thread.currentThread().getContextClassLoader());
      for (Object classLoader : serviceClassLoaders) {
        registerClassLoader(classLoader);
      }
      for (Object classLoader : pluginClassLoaders) {
        registerClassLoader(classLoader);
      }
    }

    private void registerClassLoader(Object classLoader) {
      if (isArtifactClassLoader(classLoader)) {
        try {
          Method getArtifactIdMethod = classLoader.getClass().getMethod("getArtifactId");
          String artifactId = (String) getArtifactIdMethod.invoke(classLoader);
          classLoaders.put(artifactId, (ClassLoader) classLoader);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }

    }

    private boolean isArtifactClassLoader(Object classLoader) {
      Class clazz = classLoader.getClass();
      while (clazz.getSuperclass() != null) {
        for (Class interfaceClass : clazz.getInterfaces()) {
          if (interfaceClass.getName().equals(ArtifactClassLoader.class.getName())) {
            return true;
          }
        }
        clazz = clazz.getSuperclass();
      }

      return false;
    }

    @Override
    public Optional<ClassLoader> find(String classLoaderId) {
      return ofNullable(classLoaders.get(classLoaderId));
    }

    @Override
    public Optional<String> getId(ClassLoader classLoader) {
      for (String key : classLoaders.keySet()) {
        if (classLoaders.get(key) == classLoader) {
          return of(key);
        }
      }

      return empty();
    }
  }
}
