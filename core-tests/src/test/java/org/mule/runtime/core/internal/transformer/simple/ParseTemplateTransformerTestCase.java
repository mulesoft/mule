/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.internal.transformer.simple.ParseTemplateTransformer;
import org.mule.tck.size.SmallTest;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class ParseTemplateTransformerTestCase {

  private ParseTemplateTransformer parseTemplateTransformer;
  private Event mockMuleEvent = mock(Event.class);
  private InternalMessage mockMuleMessage = mock(InternalMessage.class);
  private MuleContext mockMuleContext = mock(MuleContext.class);
  private ExtendedExpressionManager mockExpressionManager = mock(ExtendedExpressionManager.class);

  @Before
  public void setUp() {
    parseTemplateTransformer = new ParseTemplateTransformer();
    parseTemplateTransformer.setMuleContext(mockMuleContext);

    when(mockMuleEvent.getMessage()).thenReturn(mockMuleMessage);
    when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseTemplateNullContent() throws TransformerException, InitialisationException {
    parseTemplateTransformer.initialise();
    parseTemplateTransformer.process(mockMuleEvent);
  }

  @Test
  public void testParseTemplateNoExpression() throws TransformerException, InitialisationException, IOException {
    String template = "This is a template";
    parseTemplateTransformer.setContent(template);
    parseTemplateTransformer.initialise();

    when(mockMuleMessage.getPayload()).thenReturn(TypedValue.of(template));
    when(mockExpressionManager.parse(template, mockMuleEvent, null)).thenReturn(template);

    Event response = parseTemplateTransformer.process(mockMuleEvent);
    assertNotNull(response);
    assertEquals(template, (String)response.getMessage().getPayload().getValue());

    // Call a second time to make sure the template is stored once the transformer has been initialized
    response = parseTemplateTransformer.process(mockMuleEvent);
    assertNotNull(response);
    assertEquals(template, (String)response.getMessage().getPayload().getValue());
  }

}
