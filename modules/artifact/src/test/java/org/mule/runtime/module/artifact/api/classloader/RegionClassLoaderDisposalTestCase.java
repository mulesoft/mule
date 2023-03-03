/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy.CHILD_FIRST;
import static org.mule.runtime.module.artifact.api.classloader.DefaultArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import static org.mule.tck.junit4.matcher.Eventually.eventually;
import static org.mule.tck.util.CollectableReference.collectedByGc;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.tck.util.CollectableReference;

import java.net.MalformedURLException;
import java.util.Collection;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;


@RunWith(Parameterized.class)
public class RegionClassLoaderDisposalTestCase extends RegionClassLoaderTestCase {

  @Parameter
  public boolean deprecatedDisposal;

  public RegionClassLoaderDisposalTestCase() throws MalformedURLException {}

  @Parameters(name = "Dispose with deprecated method: {0}")
  public static Collection<Boolean> parameters() {
    return asList(true, false);
  }

  @Test
  public void disposesClassLoaders() {
    when(lookupPolicy.getClassLookupStrategy(anyString())).thenReturn(PARENT_FIRST);

    RegionClassLoader regionClassLoader =
        new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, getClass().getClassLoader(), lookupPolicy);
    createClassLoaders(regionClassLoader);

    final ArtifactClassLoader ownerClassLoader = spy(new TestApplicationClassLoader(regionClassLoader));
    final ArtifactClassLoader regionMember2 = mock(ArtifactClassLoader.class, RETURNS_DEEP_STUBS);
    regionClassLoader.addClassLoader(ownerClassLoader, NULL_CLASSLOADER_FILTER);
    regionClassLoader.addClassLoader(regionMember2, NULL_CLASSLOADER_FILTER);

    if (deprecatedDisposal) {
      regionClassLoader.dispose();
      verify(ownerClassLoader).dispose();
    } else {
      regionClassLoader.disposeFromOwnerClassLoader();
      verify(ownerClassLoader, never()).dispose();
    }

    verify(regionMember2).dispose();
  }

  @Test
  public void disposesClassLoadersEvenOnExceptionOnRegionOwner() {
    when(lookupPolicy.getClassLookupStrategy(anyString())).thenReturn(PARENT_FIRST);

    RegionClassLoader regionClassLoader =
        new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, getClass().getClassLoader(), lookupPolicy);
    createClassLoaders(regionClassLoader);

    final ArtifactClassLoader ownerClassLoader = spy(new TestApplicationClassLoader(regionClassLoader));
    doThrow(new RuntimeException()).when(ownerClassLoader).dispose();
    final ArtifactClassLoader regionMember2 = mock(ArtifactClassLoader.class, RETURNS_DEEP_STUBS);

    regionClassLoader.addClassLoader(ownerClassLoader, NULL_CLASSLOADER_FILTER);
    regionClassLoader.addClassLoader(regionMember2, NULL_CLASSLOADER_FILTER);

    if (deprecatedDisposal) {
      regionClassLoader.dispose();
      verify(ownerClassLoader).dispose();
    } else {
      regionClassLoader.disposeFromOwnerClassLoader();
      verify(ownerClassLoader, never()).dispose();
    }

    verify(regionMember2).dispose();
  }

  @Test
  public void disposesClassLoadersEvenOnExceptionOnRegionMember() {
    when(lookupPolicy.getClassLookupStrategy(anyString())).thenReturn(PARENT_FIRST);

    RegionClassLoader regionClassLoader =
        new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, getClass().getClassLoader(), lookupPolicy);
    createClassLoaders(regionClassLoader);

    final ArtifactClassLoader ownerClassLoader = spy(new RegionClassLoaderTestCase.TestApplicationClassLoader(regionClassLoader));
    final ArtifactClassLoader regionMember2 = mock(ArtifactClassLoader.class, RETURNS_DEEP_STUBS);
    doThrow(new RuntimeException()).when(regionMember2).dispose();
    final ArtifactClassLoader regionMember3 = mock(ArtifactClassLoader.class, RETURNS_DEEP_STUBS);

    regionClassLoader.addClassLoader(ownerClassLoader, NULL_CLASSLOADER_FILTER);
    regionClassLoader.addClassLoader(regionMember2, NULL_CLASSLOADER_FILTER);
    regionClassLoader.addClassLoader(regionMember3, NULL_CLASSLOADER_FILTER);

    if (deprecatedDisposal) {
      regionClassLoader.dispose();
      verify(ownerClassLoader).dispose();
    } else {
      regionClassLoader.disposeFromOwnerClassLoader();
      verify(ownerClassLoader, never()).dispose();
    }

    verify(regionMember2).dispose();
    verify(regionMember3).dispose();
  }

  @Test
  @Issue("W-11698566")
  @Description("The RegionClassLoader does not keep a reference to packages and resources mappings, which could cause a MuleArtifactClassLoader leak.")
  public void regionClassLoaderDoesNotLeakPackageMappingsAfterDispose() throws ClassNotFoundException {
    when(lookupPolicy.getPackageLookupStrategy(PACKAGE_NAME)).thenReturn(CHILD_FIRST);

    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);
    createClassLoaders(regionClassLoader);
    regionClassLoader.addClassLoader(appClassLoader, NULL_CLASSLOADER_FILTER);
    regionClassLoader.addClassLoader(pluginClassLoader,
                                     new DefaultArtifactClassLoaderFilter(singleton(PACKAGE_NAME), emptySet()));
    pluginClassLoader.addClass(CLASS_NAME, PLUGIN_LOADED_CLASS);

    CollectableReference<TestArtifactClassLoader> collectableReference = new CollectableReference<>(pluginClassLoader);

    pluginClassLoader = null;

    if (deprecatedDisposal) {
      regionClassLoader.dispose();
    } else {
      regionClassLoader.disposeFromOwnerClassLoader();
    }

    assertThat(collectableReference, is(eventually(collectedByGc())));
  }

}
