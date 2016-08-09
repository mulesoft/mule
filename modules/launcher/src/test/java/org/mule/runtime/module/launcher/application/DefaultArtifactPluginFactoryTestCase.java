/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.application;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleClassLoaderLookupPolicy;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

@SmallTest
public class DefaultArtifactPluginFactoryTestCase extends AbstractMuleTestCase {

  @Test
  public void createApplicationPlugin() throws MalformedURLException, ClassNotFoundException {
    ArtifactPluginDescriptor descriptor = mock(ArtifactPluginDescriptor.class);
    when(descriptor.getName()).thenReturn("aPlugin");
    URL[] urls = new URL[] {getClass().getClassLoader().getResource("lib/bar-1.0.jar")};
    when(descriptor.getRuntimeLibs()).thenReturn(urls);
    when(descriptor.getRuntimeClassesDir()).thenReturn(getClass().getClassLoader().getResource("org/foo/"));

    ArtifactClassLoader parentClassLoader = new MuleArtifactClassLoader("mule", new URL[0], getClass().getClassLoader(),
                                                                        new MuleClassLoaderLookupPolicy(emptyMap(), emptySet()));
    ArtifactPlugin appPlugin =
        new DefaultArtifactPluginFactory(new ArtifactPluginClassLoaderFactory()).create(descriptor, parentClassLoader);

    // Look for a class in bar-1.0.jar to check classloader has been correctly set
    assertThat(Class.forName("org.bar.BarUtils", true, appPlugin.getArtifactClassLoader().getClassLoader()), is(notNullValue()));
    assertThat(appPlugin.getArtifactName(), is("aPlugin"));
    assertThat(appPlugin.getDescriptor(), is(descriptor));
  }
}
