/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.NullAttributes.NULL_ATTRIBUTES;
import static org.mule.runtime.core.el.mvel.MessageVariableResolverFactory.ATTRIBUTES;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.internal.message.InternalMessage;

import org.junit.Before;
import org.junit.Test;

public class AttributesTestCase extends AbstractELTestCase {

  private Event event;
  private InternalMessage message;

  public AttributesTestCase(String mvelOptimizer) {
    super(mvelOptimizer);
  }

  @Before
  public void setup() {
    message = mock(InternalMessage.class);
    event = Event.builder(context).message(message).build();
  }

  @Test
  public void attributes() throws Exception {
    Attributes attributes = NULL_ATTRIBUTES;
    when(message.getAttributes()).thenReturn(new TypedValue<>(attributes, DataType.OBJECT));
    assertThat(evaluate(ATTRIBUTES, event), equalTo(attributes));
  }

}
