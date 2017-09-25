/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext.from;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class StaticValueResolverTestCase extends AbstractMuleTestCase {

  @Mock
  private CoreEvent event;

  private ValueResolver resolver;

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
    assertThat(resolver.resolve(ValueResolvingContext.from(event)), is(expected));
  }
}
