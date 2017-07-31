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

  private static final String LOCATION = "error.html";
  private static final String INVALID_LOCATION = "wrong_error.html";

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
  public void testParseTemplateNullTemplate() throws TransformerException, InitialisationException {
    parseTemplateTransformer.setLocation(LOCATION);
    parseTemplateTransformer.transformMessage(mockMuleEvent, UTF_8);
  }

  @Test(expected = InitialisationException.class)
  public void testParseTemplateNullLocation() throws TransformerException, InitialisationException {
    parseTemplateTransformer.setLocation(null);
    parseTemplateTransformer.initialise();
    parseTemplateTransformer.transformMessage(mockMuleEvent, UTF_8);
  }

  @Test(expected = InitialisationException.class)
  public void testParseTemplateInvalidLocation() throws TransformerException, InitialisationException {
    parseTemplateTransformer.setLocation(INVALID_LOCATION);
    parseTemplateTransformer.initialise();
    parseTemplateTransformer.transformMessage(mockMuleEvent, UTF_8);
  }

  @Test
  public void testParseTemplate() throws TransformerException, InitialisationException, IOException {
    parseTemplateTransformer.setLocation(LOCATION);
    parseTemplateTransformer.initialise();
    when(mockMuleMessage.getInboundProperty("errorMessage")).thenReturn("ERROR!!!");
    String expectedExpression = IOUtils.getResourceAsString(LOCATION, this.getClass());

    when(mockExpressionManager.parse(expectedExpression, mockMuleEvent, null)).thenReturn("Parsed");

    Object response = parseTemplateTransformer.transformMessage(mockMuleEvent, UTF_8);
    assertNotNull(response);
    assertEquals("Parsed", response);

    // Call a second time to make sure the template is stored once the transformer has been initialized
    response = parseTemplateTransformer.transformMessage(mockMuleEvent, UTF_8);
    assertNotNull(response);
    assertEquals("Parsed", response);
  }

}
