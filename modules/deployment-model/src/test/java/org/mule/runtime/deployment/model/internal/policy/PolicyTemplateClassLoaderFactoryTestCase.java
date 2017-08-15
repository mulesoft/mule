/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.policy;

import static java.util.Collections.emptyList;
import static junit.framework.TestCase.fail;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class PolicyTemplateClassLoaderFactoryTestCase extends AbstractMuleTestCase {

  private static final String POLICY_ID = "policy/policyId";

  private final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
  @Rule
  public TemporaryFolder policyFolder = new TemporaryFolder();
  private PolicyTemplateClassLoaderFactory factory = new PolicyTemplateClassLoaderFactory();
  private PolicyTemplateDescriptor descriptor;
  private ArtifactClassLoader parentClassLoader;

  @Before
  public void setUp() throws Exception {
    descriptor = new PolicyTemplateDescriptor("testPolicy");
    descriptor.setRootFolder(policyFolder.getRoot());

    parentClassLoader = mock(ArtifactClassLoader.class);
    when(parentClassLoader.getClassLoader()).thenReturn(getClass().getClassLoader());
    when(lookupPolicy.getClassLookupStrategy(anyString())).thenReturn(PARENT_FIRST);
    when(parentClassLoader.getClassLoaderLookupPolicy()).thenReturn(lookupPolicy);
  }

  @Test
  public void createsEmptyClassLoader() throws Exception {
    final ArtifactClassLoader artifactClassLoader = factory.create(POLICY_ID, parentClassLoader, descriptor, emptyList());
    final MuleArtifactClassLoader classLoader = (MuleArtifactClassLoader) artifactClassLoader.getClassLoader();
    assertThat(classLoader.getURLs(), equalTo(new URL[0]));
  }

  @Test(expected = IllegalArgumentException.class)
  public void validatesPolicyFolder() throws Exception {
    File fakePolicyFolder = new File("./fake/folder/for/test");
    descriptor.setRootFolder(fakePolicyFolder);
    factory.create(POLICY_ID, null, descriptor, emptyList());
  }

  @Test
  public void usesClassLoaderLookupPolicy() throws Exception {
    final ArtifactClassLoader artifactClassLoader = factory.create(POLICY_ID, parentClassLoader, descriptor, emptyList());
    final MuleArtifactClassLoader classLoader = (MuleArtifactClassLoader) artifactClassLoader.getClassLoader();

    final String className = "com.dummy.Foo";
    try {
      classLoader.loadClass(className);
      fail("Able to load an un-existent class");
    } catch (ClassNotFoundException e) {
      // Expected
    }

    verify(lookupPolicy).getClassLookupStrategy(className);
  }

  @Test
  public void createsClassLoaderWithPlugins() throws Exception {
    ArtifactClassLoader pluginClassLoader1 = mock(ArtifactClassLoader.class);
    ArtifactClassLoader pluginClassLoader2 = mock(ArtifactClassLoader.class);
    List<ArtifactClassLoader> artifactPluginClassLoaders = new ArrayList<>();
    artifactPluginClassLoaders.add(pluginClassLoader1);
    artifactPluginClassLoaders.add(pluginClassLoader2);

    final ArtifactClassLoader artifactClassLoader = factory.create(POLICY_ID, parentClassLoader, descriptor,
                                                                   artifactPluginClassLoaders);
    final MuleDeployableArtifactClassLoader classLoader =
        (MuleDeployableArtifactClassLoader) artifactClassLoader.getClassLoader();

    assertThat(classLoader.getArtifactPluginClassLoaders(), contains(pluginClassLoader1, pluginClassLoader2));
  }
}
