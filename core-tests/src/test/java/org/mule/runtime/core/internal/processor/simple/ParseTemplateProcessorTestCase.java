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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.create;
import static org.mule.runtime.api.metadata.TypedValue.of;
import static org.mule.runtime.core.api.util.FileUtils.newFile;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsUrl;
import static org.mule.runtime.core.internal.util.TestFileUtils.isFileOpen;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.FileUtils;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.internal.util.TestFileUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.UnsupportedCharsetException;
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

  @Test
  public void parseTemplateNullLocation() throws InitialisationException {
    parseTemplateProcessor.setLocation(null);
    expectedException.expect(InitialisationException.class);
    expectedException.expectMessage("should be defined but they are both null");
    parseTemplateProcessor.initialise();
  }

  @Test
  public void parseTemplateInvalidLocation() throws InitialisationException {
    parseTemplateProcessor.setLocation(INVALID_LOCATION);
    addMockComponentLocation(parseTemplateProcessor);
    expectedException.expect(InitialisationException.class);
    expectedException.expectMessage("not found");
    parseTemplateProcessor.initialise();
  }

  @Test
  public void parseTemplateWithBothLocationAndContentDefined() throws InitialisationException {
    parseTemplateProcessor.setLocation(LOCATION);
    parseTemplateProcessor.setContent("SOME CONTENT");
    addMockComponentLocation(parseTemplateProcessor);
    expectedException.expect(InitialisationException.class);
    expectedException.expectMessage("both location and content at the same time");
    parseTemplateProcessor.initialise();
  }

  @Test
  public void parseTemplateNullContent() throws InitialisationException {
    expectedException.expect(InitialisationException.class);
    expectedException.expectMessage("should be defined but they are both null");
    parseTemplateProcessor.initialise();
  }

  @Test
  public void parseTemplateFromLocation() throws InitialisationException, IOException {
    parseTemplateProcessor.setLocation(LOCATION);
    parseTemplateProcessor.initialise();
    when(mockMuleMessage.getInboundProperty("errorMessage")).thenReturn("ERROR!!!");
    String expectedExpression = getResourceAsString(LOCATION, this.getClass());

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
    String expectedExpression = getResourceAsString(LOCATION, this.getClass());
    when(mockExpressionManager.parseLogTemplate(eq(expectedExpression), eq(event), any(), any())).thenReturn("Parsed");
    CoreEvent response = parseTemplateProcessor.process(event);
    assertThat(response.getMessage().getPayload().getDataType().getMediaType(), is(equalTo(LOCATION_MEDIA_TYPE)));
  }

  @Test
  public void parseTemplateFromLocationWithUnknownMediaType() throws Exception {
    parseTemplateProcessor.setLocation(UNKNOWN_MEDIATYPE_LOCATION);
    parseTemplateProcessor.initialise();
    String expectedExpression = getResourceAsString(UNKNOWN_MEDIATYPE_LOCATION, this.getClass());
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
    String expectedExpression = getResourceAsString(UNKNOWN_MEDIATYPE_LOCATION, this.getClass());
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
    expectedException.expect(UnsupportedCharsetException.class);
    expectedException.expectMessage("invalidEncoding");
    parseTemplateProcessor.setOutputEncoding("invalidEncoding");
  }

  @Test
  public void invalidMimeTypeStringThrowsException() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("MediaType cannot be parsed");
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
    String expectedExpression = getResourceAsString(UNKNOWN_MEDIATYPE_LOCATION, this.getClass());
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
  public void parseTemplateDoesNotLeakTheLocationFile() throws Exception {
    String customEncoding = "UTF-16";
    MediaType customMediaType = create("application", "lrmextension");
    parseTemplateProcessor.setLocation(UNKNOWN_MEDIATYPE_LOCATION);
    parseTemplateProcessor.setOutputMimeType(customMediaType.toRfcString());
    parseTemplateProcessor.setOutputEncoding(customEncoding);
    parseTemplateProcessor.initialise();

    String expectedExpression = getResourceAsString(UNKNOWN_MEDIATYPE_LOCATION, this.getClass());
    when(mockExpressionManager.parseLogTemplate(eq(expectedExpression), eq(event), any(), any())).thenReturn("Parsed");
    parseTemplateProcessor.process(event);

    URL url = getResourceAsUrl(UNKNOWN_MEDIATYPE_LOCATION, getClass());
    assertThat(isFileOpen(newFile(url.toURI())), is(false));
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
