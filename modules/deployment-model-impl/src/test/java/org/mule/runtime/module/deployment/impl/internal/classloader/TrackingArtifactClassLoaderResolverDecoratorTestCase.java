/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.classloader;

import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.CLASSLOADER_GENERATION;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.module.artifact.activation.api.classloader.ArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderManager;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Issue("W-11069995")
@Feature(CLASSLOADING_ISOLATION)
@Story(CLASSLOADER_GENERATION)
public class TrackingArtifactClassLoaderResolverDecoratorTestCase extends AbstractMuleTestCase {

  private ArtifactClassLoaderManager artifactClassLoaderManager;
  private TrackingArtifactClassLoaderResolverDecorator decorator;

  @Before
  public void setUp() throws Exception {
    artifactClassLoaderManager = mock(ArtifactClassLoaderManager.class);
    ArtifactClassLoaderResolver artifactClassLoaderResolver = mock(ArtifactClassLoaderResolver.class);
    decorator = new TrackingArtifactClassLoaderResolverDecorator(artifactClassLoaderManager, artifactClassLoaderResolver);
    Function<String, MuleDeployableArtifactClassLoader> classLoaderFactory =
        (artifactName) -> new MuleDeployableArtifactClassLoader(artifactName, new ArtifactDescriptor(artifactName), new URL[0],
                                                                mock(RegionClassLoader.class),
                                                                mock(ClassLoaderLookupPolicy.class));
    Function<String, MuleDeployableArtifactClassLoader> classLoaderWithPluginsFactory = (artifactName) -> {
      MuleDeployableArtifactClassLoader classLoader = classLoaderFactory.apply(artifactName);
      List<ArtifactClassLoader> pluginClassLoaders = new ArrayList<>();
      pluginClassLoaders.add(classLoaderFactory.apply(artifactName + " Plugin Class loader 1"));
      pluginClassLoaders.add(classLoaderFactory.apply(artifactName + " Plugin Class loader 2"));

      when(classLoader.getArtifactPluginClassLoaders()).thenReturn(pluginClassLoaders);
      return classLoader;
    };

    MuleDeployableArtifactClassLoader domainClassLoader1 = classLoaderWithPluginsFactory.apply("Domain Class loader 1");
    MuleDeployableArtifactClassLoader domainClassLoader2 = classLoaderWithPluginsFactory.apply("Domain Class loader 2");
    MuleDeployableArtifactClassLoader applicationClassLoader1 = classLoaderWithPluginsFactory.apply("Application Class loader 1");
    MuleDeployableArtifactClassLoader applicationClassLoader2 = classLoaderWithPluginsFactory.apply("Application Class loader 2");
    MuleDeployableArtifactClassLoader applicationClassLoader3 = classLoaderWithPluginsFactory.apply("Application Class loader 3");
    MuleDeployableArtifactClassLoader applicationClassLoader4 = classLoaderWithPluginsFactory.apply("Application Class loader 4");
    MuleDeployableArtifactClassLoader pluginClassLoader1 = classLoaderWithPluginsFactory.apply("Plugin Class loader 1");
    MuleDeployableArtifactClassLoader pluginClassLoader2 = classLoaderWithPluginsFactory.apply("Plugin Class loader 2");

    when(artifactClassLoaderResolver.createDomainClassLoader(any())).thenReturn(domainClassLoader1);
    when(artifactClassLoaderResolver.createDomainClassLoader(any(), any())).thenReturn(domainClassLoader2);
    when(artifactClassLoaderResolver.createApplicationClassLoader(any())).thenReturn(applicationClassLoader1);
    when(artifactClassLoaderResolver.createApplicationClassLoader(any(), any(PluginClassLoaderResolver.class)))
        .thenReturn(applicationClassLoader2);
    when(artifactClassLoaderResolver.createApplicationClassLoader(any(), any(Supplier.class)))
        .thenReturn(applicationClassLoader3);
    when(artifactClassLoaderResolver.createApplicationClassLoader(any(), any(), any(PluginClassLoaderResolver.class)))
        .thenReturn(applicationClassLoader4);
    when(artifactClassLoaderResolver.createMulePluginClassLoader(any(), any(), any())).thenReturn(pluginClassLoader1);
    when(artifactClassLoaderResolver.createMulePluginClassLoader(any(), any(), any(), any())).thenReturn(pluginClassLoader2);
  }

