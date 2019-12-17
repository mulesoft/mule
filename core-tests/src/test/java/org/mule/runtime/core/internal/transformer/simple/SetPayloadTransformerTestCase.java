/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.DataType.STRING;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class SetPayloadTransformerTestCase extends AbstractMuleContextTestCase {

  private static final TypedValue<String> PLAIN_TEXT = new TypedValue<>("This is a plain text", STRING);
  private static final String EXPRESSION = "#[vars.myVar]";
  private static final String VAR_NAME = "myVar";

  private SetPayloadTransformer setPayloadTransformer;
  private CoreEvent event;

  @Before
  public void setUp() {
    setPayloadTransformer = new SetPayloadTransformer();
    setPayloadTransformer.setMuleContext(muleContext);
    event = CoreEvent.builder(mock(BaseEventContext.class))
        .addVariable(VAR_NAME, PLAIN_TEXT)
        .message(of("")).build();
  }

  @Test
  public void testSetPayloadTransformerNulValue() throws InitialisationException, MessageTransformerException {
    setPayloadTransformer.setValue(null);
    setPayloadTransformer.initialise();

    Object response = setPayloadTransformer.transformMessage(event, UTF_8);
    assertThat(response, is(nullValue()));
  }

  @Test
  public void testSetPayloadTransformerPlainText() throws InitialisationException, MessageTransformerException {
    setPayloadTransformer.setValue(PLAIN_TEXT.getValue());
    setPayloadTransformer.initialise();

    Object response = setPayloadTransformer.transformMessage(event, UTF_8);
    assertThat(response, is(PLAIN_TEXT.getValue()));
  }

  @Test
  public void testSetPayloadTransformerExpression() throws InitialisationException, MessageTransformerException {
    setPayloadTransformer.setValue(EXPRESSION);
    setPayloadTransformer.initialise();

    Object response = setPayloadTransformer.transformMessage(event, UTF_8);
    assertThat(response, is(PLAIN_TEXT.getValue()));
  }
}
