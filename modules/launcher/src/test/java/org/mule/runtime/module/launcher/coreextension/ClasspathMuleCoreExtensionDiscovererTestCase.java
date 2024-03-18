/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.coreextension;

import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.launcher.coreextension.ClasspathMuleCoreExtensionDiscoverer.CORE_EXTENSION_RESOURCE_NAME;

import static java.util.Collections.enumeration;
import static java.util.Collections.singleton;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.net.URL;
import java.util.List;

import org.junit.Test;

public class ClasspathMuleCoreExtensionDiscovererTestCase extends AbstractMuleTestCase {

  @Test
  public void setsContainerClassLoaderOnDiscoveredExtensions() throws Exception {
    final ArtifactClassLoader artifactClassLoader = mock(ArtifactClassLoader.class);
    final ClassLoader classLoader = mock(ClassLoader.class);
    final URL resource = getClass().getClassLoader().getResource("test-core-extension.properties");
    when(classLoader.getResources(CORE_EXTENSION_RESOURCE_NAME))
        .thenReturn(enumeration(singleton(resource)));
    when(artifactClassLoader.getClassLoader()).thenReturn(classLoader);

    final ClasspathMuleCoreExtensionDiscoverer discoverer = new ClasspathMuleCoreExtensionDiscoverer(artifactClassLoader);

    // Uses context classloader to force discovering of the test properties
    final List<MuleCoreExtension> discover = withContextClassLoader(artifactClassLoader.getClassLoader(), () -> {
      try {
        return discoverer.discover();
      } catch (DefaultMuleException e) {
        throw new IllegalStateException(e);
      }
    });

    assertThat(discover.size(), equalTo(2));
    assertThat(discover.get(0), instanceOf(PrioritizedTestCoreExtension.class));
    assertThat(discover.get(1), instanceOf(TestCoreExtension.class));
    assertThat(((TestCoreExtension) discover.get(1)).containerClassLoader, is(artifactClassLoader));
  }

  public static class TestCoreExtension implements MuleCoreExtension {

    private ArtifactClassLoader containerClassLoader;

    @Override
    public void setContainerClassLoader(ArtifactClassLoader containerClassLoader) {

      this.containerClassLoader = containerClassLoader;
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public void dispose() {

    }

    @Override
    public void initialise() throws InitialisationException {

    }

    @Override
    public void start() throws MuleException {

    }

    @Override
    public void stop() throws MuleException {

    }
  }

  public static class PrioritizedTestCoreExtension extends TestCoreExtension {

    @Override
    public int priority() {
      return 0;
    }
  }
}
