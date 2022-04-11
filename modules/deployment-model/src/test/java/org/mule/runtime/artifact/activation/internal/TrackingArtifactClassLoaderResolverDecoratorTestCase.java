/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.artifact.activation.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.CLASSLOADER_GENERATION;
import static org.powermock.api.mockito.PowerMockito.when;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;
import org.mule.runtime.module.artifact.activation.api.classloader.ArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginClassLoaderResolver;
import org.mule.runtime.module.artifact.api.classloader.*;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.net.URL;
import java.util.function.Function;
import java.util.function.Supplier;

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
    ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
    Function<String, MuleDeployableArtifactClassLoader> classLoaderFactory =
        (artifactName) -> new MuleDeployableArtifactClassLoader(artifactName, new ArtifactDescriptor(artifactName), new URL[0],
                                                                getClass().getClassLoader(), lookupPolicy);

    when(lookupPolicy.getClassLookupStrategy(any())).thenReturn(PARENT_FIRST);
    when(artifactClassLoaderResolver.createDomainClassLoader(any())).thenReturn(classLoaderFactory.apply("Class loader 1"));
    when(artifactClassLoaderResolver.createDomainClassLoader(any(), any()))
        .thenReturn(classLoaderFactory.apply("Class loader 2"));
    when(artifactClassLoaderResolver.createApplicationClassLoader(any(), any()))
        .thenReturn(classLoaderFactory.apply("Class loader 3"));
    when(artifactClassLoaderResolver.createApplicationClassLoader(any(), any(), any()))
        .thenReturn(classLoaderFactory.apply("Class loader 4"));
    when(artifactClassLoaderResolver.createMulePluginClassLoader(any(), any(), any()))
        .thenReturn(classLoaderFactory.apply("Class loader 5"));
    when(artifactClassLoaderResolver.createMulePluginClassLoader(any(), any(), any(), any()))
        .thenReturn(classLoaderFactory.apply("Class loader 6"));
  }

  @Test
  public void registersClassLoaders() {
    ArtifactClassLoader domainClassLoader1 = decorator.createDomainClassLoader(mock(DomainDescriptor.class));
    ArtifactClassLoader domainClassLoader2 =
        decorator.createDomainClassLoader(mock(DomainDescriptor.class), mock(PluginClassLoaderResolver.class));
    ArtifactClassLoader applicationClassLoader1 =
        decorator.createApplicationClassLoader(mock(ApplicationDescriptor.class), mock(Supplier.class));
    ArtifactClassLoader applicationClassLoader2 =
        decorator.createApplicationClassLoader(mock(ApplicationDescriptor.class), mock(Supplier.class),
                                               mock(PluginClassLoaderResolver.class));
    ArtifactClassLoader pluginClassLoader1 =
        decorator.createMulePluginClassLoader(mock(MuleDeployableArtifactClassLoader.class), mock(ArtifactPluginDescriptor.class),
                                              mock(Function.class));
    ArtifactClassLoader pluginClassLoader2 =
        decorator.createMulePluginClassLoader(mock(MuleDeployableArtifactClassLoader.class), mock(ArtifactPluginDescriptor.class),
                                              mock(Function.class), mock(PluginClassLoaderResolver.class));

    verify(artifactClassLoaderManager).register(domainClassLoader1);
    verify(artifactClassLoaderManager).register(domainClassLoader2);
    verify(artifactClassLoaderManager).register(applicationClassLoader1);
    verify(artifactClassLoaderManager).register(applicationClassLoader2);
    verify(artifactClassLoaderManager).register(pluginClassLoader1);
    verify(artifactClassLoaderManager).register(pluginClassLoader2);
  }

  @Test
  public void disposesClassLoaders() {
    ArtifactClassLoader domainClassLoader1 = decorator.createDomainClassLoader(mock(DomainDescriptor.class));
    ArtifactClassLoader domainClassLoader2 =
        decorator.createDomainClassLoader(mock(DomainDescriptor.class), mock(PluginClassLoaderResolver.class));
    ArtifactClassLoader applicationClassLoader1 =
        decorator.createApplicationClassLoader(mock(ApplicationDescriptor.class), mock(Supplier.class));
    ArtifactClassLoader applicationClassLoader2 =
        decorator.createApplicationClassLoader(mock(ApplicationDescriptor.class), mock(Supplier.class),
                                               mock(PluginClassLoaderResolver.class));
    ArtifactClassLoader pluginClassLoader1 =
        decorator.createMulePluginClassLoader(mock(MuleDeployableArtifactClassLoader.class), mock(ArtifactPluginDescriptor.class),
                                              mock(Function.class));
    ArtifactClassLoader pluginClassLoader2 =
        decorator.createMulePluginClassLoader(mock(MuleDeployableArtifactClassLoader.class), mock(ArtifactPluginDescriptor.class),
                                              mock(Function.class), mock(PluginClassLoaderResolver.class));

    domainClassLoader1.dispose();
    domainClassLoader2.dispose();
    applicationClassLoader1.dispose();
    applicationClassLoader2.dispose();
    pluginClassLoader1.dispose();
    pluginClassLoader2.dispose();

    verify(artifactClassLoaderManager).unregister(domainClassLoader1.getArtifactId());
    verify(artifactClassLoaderManager).unregister(domainClassLoader2.getArtifactId());
    verify(artifactClassLoaderManager).unregister(applicationClassLoader1.getArtifactId());
    verify(artifactClassLoaderManager).unregister(applicationClassLoader2.getArtifactId());
    verify(artifactClassLoaderManager).unregister(pluginClassLoader1.getArtifactId());
    verify(artifactClassLoaderManager).unregister(pluginClassLoader2.getArtifactId());
  }

}
