/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

@SmallTest
public class ParseTemplateProcessorTestCase extends AbstractMuleTestCase {

  private static final String LOCATION = "error.html";
  private static final String INVALID_LOCATION = "wrong_error.html";

  private ParseTemplateProcessor parseTemplateProcessor;
  private CoreEvent event;
  private InternalMessage mockMuleMessage = mock(InternalMessage.class);
  private MuleContext mockMuleContext = mock(MuleContext.class);
  private ExtendedExpressionManager mockExpressionManager = mock(ExtendedExpressionManager.class);

  @Before
  public void setUp() throws MuleException {
    event = getEventBuilder().message(mockMuleMessage).build();

    parseTemplateProcessor = new ParseTemplateProcessor();
    parseTemplateProcessor.setMuleContext(mockMuleContext);
    when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseTemplateNullTemplate() throws InitialisationException {
    parseTemplateProcessor.setLocation(LOCATION);
    parseTemplateProcessor.process(event);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseTemplateNullLocation() throws InitialisationException {
    parseTemplateProcessor.setLocation(null);
    parseTemplateProcessor.initialise();
    parseTemplateProcessor.process(event);
  }

  @Test(expected = InitialisationException.class)
  public void testParseTemplateInvalidLocation() throws InitialisationException {
    parseTemplateProcessor.setLocation(INVALID_LOCATION);
    addMockComponentLocation(parseTemplateProcessor);
    parseTemplateProcessor.initialise();
    parseTemplateProcessor.process(event);
  }

  @Test(expected = InitialisationException.class)
  public void testParseTemplateWithBothLocationAndContentDefined() throws InitialisationException {
    parseTemplateProcessor.setLocation(LOCATION);
    parseTemplateProcessor.setContent("SOME CONTENT");
    addMockComponentLocation(parseTemplateProcessor);
    parseTemplateProcessor.initialise();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseTemplateNullContent() throws InitialisationException {
    parseTemplateProcessor.initialise();
    parseTemplateProcessor.process(event);
  }

  @Test
  public void testParseTemplateFromLocation() throws InitialisationException, IOException {
    parseTemplateProcessor.setLocation(LOCATION);
    parseTemplateProcessor.initialise();
    when(mockMuleMessage.getInboundProperty("errorMessage")).thenReturn("ERROR!!!");
    String expectedExpression = IOUtils.getResourceAsString(LOCATION, this.getClass());

    when(mockMuleMessage.getPayload()).thenReturn(TypedValue.of("Parsed"));
    when(mockMuleMessage.getAttributes()).thenReturn(TypedValue.of(new HashMap<>()));
    when(mockExpressionManager.parse(expectedExpression, event, null)).thenReturn("Parsed");

    CoreEvent response = parseTemplateProcessor.process(event);
    assertNotNull(response);
    assertEquals("Parsed", response.getMessage().getPayload().getValue());

    // Call a second time to make sure the template is stored once the transformer has been initialized
    response = parseTemplateProcessor.process(event);
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
    when(mockExpressionManager.parse(template, event, null)).thenReturn(template);

    CoreEvent response = parseTemplateProcessor.process(event);
    assertNotNull(response);
    assertEquals(template, response.getMessage().getPayload().getValue());

    // Call a second time to make sure the template is stored once the transformer has been initialized
    response = parseTemplateProcessor.process(event);
    assertNotNull(response);
    assertEquals(template, response.getMessage().getPayload().getValue());
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
    when(mockExpressionManager.parse(any(), any(), any())).thenReturn("Parsed");

    CoreEvent response = parseTemplateProcessor.process(event);
    assertNotNull(response);
    assertEquals(payload, response.getMessage().getPayload().getValue());
    assertEquals("Parsed",
                 ((Message) response.getVariables().get("some_target_variable").getValue()).getPayload().getValue());
  }
}