  @Test
  public void registersClassLoaders() {
    verifyClassLoaderRegistered(decorator.createDomainClassLoader(mock(DomainDescriptor.class)));
    verifyClassLoaderRegistered(decorator.createDomainClassLoader(mock(DomainDescriptor.class),
                                                                  mock(PluginClassLoaderResolver.class)));
    verifyClassLoaderRegistered(decorator.createApplicationClassLoader(mock(ApplicationDescriptor.class)));
    verifyClassLoaderRegistered(decorator.createApplicationClassLoader(mock(ApplicationDescriptor.class),
                                                                       mock(PluginClassLoaderResolver.class)));
    verifyClassLoaderRegistered(decorator.createApplicationClassLoader(mock(ApplicationDescriptor.class), mock(Supplier.class)));
    verifyClassLoaderRegistered(decorator.createApplicationClassLoader(mock(ApplicationDescriptor.class), mock(Supplier.class),
                                                                       mock(PluginClassLoaderResolver.class)));
    verifyClassLoaderRegistered(decorator.createMulePluginClassLoader(mock(MuleDeployableArtifactClassLoader.class),
                                                                      mock(ArtifactPluginDescriptor.class),
                                                                      mock(PluginDescriptorResolver.class)));
    verifyClassLoaderRegistered(decorator
        .createMulePluginClassLoader(mock(MuleDeployableArtifactClassLoader.class), mock(ArtifactPluginDescriptor.class),
                                     mock(PluginDescriptorResolver.class), mock(PluginClassLoaderResolver.class)));
  }

  private void verifyClassLoaderRegistered(MuleArtifactClassLoader classLoader) {
    verify(artifactClassLoaderManager).register(classLoader);
  }

  private void verifyClassLoaderRegistered(MuleDeployableArtifactClassLoader classLoader) {
    verify(artifactClassLoaderManager).register(classLoader);
    classLoader.getArtifactPluginClassLoaders().forEach(pcl -> verify(artifactClassLoaderManager).register(pcl));
  }

  @Test
  public void disposesClassLoaders() {
    verifyClassLoaderDisposed(decorator.createDomainClassLoader(mock(DomainDescriptor.class)));
    verifyClassLoaderDisposed(decorator.createDomainClassLoader(mock(DomainDescriptor.class),
                                                                mock(PluginClassLoaderResolver.class)));
    verifyClassLoaderDisposed(decorator.createApplicationClassLoader(mock(ApplicationDescriptor.class)));
    verifyClassLoaderDisposed(decorator.createApplicationClassLoader(mock(ApplicationDescriptor.class),
                                                                     mock(PluginClassLoaderResolver.class)));
    verifyClassLoaderDisposed(decorator.createApplicationClassLoader(mock(ApplicationDescriptor.class), mock(Supplier.class)));
    verifyClassLoaderDisposed(decorator.createApplicationClassLoader(mock(ApplicationDescriptor.class), mock(Supplier.class),
                                                                     mock(PluginClassLoaderResolver.class)));
    verifyClassLoaderDisposed(decorator.createMulePluginClassLoader(mock(MuleDeployableArtifactClassLoader.class),
                                                                    mock(ArtifactPluginDescriptor.class),
                                                                    mock(PluginDescriptorResolver.class)));
    verifyClassLoaderDisposed(decorator
        .createMulePluginClassLoader(mock(MuleDeployableArtifactClassLoader.class), mock(ArtifactPluginDescriptor.class),
                                     mock(PluginDescriptorResolver.class), mock(PluginClassLoaderResolver.class)));
  }

  private void verifyClassLoaderDisposed(MuleArtifactClassLoader classLoader) {
    classLoader.dispose();
    verify(artifactClassLoaderManager).unregister(classLoader.getArtifactId());
  }

  private void verifyClassLoaderDisposed(MuleDeployableArtifactClassLoader classLoader) {
    classLoader.dispose();
    verify(artifactClassLoaderManager).unregister(classLoader.getArtifactId());
    classLoader.getArtifactPluginClassLoaders().forEach(pcl -> {
      pcl.dispose();
      verify(artifactClassLoaderManager).unregister(pcl.getArtifactId());
    });
  }

}
