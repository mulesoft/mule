/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.internal.registry.RegistryDelegatingInjector;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class RegistryDelegatingInjectorTestCase extends AbstractMuleTestCase {

  @Mock
  private RegistryProvider registryProvider;

  @Mock(extraInterfaces = Injector.class)
  private Registry injectorRegistry;

  private Injector injector;

  @Before
  public void before() {
    injector = new RegistryDelegatingInjector(registryProvider);
  }

  @Test
  public void inject() throws Exception {
    when(registryProvider.getRegistries()).thenReturn(asList(mock(Registry.class), injectorRegistry));
    Object target = new Object();
    Object injected = new Object();

    when(((Injector) injectorRegistry).inject(target)).thenReturn(injected);
    assertThat(injector.inject(target), is(injected));
  }

  @Test
  public void noSuitableRegistry() throws Exception {
    when(registryProvider.getRegistries()).thenReturn(asList(mock(Registry.class)));
    assertNoinjection();
  }

  @Test
  public void noRegistriesAtAll() throws Exception {
    when(registryProvider.getRegistries()).thenReturn(ImmutableList.<Registry>of());
    assertNoinjection();
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullProvider() {
    new RegistryDelegatingInjector(null);
  }

  private void assertNoinjection() throws MuleException {
    Object target = mock(Object.class);

    assertThat(injector.inject(target), is(sameInstance(target)));
    verifyNoMoreInteractions(target);
  }

}
