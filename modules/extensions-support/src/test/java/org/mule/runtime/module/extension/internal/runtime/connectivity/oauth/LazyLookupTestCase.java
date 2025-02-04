/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.privileged.registry.RegistrationException;

import org.junit.Test;

public class LazyLookupTestCase {

  @Test
  public void testSuccessfulLookup() throws RegistrationException {
    Class<String> type = String.class;
    MuleContext muleContext = mock(MuleContextWithRegistry.class);
    MuleRegistry registry = mock(MuleRegistry.class);
    when(((MuleContextWithRegistry) muleContext).getRegistry()).thenReturn(registry);
    when(registry.lookupObject(type)).thenReturn("TestValue");

    LazyLookup<String> lazyLookup = new LazyLookup<>(type, muleContext);
    assertThat(lazyLookup.get(), is("TestValue"));
  }

  @Test(expected = MuleRuntimeException.class)
  public void testLookupThrowsException() {
    Class<String> type = String.class;
    MuleContext muleContext = mock(MuleContextWithRegistry.class);
    when(((MuleContextWithRegistry) muleContext).getRegistry()).thenThrow(new RuntimeException("Registry failure"));

    LazyLookup<String> lazyLookup = new LazyLookup<>(type, muleContext);
    lazyLookup.get();
  }
}
