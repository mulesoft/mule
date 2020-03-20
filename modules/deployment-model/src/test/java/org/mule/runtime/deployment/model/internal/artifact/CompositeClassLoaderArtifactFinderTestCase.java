/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.artifact;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.deployment.model.internal.artifact.CompositeClassLoaderArtifactFinder.findClassLoader;

import org.junit.Test;
import org.mule.runtime.core.internal.util.CompositeClassLoader;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.size.SmallTest;

@SmallTest
public class CompositeClassLoaderArtifactFinderTestCase {

  private static final ArtifactDescriptor pluginDescriptor = new ArtifactPluginDescriptor("my-plugin");
  private static final ArtifactDescriptor appDescriptor = new ApplicationDescriptor("my-app");
  private static final ArtifactDescriptor domainDescriptor = new DomainDescriptor("my-domain");

  @Test
  public void appClassLoader() {
    ClassLoader appClassLoader = mockArtifactClassLoader(appDescriptor);
    CompositeClassLoader compositeClassLoader =
        new CompositeClassLoader(mockArtifactClassLoader(pluginDescriptor), appClassLoader);

    assertThat(findClassLoader(compositeClassLoader), equalTo(appClassLoader));
  }

  @Test
  public void domainClassLoader() {
    ClassLoader domainClassLoader = mockArtifactClassLoader(domainDescriptor);
    CompositeClassLoader compositeClassLoader =
        new CompositeClassLoader(mockArtifactClassLoader(pluginDescriptor), domainClassLoader);

    assertThat(findClassLoader(compositeClassLoader), equalTo(domainClassLoader));
  }

  @Test
  public void firstDelegateIfNoArtifactClassLoaderFound() {
    ClassLoader pluginClassLoader = mockArtifactClassLoader(pluginDescriptor);
    CompositeClassLoader compositeClassLoader = new CompositeClassLoader(pluginClassLoader);

    assertThat(findClassLoader(compositeClassLoader), equalTo(pluginClassLoader));
  }

  private static ClassLoader mockArtifactClassLoader(ArtifactDescriptor descriptor) {
    MuleDeployableArtifactClassLoader classLoader = mock(MuleDeployableArtifactClassLoader.class);
    when(classLoader.getArtifactDescriptor()).thenReturn(descriptor);
    return classLoader;
  }

}
