/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.simple;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.create;
import static org.mule.runtime.api.metadata.TypedValue.of;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.HashMap;

@SmallTest
public class ParseTemplateProcessorTestCase extends AbstractMuleTestCase {

  private static final String LOCATION = "error.html";
  private static final MediaType LOCATION_MEDIA_TYPE = create("text", "html");
  private static final String INVALID_LOCATION = "wrong_error.html";

  private static final String UNKNOWN_MEDIATYPE_LOCATION = "template.lrmextension";

  private ParseTemplateProcessor parseTemplateProcessor;
  private CoreEvent event;
  private InternalMessage mockMuleMessage = mock(InternalMessage.class);
  private MuleContext mockMuleContext = mock(MuleContext.class);
  private ExtendedExpressionManager mockExpressionManager = mock(ExtendedExpressionManager.class);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() throws MuleException {
    event = getEventBuilder().message(mockMuleMessage).build();

    parseTemplateProcessor = new ParseTemplateProcessor();
    parseTemplateProcessor.setMuleContext(mockMuleContext);
    when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
  }

  @Test(expected = InitialisationException.class)
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
  }

  @Test(expected = InitialisationException.class)
  public void testParseTemplateWithBothLocationAndContentDefined() throws InitialisationException {
    parseTemplateProcessor.setLocation(LOCATION);
    parseTemplateProcessor.setContent("SOME CONTENT");
    addMockComponentLocation(parseTemplateProcessor);
    parseTemplateProcessor.initialise();
  }

  @Test(expected = InitialisationException.class)
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

    when(mockMuleMessage.getPayload()).thenReturn(of("Parsed"));
    when(mockMuleMessage.getAttributes()).thenReturn(of(new HashMap<>()));
    when(mockExpressionManager.parseLogTemplate(eq(expectedExpression), eq(event), any(), any())).thenReturn("Parsed");

    CoreEvent response = parseTemplateProcessor.process(event);
    assertNotNull(response);
    assertEquals("Parsed", response.getMessage().getPayload().getValue());

    // Call a second time to make sure the template is stored once the transformer has been initialized
    response = parseTemplateProcessor.process(event);
    assertNotNull(response);
    assertEquals("Parsed", response.getMessage().getPayload().getValue());
  }

  @Test
  public void parseTemplateFromLocationWithKnownMediaType() throws Exception {
    parseTemplateProcessor.setLocation(LOCATION);
    parseTemplateProcessor.initialise();
    String expectedExpression = IOUtils.getResourceAsString(LOCATION, this.getClass());
    when(mockExpressionManager.parseLogTemplate(eq(expectedExpression), eq(event), any(), any())).thenReturn("Parsed");
    CoreEvent response = parseTemplateProcessor.process(event);
    assertThat(response.getMessage().getPayload().getDataType().getMediaType(), is(equalTo(LOCATION_MEDIA_TYPE)));
  }

  @Test
  public void parseTemplateFromLocationWithUnknownMediaType() throws Exception {
    parseTemplateProcessor.setLocation(UNKNOWN_MEDIATYPE_LOCATION);
    parseTemplateProcessor.initialise();
    String expectedExpression = IOUtils.getResourceAsString(UNKNOWN_MEDIATYPE_LOCATION, this.getClass());
    when(mockExpressionManager.parseLogTemplate(eq(expectedExpression), eq(event), any(), any())).thenReturn("Parsed");
    CoreEvent response = parseTemplateProcessor.process(event);
    assertThat(response.getMessage().getPayload().getDataType().getMediaType(), is(equalTo(ANY)));
  }

  @Test
  public void parseTemplateWithOverriddenDataType() throws Exception {
    String customEncoding = "UTF-16";
    MediaType customMediaType = create("application", "lrmextension");
    parseTemplateProcessor.setLocation(UNKNOWN_MEDIATYPE_LOCATION);
    parseTemplateProcessor.setOutputMimeType(customMediaType.toRfcString());
    parseTemplateProcessor.setOutputEncoding(customEncoding);
    parseTemplateProcessor.initialise();
    String expectedExpression = IOUtils.getResourceAsString(UNKNOWN_MEDIATYPE_LOCATION, this.getClass());
    when(mockExpressionManager.parseLogTemplate(eq(expectedExpression), eq(event), any(), any())).thenReturn("Parsed");
    CoreEvent response = parseTemplateProcessor.process(event);
    assertThat(response.getMessage().getPayload().getDataType().getMediaType().getPrimaryType(),
               is(equalTo(customMediaType.getPrimaryType())));
    assertThat(response.getMessage().getPayload().getDataType().getMediaType().getSubType(),
               is(equalTo(customMediaType.getSubType())));
    assertThat(response.getMessage().getPayload().getDataType().getMediaType().getCharset().get().toString(),
               is(equalTo(customEncoding)));
  }

  @Test
  public void unsupportedEncodingThrowsException() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    parseTemplateProcessor.setOutputEncoding("invalidEncoding");
  }

  @Test
  public void invalidMimeTypeStringThrowsException() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    parseTemplateProcessor.setOutputMimeType("primaryType-wrongDelimiter-subType");
  }

  @Test
  public void parseTemplateWithOverriddenDataTypeAsExpression() throws Exception {
    String customEncoding = "UTF-16";
    MediaType customMediaType = create("application", "lrmextension");
    parseTemplateProcessor.setLocation(UNKNOWN_MEDIATYPE_LOCATION);
    parseTemplateProcessor.setOutputMimeType(customMediaType.toRfcString());
    parseTemplateProcessor.setOutputEncoding(customEncoding);
    parseTemplateProcessor.initialise();
    String expectedExpression = IOUtils.getResourceAsString(UNKNOWN_MEDIATYPE_LOCATION, this.getClass());
    when(mockExpressionManager.parseLogTemplate(eq(expectedExpression), eq(event), any(), any())).thenReturn("Parsed");
    CoreEvent response = parseTemplateProcessor.process(event);
    assertThat(response.getMessage().getPayload().getDataType().getMediaType().getPrimaryType(),
               is(equalTo(customMediaType.getPrimaryType())));
    assertThat(response.getMessage().getPayload().getDataType().getMediaType().getSubType(),
               is(equalTo(customMediaType.getSubType())));
    assertThat(response.getMessage().getPayload().getDataType().getMediaType().getCharset().get().toString(),
               is(equalTo(customEncoding)));
  }

  @Test
  public void parseTemplateFromContent() throws InitialisationException {
    String template = "This is a template";
    parseTemplateProcessor.setContent(template);
    parseTemplateProcessor.initialise();

    when(mockMuleMessage.getPayload()).thenReturn(of(template));
    when(mockMuleMessage.getAttributes()).thenReturn(of(new HashMap<>()));
    when(mockExpressionManager.parseLogTemplate(eq(template), eq(event), any(), any())).thenReturn(template);

    CoreEvent response = parseTemplateProcessor.process(event);
    assertNotNull(response);
    assertEquals(template, response.getMessage().getPayload().getValue());

    // Call a second time to make sure the template is stored once the transformer has been initialized
    response = parseTemplateProcessor.process(event);
    assertNotNull(response);
    assertEquals(template, response.getMessage().getPayload().getValue());
  }

  @Test
  public void parseTemplateToTarget() throws InitialisationException {
    String payload = "Payload";
    String template = "Template";
    parseTemplateProcessor.setContent(template);
    parseTemplateProcessor.setTarget("some_target_variable");
    parseTemplateProcessor.initialise();

    when(mockMuleMessage.getPayload()).thenReturn(of(payload));
    when(mockMuleMessage.getAttributes()).thenReturn(of(new HashMap<>()));
    when(mockExpressionManager.parseLogTemplate(any(), any(), any(), any())).thenReturn("Parsed");

    CoreEvent response = parseTemplateProcessor.process(event);
    assertNotNull(response);
    assertEquals(payload, response.getMessage().getPayload().getValue());
    assertEquals("Parsed",
                 ((Message) response.getVariables().get("some_target_variable").getValue()).getPayload().getValue());
  }
}
