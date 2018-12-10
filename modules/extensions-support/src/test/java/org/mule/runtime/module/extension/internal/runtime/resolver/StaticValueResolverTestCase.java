/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class StaticValueResolverTestCase extends AbstractMuleTestCase {

  @Mock(lenient = true)
  private ExpressionManager expressionManager;

  @Mock
  private CoreEvent event;

  private ValueResolver resolver;

  @Before
  public void before() {
    when(expressionManager.openSession(any())).thenReturn(mock(ExpressionManagerSession.class));
  }

  @Test
  public void staticValue() throws Exception {
    assertExpected(new Object());
  }

  @Test
  public void nullValue() throws Exception {
    assertExpected(null);
  }

  @Test
  public void nullEvent() throws Exception {
    event = null;
    staticValue();
  }

  private void assertExpected(Object expected) throws Exception {
    resolver = new StaticValueResolver(expected);
    assertThat(resolver.resolve(ValueResolvingContext.builder(event).build()), is(expected));
  }
}
