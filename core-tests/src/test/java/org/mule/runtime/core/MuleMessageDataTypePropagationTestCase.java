/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core;

import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.InputStream;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class MuleMessageDataTypePropagationTestCase extends AbstractMuleTestCase {

  public static final Charset DEFAULT_ENCODING = UTF_8;
  public static final Charset CUSTOM_ENCODING = UTF_16;
  public static final String TEST_PROPERTY = "testProperty";
  public static final String TEST = "test";
  public static final MediaType CUSTOM_MIME_TYPE = MediaType.TEXT;
  public static final MediaType APPLICATION_XML_DEFAULT = APPLICATION_XML.withCharset(DEFAULT_ENCODING);
  public static final MediaType APPLICATION_XML_CUSTOM = APPLICATION_XML.withCharset(CUSTOM_ENCODING);
  public static final String CUSTOM_CONTENT_TYPE = CUSTOM_MIME_TYPE + "; charset=" + CUSTOM_ENCODING;

  private MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
  private TransformationService transformationService;

  @Before
  public void setUp() throws Exception {
    when(muleContext.getConfiguration().getDefaultEncoding()).thenReturn(DEFAULT_ENCODING.name());
    when(muleContext.getRegistry().lookupObject(OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE))
        .thenReturn(mock(RetryPolicyTemplate.class));
    transformationService = new TransformationService(muleContext);
  }

  @Test
  public void defaultEmptyEncodingWithNoProperty() throws Exception {
    InternalMessage message = InternalMessage.builder().payload(TEST).build();

    assertEmptyDataType(message);
  }

  @Test
  public void usesCustomEncodingWithNoProperty() throws Exception {
    InternalMessage message = InternalMessage.builder().payload(TEST).mediaType(ANY.withCharset(CUSTOM_ENCODING)).build();

    assertThat(message.getPayload().getDataType().getMediaType().getCharset().get(), equalTo(CUSTOM_ENCODING));
    assertCustomEncoding(message);
  }

  @Test
  public void setsDataTypeFromPreviousMessageOnCreation() throws Exception {
    InternalMessage message = InternalMessage.builder().payload(1).mediaType(APPLICATION_XML_CUSTOM).build();

    assertDataType(InternalMessage.builder(message).build(), message.getPayload().getDataType());
  }

  @Test
  public void overridesDataTypeFromPreviousMessageOnCreation() throws Exception {
    InternalMessage message = InternalMessage.builder().payload(1).mediaType(APPLICATION_XML_CUSTOM).build();

    assertDataType(InternalMessage.builder(message).payload("TEST").build(), String.class, APPLICATION_XML, CUSTOM_ENCODING);
  }

  @Test
  public void updatesDataTypeWhenPayloadIsReplacedWithNullPayload() throws Exception {
    InternalMessage muleMessage = InternalMessage.builder().payload(TEST).build();

    assertDataType(InternalMessage.builder(muleMessage).nullPayload().build(), Object.class, ANY, null);
  }

  @Test
  public void setsNullPayloadWithDataType() throws Exception {
    InternalMessage muleMessage = InternalMessage.builder().payload(TEST).build();

    assertDataType(InternalMessage.builder(muleMessage).nullPayload().mediaType(APPLICATION_XML_CUSTOM).build(), Object.class,
                   APPLICATION_XML, CUSTOM_ENCODING);
  }

  @Test
  public void setsPayloadWithDataType() throws Exception {
    InternalMessage muleMessage = InternalMessage.builder().payload(TEST).build();

    assertDataType(InternalMessage.builder(muleMessage).payload(1).mediaType(APPLICATION_XML_CUSTOM).build(), Integer.class,
                   APPLICATION_XML, CUSTOM_ENCODING);
  }

  @Test
  public void updatesTypeOnTransformation() throws Exception {
    InternalMessage message = InternalMessage.builder().payload(1).mediaType(APPLICATION_XML_DEFAULT).build();

    Transformer transformer = mock(Transformer.class);
    when(transformer.isSourceDataTypeSupported(any())).thenReturn(true);
    DataType outputDataType = DataType.builder().type(Integer.class).mediaType(ANY).charset(DEFAULT_ENCODING).build();
    when(transformer.getReturnDataType()).thenReturn(outputDataType);
    when(transformer.transform(anyObject())).thenReturn(1);

    Event muleEvent = mock(Event.class);

    InternalMessage result = transformationService.applyTransformers(message, muleEvent, singletonList(transformer));

    assertDataType(result, Integer.class, APPLICATION_XML, DEFAULT_ENCODING);
  }

  @Test
  public void updatesEncodingOnTransformation() throws Exception {
    InternalMessage message = InternalMessage.builder().payload(TEST).mediaType(APPLICATION_XML_DEFAULT).build();

    Transformer transformer = mock(Transformer.class);
    when(transformer.isSourceDataTypeSupported(any())).thenReturn(true);
    DataType outputDataType = DataType.builder().type(Integer.class).charset(CUSTOM_ENCODING).build();
    when(transformer.getReturnDataType()).thenReturn(outputDataType);
    when(transformer.transform(anyObject())).thenReturn(Integer.valueOf(1));

    Event muleEvent = mock(Event.class);

    InternalMessage result = transformationService.applyTransformers(message, muleEvent, singletonList(transformer));

    assertDataType(result, Integer.class, APPLICATION_XML, CUSTOM_ENCODING);
  }

  @Test
  public void updatesMimeTypeOnTransformation() throws Exception {
    InternalMessage message = InternalMessage.builder().payload(TEST).mediaType(ANY.withCharset(CUSTOM_ENCODING)).build();

    Transformer transformer = mock(Transformer.class);
    when(transformer.isSourceDataTypeSupported(any())).thenReturn(true);
    DataType outputDataType = DataType.builder().type(Integer.class).mediaType(APPLICATION_XML).build();
    when(transformer.getReturnDataType()).thenReturn(outputDataType);
    when(transformer.transform(anyString())).thenReturn(1);

    Event muleEvent = mock(Event.class);

    InternalMessage result = transformationService.applyTransformers(message, muleEvent, singletonList(transformer));

    assertDataType(result, Integer.class, APPLICATION_XML, CUSTOM_ENCODING);
  }

  @Test
  public void maintainsCurrentDataTypeClassWhenTransformerOutputTypeIsObject() throws Exception {
    InternalMessage message = InternalMessage.builder().payload(TEST).build();

    Transformer transformer = mock(Transformer.class);
    when(transformer.isSourceDataTypeSupported(any())).thenReturn(true);
    DataType outputDataType = DataType.builder().type(Object.class).mediaType(ANY).build();
    when(transformer.getReturnDataType()).thenReturn(outputDataType);
    when(transformer.transform(message)).thenReturn(TEST);

    Event muleEvent = mock(Event.class);

    InternalMessage result = transformationService.applyTransformers(message, muleEvent, singletonList(transformer));

    assertDataType(result, String.class, ANY, DEFAULT_ENCODING);
  }

  @Test
  public void maintainsDataTypeOnGetPayloadTransformation() throws Exception {
    InputStream payload = mock(InputStream.class);
    InternalMessage message = InternalMessage.builder().payload(payload).mediaType(APPLICATION_XML_CUSTOM).build();

    MuleRegistry muleRegistry = mock(MuleRegistry.class);
    when(muleContext.getRegistry()).thenReturn(muleRegistry);
    Transformer transformer = mock(Transformer.class);
    when(transformer.transform(anyObject(), anyObject())).thenReturn(TEST);
    when(muleRegistry.lookupTransformer(any(), any())).thenReturn(transformer);

    assertThat(message.getPayload().getDataType().getMediaType().getPrimaryType(), equalTo(APPLICATION_XML.getPrimaryType()));
    assertThat(message.getPayload().getDataType().getMediaType().getSubType(), equalTo(APPLICATION_XML.getSubType()));
    assertThat(message.getPayload().getDataType().getMediaType().getCharset().get(), equalTo(CUSTOM_ENCODING));
  }

  @Test
  public void setsDefaultOutboundPropertyDataType() throws Exception {
    InternalMessage message = InternalMessage.builder().payload(TEST).addOutboundProperty(TEST_PROPERTY, TEST).build();

    assertDefaultOutboundPropertyDataType(message);
  }

  @Test
  public void setsCustomOutboundPropertyDataType() throws Exception {
    MediaType mediaType = APPLICATION_XML_CUSTOM;

    InternalMessage message = InternalMessage.builder().payload(TEST).addOutboundProperty(TEST_PROPERTY, TEST, mediaType).build();

    assertOutboundPropertyDataType(message, DataType.builder().type(String.class).mediaType(mediaType).build());
  }

  @Test
  public void setsDefaultOutboundScopePropertyDataType() throws Exception {
    InternalMessage message = InternalMessage.builder().payload(TEST).addOutboundProperty(TEST_PROPERTY, TEST).build();

    assertDefaultOutboundPropertyDataType(message);
  }

  @Test
  public void setsDefaultInboundPropertyDataType() throws Exception {
    InternalMessage message = InternalMessage.builder().payload(TEST).addInboundProperty(TEST_PROPERTY, TEST).build();

    assertDefaultInboundPropertyDataType(message);
  }

  @Test
  public void setsCustomInboundPropertyDataType() throws Exception {
    MediaType mediaType = APPLICATION_XML_CUSTOM;

    InternalMessage message = InternalMessage.builder().payload(TEST).addInboundProperty(TEST_PROPERTY, TEST, mediaType).build();
    assertInboundPropertyDataType(message, DataType.builder().type(String.class).mediaType(mediaType).build());
  }

  // TODO

  @Test
  public void setsDefaultInboundScopePropertyDataType() throws Exception {
    InternalMessage message = InternalMessage.builder().payload(TEST).addInboundProperty(TEST_PROPERTY, TEST).build();

    assertDefaultInboundPropertyDataType(message);
  }

  @Test
  public void setsDefaultFlowVariableDataType() throws Exception {
    Event muleEvent = Event.builder(DefaultEventContext.create(getTestFlow(muleContext), TEST_CONNECTOR))
        .message(InternalMessage.of(TEST))
        .build();
    muleEvent = Event.builder(muleEvent).addVariable(TEST_PROPERTY, TEST).build();

    assertVariableDataType(muleEvent, STRING);
  }

  @Test
  public void setsCustomFlowVariableDataType() throws Exception {
    Event muleEvent = Event.builder(DefaultEventContext.create(getTestFlow(muleContext), TEST_CONNECTOR))
        .message(InternalMessage.of(TEST))
        .build();
    DataType dataType = DataType.builder().type(String.class).mediaType(APPLICATION_XML).charset(CUSTOM_ENCODING).build();

    muleEvent = Event.builder(muleEvent).addVariable(TEST_PROPERTY, TEST, dataType).build();

    assertVariableDataType(muleEvent, dataType);
  }

  @Test
  public void setsDefaultSessionVariableDataType() throws Exception {
    Event muleEvent = Event.builder(DefaultEventContext.create(getTestFlow(muleContext), TEST_CONNECTOR))
        .message(InternalMessage.of(TEST))
        .build();
    muleEvent.getSession().setProperty(TEST_PROPERTY, TEST);

    assertSessionVariableDataType(muleEvent, STRING);
  }

  @Test
  public void setsCustomSessionVariableDataType() throws Exception {
    Event muleEvent = Event.builder(DefaultEventContext.create(getTestFlow(muleContext), TEST_CONNECTOR))
        .message(InternalMessage.of(TEST))
        .build();
    DataType dataType = DataType.builder().type(String.class).mediaType(APPLICATION_XML).charset(CUSTOM_ENCODING).build();

    muleEvent.getSession().setProperty(TEST_PROPERTY, TEST, dataType);

    assertSessionVariableDataType(muleEvent, dataType);
  }

  @Test
  public void setsCustomPropertyDataType() throws Exception {
    MediaType mediaType = APPLICATION_XML_CUSTOM;

    InternalMessage message = InternalMessage.builder().payload(TEST).addOutboundProperty(TEST_PROPERTY, TEST, mediaType).build();

    assertOutboundPropertyDataType(message, DataType.builder(STRING).mediaType(mediaType).build());
  }

  protected DataType buildDefaultEncodingDataType() {
    return DataType.builder(OBJECT).charset(getDefaultEncoding(muleContext)).build();
  }

  private void assertEmptyDataType(InternalMessage muleMessage) {
    assertThat(muleMessage.getPayload().getDataType().getMediaType().getCharset().isPresent(), is(false));
  }

  private void assertCustomEncoding(InternalMessage muleMessage) {
    assertThat(muleMessage.getPayload().getDataType().getMediaType().getCharset().get(), is(CUSTOM_ENCODING));
  }

  private void assertDataType(InternalMessage muleMessage, Class<?> type, MediaType mimeType, Charset encoding) {
    assertThat(muleMessage.getPayload().getDataType(), like(type, mimeType, encoding));
  }

  private void assertDataType(InternalMessage muleMessage, DataType dataType) {
    assertThat(muleMessage.getPayload().getDataType(), like(dataType));
  }

  private void assertDefaultInboundPropertyDataType(InternalMessage muleMessage) {
    assertInboundPropertyDataType(muleMessage, STRING);
  }

  private void assertDefaultOutboundPropertyDataType(InternalMessage muleMessage) {
    assertOutboundPropertyDataType(muleMessage, STRING);
  }

  private void assertInboundPropertyDataType(InternalMessage muleMessage, DataType dataType) {
    DataType actualDataType = muleMessage.getInboundPropertyDataType(TEST_PROPERTY);
    assertThat(actualDataType, like(dataType));
  }

  private void assertOutboundPropertyDataType(InternalMessage muleMessage, DataType dataType) {
    DataType actualDataType = muleMessage.getOutboundPropertyDataType(TEST_PROPERTY);
    assertThat(actualDataType, like(dataType));
  }

  private void assertVariableDataType(Event event, DataType dataType) {
    DataType actualDataType = event.getVariable(TEST_PROPERTY).getDataType();
    assertThat(actualDataType, like(dataType));
  }

  private void assertSessionVariableDataType(Event event, DataType dataType) {
    DataType actualDataType = event.getSession().getPropertyDataType(TEST_PROPERTY);
    assertThat(actualDataType, like(dataType));
  }
}
