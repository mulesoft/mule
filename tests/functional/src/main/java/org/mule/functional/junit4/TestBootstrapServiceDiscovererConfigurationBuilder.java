/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4;

import static org.mule.runtime.core.internal.config.bootstrap.ClassLoaderRegistryBootstrapDiscoverer.BOOTSTRAP_PROPERTIES;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.bootstrap.BootstrapService;
import org.mule.runtime.core.api.config.bootstrap.BootstrapServiceDiscoverer;
import org.mule.runtime.core.api.config.bootstrap.PropertiesBootstrapService;
import org.mule.runtime.core.api.config.bootstrap.PropertiesBootstrapServiceDiscoverer;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.util.PropertiesUtils;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Configures a {@link BootstrapServiceDiscoverer} on a test's {@link MuleContext}
 * <p/>
 * Configuration done by this builder will use a {@link BootstrapServiceDiscoverer} to find {@link BootstrapService} provided by
 * the container, will create a {@link BootstrapServiceDiscoverer} for each plugin deployed in the artifact and finally another
 * {@link BootstrapServiceDiscoverer} for the test itself. All the discovered services will then used to configure the context.
 */

public class TestBootstrapServiceDiscovererConfigurationBuilder extends AbstractConfigurationBuilder {

  private final ClassLoader containerClassLoader;
  private final ClassLoader executionClassLoader;
  private final List<ArtifactClassLoader> pluginClassLoaders;

  /**
   * Creates a context builder for a test.
   *
   * @param containerClassLoader class loader corresponding to the container where the test is running. Non null.
   * @param executionClassLoader class loader corresponding to the isolates test. Non null
   * @param pluginClassLoaders class loaders corresponding to plugins deployed in the test (without any filtering). Non null.
   */
  public TestBootstrapServiceDiscovererConfigurationBuilder(ClassLoader containerClassLoader, ClassLoader executionClassLoader,
                                                            List<ArtifactClassLoader> pluginClassLoaders) {
    checkArgument(containerClassLoader != null, "ContainerClassLoader cannot be null");
    checkArgument(executionClassLoader != null, "ExecutionClassLoader cannot be null");
    checkArgument(pluginClassLoaders != null, "PluginClassLoaders cannot be null");
    this.containerClassLoader = containerClassLoader;
    this.executionClassLoader = executionClassLoader;
    this.pluginClassLoaders = pluginClassLoaders;
  }

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    final PropertiesBootstrapServiceDiscoverer propertiesBootstrapServiceDiscoverer =
        new PropertiesBootstrapServiceDiscoverer(containerClassLoader);

    List<BootstrapService> bootstrapServices = new LinkedList<>();
    bootstrapServices.addAll(propertiesBootstrapServiceDiscoverer.discover());

    // Uses Object instead of ArtifactClassLoader because that class was originally loaded from a different class loader
    for (Object pluginClassLoader : pluginClassLoaders) {
      List<BootstrapService> pluginBootstrapServices = getArtifactBootstrapService(pluginClassLoader);
      bootstrapServices.addAll(pluginBootstrapServices);
    }

    List<BootstrapService> appBootstrapServices = getArtifactBootstrapService(executionClassLoader);
    bootstrapServices.addAll(appBootstrapServices);

    muleContext.setBootstrapServiceDiscoverer(() -> bootstrapServices);
  }

  private List<BootstrapService> getArtifactBootstrapService(Object artifactClassLoader)
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException {
    // Uses reflection to access the class loader as classes were loaded from different class loaders so they cannot be casted
    ClassLoader classLoader =
        (ClassLoader) artifactClassLoader.getClass().getMethod("getClassLoader").invoke(artifactClassLoader);
    final Enumeration<URL> resources =
        (Enumeration<URL>) classLoader.getClass().getMethod("findResources", String.class).invoke(classLoader,
                                                                                                  BOOTSTRAP_PROPERTIES);

    final List<BootstrapService> bootstrapServices = new ArrayList<>();
    if (resources.hasMoreElements()) {
      while (resources.hasMoreElements()) {
        final URL localResource = resources.nextElement();
        final Properties properties = PropertiesUtils.loadProperties(localResource);
        final PropertiesBootstrapService propertiesBootstrapService = new PropertiesBootstrapService(classLoader, properties);
        bootstrapServices.add(propertiesBootstrapService);
      }
    }

    return bootstrapServices;
  }
}
