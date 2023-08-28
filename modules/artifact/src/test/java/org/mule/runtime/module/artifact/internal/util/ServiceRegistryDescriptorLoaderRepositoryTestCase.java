/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.internal.util;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.PLUGIN;
import static org.mule.runtime.module.artifact.internal.util.ServiceRegistryDescriptorLoaderRepository.noRegisteredLoaderError;
import static org.mule.test.allure.AllureConstants.DescriptorLoaderFeature.DESCRIPTOR_LOADER;
import static org.mule.test.allure.AllureConstants.ServicesFeature.SERVICES;
import static org.mule.test.allure.AllureConstants.ServicesFeature.ServicesStory.SERVICE_REGISTRY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoader;
import org.mule.runtime.module.artifact.api.descriptor.LoaderNotFoundException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;

@SmallTest
@Features({@Feature(SERVICES), @Feature(DESCRIPTOR_LOADER)})
@Story(SERVICE_REGISTRY)
public class ServiceRegistryDescriptorLoaderRepositoryTestCase extends AbstractMuleTestCase {

  private static final String LOADER_ID = "loader";

  private final ClassLoaderConfigurationLoader classLoaderConfigurationLoader = mock(ClassLoaderConfigurationLoader.class);
  private final Function<Class<? extends DescriptorLoader>, Stream<? extends DescriptorLoader>> serviceRegistry =
      spyLambda(clazz -> Stream.of(classLoaderConfigurationLoader), Function.class);

  /**
   * This method overcomes the issue with the original Mockito.spy when passing a lambda which fails with an error saying that the
   * passed class is final.
   */
  @SuppressWarnings("unchecked")
  public static <R, G extends R> G spyLambda(final G lambda, final Class<R> lambdaType) {
    return (G) mock(lambdaType, delegatesTo(lambda));
  }

  private final ServiceRegistryDescriptorLoaderRepository repository =
      new ServiceRegistryDescriptorLoaderRepository(serviceRegistry);
  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void initializesClassLoaderConfigurationLoadersOnce() throws Exception {
    when(classLoaderConfigurationLoader.getId()).thenReturn(LOADER_ID);
    when(classLoaderConfigurationLoader.supportsArtifactType(PLUGIN)).thenReturn(true);

    repository.get(LOADER_ID, PLUGIN, ClassLoaderConfigurationLoader.class);
    repository.get(LOADER_ID, PLUGIN, ClassLoaderConfigurationLoader.class);

    verify(serviceRegistry, times(1)).apply(ClassLoaderConfigurationLoader.class);
    verify(serviceRegistry, times(1)).apply(BundleDescriptorLoader.class);
  }

  @Test
  public void doesNotFindInvalidLoaderId() throws Exception {
    when(classLoaderConfigurationLoader.getId()).thenReturn(LOADER_ID);

    expectedException.expect(LoaderNotFoundException.class);
    expectedException.expectMessage(noRegisteredLoaderError("invalid", ClassLoaderConfigurationLoader.class));

    repository.get("invalid", PLUGIN, ClassLoaderConfigurationLoader.class);
  }

  @Test
  public void findsLoader() throws Exception {
    when(classLoaderConfigurationLoader.getId()).thenReturn(LOADER_ID);
    when(classLoaderConfigurationLoader.supportsArtifactType(PLUGIN)).thenReturn(true);
    ClassLoaderConfigurationLoader classLoaderConfigurationLoader =
        repository.get(LOADER_ID, PLUGIN, ClassLoaderConfigurationLoader.class);

    assertThat(classLoaderConfigurationLoader, is(classLoaderConfigurationLoader));
  }

  @Test
  public void findsLoaderIdWithType() throws Exception {
    when(classLoaderConfigurationLoader.getId()).thenReturn(LOADER_ID);

    expectedException.expect(LoaderNotFoundException.class);
    expectedException.expectMessage(noRegisteredLoaderError(LOADER_ID, BundleDescriptorLoader.class));

    repository.get(LOADER_ID, PLUGIN, BundleDescriptorLoader.class);
  }
}
