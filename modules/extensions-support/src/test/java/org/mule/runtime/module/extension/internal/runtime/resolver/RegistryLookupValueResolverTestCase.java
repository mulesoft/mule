/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.HELLO_WORLD;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class RegistryLookupValueResolverTestCase extends AbstractMuleTestCase {

  private static final String KEY = "key";
  private static final String FAKE_KEY = "not there";

  @Mock(answer = RETURNS_DEEP_STUBS)
  private InternalEvent event;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private MuleContext muleContext;

  private ValueResolver resolver;

  @Before
  public void before() throws Exception {
    when(muleContext.getRegistry().get(KEY)).thenReturn(HELLO_WORLD);
    when(muleContext.getRegistry().get(FAKE_KEY)).thenReturn(null);
    resolver = new RegistryLookupValueResolver(KEY);
    ((RegistryLookupValueResolver) resolver).setMuleContext(muleContext);
  }

  @Test
  public void cache() throws Exception {
    Object value = resolver.resolve(ValueResolvingContext.from(event));
    assertThat(value, is(HELLO_WORLD));
    verify(muleContext.getRegistry()).get(KEY);
  }

  @Test
  public void isDynamic() {
    assertThat(resolver.isDynamic(), is(false));
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullKey() {
    new RegistryLookupValueResolver(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void blankKey() {
    new RegistryLookupValueResolver("");
  }

  @Test(expected = ConfigurationException.class)
  public void nonExistingKey() throws Exception {
    RegistryLookupValueResolver<Object> valueResolver = new RegistryLookupValueResolver<>(FAKE_KEY);
    valueResolver.setMuleContext(muleContext);
    valueResolver.resolve(ValueResolvingContext.from(event));
  }
}
