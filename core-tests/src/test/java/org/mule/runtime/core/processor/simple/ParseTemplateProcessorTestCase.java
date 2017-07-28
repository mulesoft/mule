/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.internal.processor.simple.ParseTemplateProcessor;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.HashMap;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class ParseTemplateProcessorTestCase {

  private static final String LOCATION = "error.html";
  private static final String INVALID_LOCATION = "wrong_error.html";

  private ParseTemplateProcessor parseTemplateProcessor;
  private Event mockMuleEvent = mock(Event.class);
  private InternalMessage mockMuleMessage = mock(InternalMessage.class);
  private MuleContext mockMuleContext = mock(MuleContext.class);
  private ExtendedExpressionManager mockExpressionManager = mock(ExtendedExpressionManager.class);

  @Before
  public void setUp() {
    parseTemplateProcessor = new ParseTemplateProcessor();
    parseTemplateProcessor.setMuleContext(mockMuleContext);
    FlowCallStack flowCallStack = mock(FlowCallStack.class);
    Optional<Error> error = Optional.empty();
    when(flowCallStack.clone()).thenReturn(null);
    when(mockMuleEvent.getError()).thenReturn(error);
    when(mockMuleEvent.getFlowCallStack()).thenReturn(flowCallStack);
    when(mockMuleEvent.getMessage()).thenReturn(mockMuleMessage);
    when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseTemplateNullTemplate() throws InitialisationException {
    parseTemplateProcessor.setLocation(LOCATION);
    parseTemplateProcessor.process(mockMuleEvent);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseTemplateNullLocation() throws InitialisationException {
    parseTemplateProcessor.setLocation(null);
    parseTemplateProcessor.initialise();
    parseTemplateProcessor.process(mockMuleEvent);
  }

  @Test(expected = InitialisationException.class)
  public void testParseTemplateInvalidLocation() throws InitialisationException {
    parseTemplateProcessor.setLocation(INVALID_LOCATION);
    parseTemplateProcessor.initialise();
    parseTemplateProcessor.process(mockMuleEvent);
  }

  @Test(expected = InitialisationException.class)
  public void testParseTemplateWithBothLocationAndContentDefined() throws InitialisationException {
    parseTemplateProcessor.setLocation(LOCATION);
    parseTemplateProcessor.setContent("SOME CONTENT");
    parseTemplateProcessor.initialise();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseTemplateNullContent() throws InitialisationException {
    parseTemplateProcessor.initialise();
    parseTemplateProcessor.process(mockMuleEvent);
  }

  @Test(expected = InitialisationException.class)
  public void testNonExistentEncoding() throws InitialisationException {
    parseTemplateProcessor.setEncoding("NON_EXISTENT_ENCODING");
    parseTemplateProcessor.initialise();
  }

  @Test
  public void testParseTemplateFromLocation() throws InitialisationException, IOException {
    parseTemplateProcessor.setLocation(LOCATION);
    parseTemplateProcessor.initialise();
    when(mockMuleMessage.getInboundProperty("errorMessage")).thenReturn("ERROR!!!");
    String expectedExpression = IOUtils.getResourceAsString(LOCATION, this.getClass());

    when(mockMuleMessage.getPayload()).thenReturn(TypedValue.of("Parsed"));
    when(mockMuleMessage.getAttributes()).thenReturn(TypedValue.of(new HashMap<>()));
    when(mockExpressionManager.parse(expectedExpression, mockMuleEvent, null)).thenReturn("Parsed");

    Event response = parseTemplateProcessor.process(mockMuleEvent);
    assertNotNull(response);
    assertEquals("Parsed", response.getMessage().getPayload().getValue());

    // Call a second time to make sure the template is stored once the transformer has been initialized
    response = parseTemplateProcessor.process(mockMuleEvent);
    assertNotNull(response);
    assertEquals("Parsed", response.getMessage().getPayload().getValue());
  }

  @Test
  public void testParseTemplateFromContent() throws InitialisationException {
    String template = "This is a template";
    parseTemplateProcessor.setContent(template);
    parseTemplateProcessor.initialise();

    when(mockMuleMessage.getPayload()).thenReturn(TypedValue.of(template));
    when(mockMuleMessage.getAttributes()).thenReturn(TypedValue.of(new HashMap<>()));
    when(mockExpressionManager.parse(template, mockMuleEvent, null)).thenReturn(template);

    Event response = parseTemplateProcessor.process(mockMuleEvent);
    assertNotNull(response);
    assertEquals(template, (String) response.getMessage().getPayload().getValue());

    // Call a second time to make sure the template is stored once the transformer has been initialized
    response = parseTemplateProcessor.process(mockMuleEvent);
    assertNotNull(response);
    assertEquals(template, (String) response.getMessage().getPayload().getValue());
  }

  @Test
  public void testParseTemplateToTarget() throws InitialisationException {
    String payload = "Payload";
    String template = "Template";
    parseTemplateProcessor.setContent(template);
    parseTemplateProcessor.setTarget("some_target_variable");
    parseTemplateProcessor.initialise();

    when(mockMuleMessage.getPayload()).thenReturn(TypedValue.of(payload));
    when(mockMuleMessage.getAttributes()).thenReturn(TypedValue.of(new HashMap<>()));
    when(mockExpressionManager.parse(template, mockMuleEvent, null)).thenReturn("Parsed");

    Event response = parseTemplateProcessor.process(mockMuleEvent);
    assertNotNull(response);
    assertEquals(payload, (String) response.getMessage().getPayload().getValue());
    assertEquals("Parsed", (String) ((Message) response.getVariable("some_target_variable").getValue()).getPayload().getValue());
  }

  @Test
  public void testParseTemplateWithEncoding() throws InitialisationException {
    String payload = "Payload";
    String template = "Template";
    parseTemplateProcessor.setContent(template);
    parseTemplateProcessor.setEncoding("UTF-8");
    parseTemplateProcessor.initialise();

    when(mockMuleMessage.getPayload()).thenReturn(TypedValue.of(payload));
    when(mockMuleMessage.getAttributes()).thenReturn(TypedValue.of(new HashMap<>()));
    when(mockExpressionManager.parse(template, mockMuleEvent, null)).thenReturn("nón äscíí éncódíng");

    Event response = parseTemplateProcessor.process(mockMuleEvent);
    ByteBuffer result = (ByteBuffer) response.getMessage().getPayload().getValue();
    assertNotNull(response);
    try {
      Charset.forName("US-ASCII").newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(result);
      fail();
    } catch (Exception e) {
      Charset.forName("UTF-8").decode(result);
    }
  }

}
