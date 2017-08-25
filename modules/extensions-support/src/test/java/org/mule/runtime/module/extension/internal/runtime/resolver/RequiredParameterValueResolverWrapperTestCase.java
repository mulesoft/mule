/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.tck.size.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class RequiredParameterValueResolverWrapperTestCase extends AbstractValueResolverWrapperTestCase<Object> {

  private static final String PARAMETER_NAME = "requiredParam";
  private static final String EXPRESSION = "#[someExpression]";

  @Rule
  public ExpectedException expectedException = none();

  @Mock
  private ValueResolvingContext context;

  @Override
  protected AbstractValueResolverWrapper<Object> createWrapper(ValueResolver<Object> delegate) {
    return new RequiredParameterValueResolverWrapper<>(delegate, PARAMETER_NAME, EXPRESSION);
  }

  @Override
  protected ValueResolver<Object> createDelegate() {
    return mock(ValueResolver.class, withSettings().extraInterfaces(Lifecycle.class));
  }

  @Test
  public void nonNullValue() throws Exception {
    Object value = new Object();
    when(delegate.resolve(context)).thenReturn(value);

    assertThat(wrapper.resolve(context), is(sameInstance(value)));
  }

  @Test
  public void resolveNullValue() throws Exception {
    when(delegate.resolve(context)).thenReturn(null);
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(allOf(containsString(PARAMETER_NAME), containsString(EXPRESSION)));

    wrapper.resolve(context);
  }
}
