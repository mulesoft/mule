/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4;

import static org.mule.runtime.core.config.bootstrap.ClassLoaderRegistryBootstrapDiscoverer.BOOTSTRAP_PROPERTIES;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.config.bootstrap.BootstrapService;
import org.mule.runtime.core.config.bootstrap.BootstrapServiceDiscoverer;
import org.mule.runtime.core.config.bootstrap.PropertiesBootstrapService;
import org.mule.runtime.core.config.bootstrap.PropertiesBootstrapServiceDiscoverer;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.util.PropertiesUtils;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
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

public class TestBootstrapServiceDiscovererContextBuilder extends AbstractConfigurationBuilder {

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
  public TestBootstrapServiceDiscovererContextBuilder(ClassLoader containerClassLoader, ClassLoader executionClassLoader,
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

      BootstrapService pluginBootstrapService = getArtifactBootstrapService(pluginClassLoader);
      if (pluginBootstrapService != null) {
        bootstrapServices.add(pluginBootstrapService);
      }
    }

    BootstrapService appBootstrapService = getArtifactBootstrapService(executionClassLoader);
    if (appBootstrapService != null) {
      bootstrapServices.add(appBootstrapService);
    }

    muleContext.setBootstrapServiceDiscoverer(() -> bootstrapServices);
  }

  private BootstrapService getArtifactBootstrapService(Object pluginClassLoader)
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException {
    // Uses reflection to access the class loader as classes were loaded from different class loaders so they cannot be casted
    ClassLoader classLoader =
        (ClassLoader) pluginClassLoader.getClass().getMethod("getClassLoader").invoke(pluginClassLoader);
    final URL localResource =
        (URL) classLoader.getClass().getMethod("findResource", String.class).invoke(classLoader, BOOTSTRAP_PROPERTIES);

    BootstrapService pluginBootstrapService = null;
    if (localResource != null) {
      final Properties properties = PropertiesUtils.loadProperties(localResource);
      pluginBootstrapService = new PropertiesBootstrapService(classLoader, properties);
    }

    return pluginBootstrapService;
  }
}
