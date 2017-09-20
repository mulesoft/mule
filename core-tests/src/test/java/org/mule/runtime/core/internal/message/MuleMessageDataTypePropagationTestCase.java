/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.message;

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
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import static org.mule.tck.util.MuleContextUtils.registerIntoMockContext;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.privileged.transformer.ExtendedTransformationService;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.Charset;

@SmallTest
public class MuleMessageDataTypePropagationTestCase extends AbstractMuleTestCase {

  public static final Charset DEFAULT_ENCODING = UTF_8;
  public static final Charset CUSTOM_ENCODING = UTF_16;
  public static final String TEST_PROPERTY = "testProperty";
  public static final MediaType CUSTOM_MIME_TYPE = MediaType.TEXT;
  public static final MediaType APPLICATION_XML_DEFAULT = APPLICATION_XML.withCharset(DEFAULT_ENCODING);
  public static final MediaType APPLICATION_XML_CUSTOM = APPLICATION_XML.withCharset(CUSTOM_ENCODING);
  public static final String CUSTOM_CONTENT_TYPE = CUSTOM_MIME_TYPE + "; charset=" + CUSTOM_ENCODING;

  private MuleContextWithRegistries muleContext = mock(MuleContextWithRegistries.class, RETURNS_DEEP_STUBS);
  private ExtendedTransformationService transformationService;

  @Before
  public void setUp() throws Exception {
    when(muleContext.getConfiguration().getDefaultEncoding()).thenReturn(DEFAULT_ENCODING.name());
    registerIntoMockContext(muleContext, OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE, mock(RetryPolicyTemplate.class));
    transformationService = new ExtendedTransformationService(muleContext);
  }

  @Test
  public void defaultEmptyEncodingWithNoProperty() throws Exception {
    Message message = of(TEST_PAYLOAD);

    assertEmptyDataType(message);
  }

  @Test
  public void usesCustomEncodingWithNoProperty() throws Exception {
    Message message = Message.builder().value(TEST_PAYLOAD).mediaType(ANY.withCharset(CUSTOM_ENCODING)).build();

    assertThat(message.getPayload().getDataType().getMediaType().getCharset().get(), equalTo(CUSTOM_ENCODING));
    assertCustomEncoding(message);
  }

  @Test
  public void setsDataTypeFromPreviousMessageOnCreation() throws Exception {
    Message message = Message.builder().value(1).mediaType(APPLICATION_XML_CUSTOM).build();

    assertDataType(InternalMessage.builder(message).build(), message.getPayload().getDataType());
  }

  @Test
  public void overridesDataTypeFromPreviousMessageOnCreation() throws Exception {
    Message message = Message.builder().value(1).mediaType(APPLICATION_XML_CUSTOM).build();

    assertDataType(InternalMessage.builder(message).value("TEST").build(), String.class, APPLICATION_XML, CUSTOM_ENCODING);
  }

  @Test
  public void updatesDataTypeWhenPayloadIsReplacedWithNullPayload() throws Exception {
    Message muleMessage = of(TEST_PAYLOAD);

    assertDataType(InternalMessage.builder(muleMessage).nullValue().build(), Object.class, ANY, null);
  }

  @Test
  public void setsNullPayloadWithDataType() throws Exception {
    Message muleMessage = of(TEST_PAYLOAD);

    assertDataType(InternalMessage.builder(muleMessage).nullValue().mediaType(APPLICATION_XML_CUSTOM).build(), Object.class,
                   APPLICATION_XML, CUSTOM_ENCODING);
  }

  @Test
  public void setsPayloadWithDataType() throws Exception {
    Message muleMessage = of(TEST_PAYLOAD);

    assertDataType(InternalMessage.builder(muleMessage).value(1).mediaType(APPLICATION_XML_CUSTOM).build(), Integer.class,
                   APPLICATION_XML, CUSTOM_ENCODING);
  }

  @Test
  public void updatesTypeOnTransformation() throws Exception {
    Message message = Message.builder().value(1).mediaType(APPLICATION_XML_DEFAULT).build();

    Transformer transformer = mock(Transformer.class);
    when(transformer.isSourceDataTypeSupported(any())).thenReturn(true);
    DataType outputDataType = DataType.builder().type(Integer.class).mediaType(ANY).charset(DEFAULT_ENCODING).build();
    when(transformer.getReturnDataType()).thenReturn(outputDataType);
    when(transformer.transform(anyObject())).thenReturn(1);

    CoreEvent muleEvent = mock(CoreEvent.class);

    Message result = transformationService.applyTransformers(message, muleEvent, singletonList(transformer));

    assertDataType(result, Integer.class, APPLICATION_XML, DEFAULT_ENCODING);
  }

  @Test
  public void updatesEncodingOnTransformation() throws Exception {
    Message message = Message.builder().value(TEST_PAYLOAD).mediaType(APPLICATION_XML_DEFAULT).build();

    Transformer transformer = mock(Transformer.class);
    when(transformer.isSourceDataTypeSupported(any())).thenReturn(true);
    DataType outputDataType = DataType.builder().type(Integer.class).charset(CUSTOM_ENCODING).build();
    when(transformer.getReturnDataType()).thenReturn(outputDataType);
    when(transformer.transform(anyObject())).thenReturn(Integer.valueOf(1));

    CoreEvent muleEvent = mock(CoreEvent.class);

    Message result = transformationService.applyTransformers(message, muleEvent, singletonList(transformer));

    assertDataType(result, Integer.class, APPLICATION_XML, CUSTOM_ENCODING);
  }

  @Test
  public void updatesMimeTypeOnTransformation() throws Exception {
    Message message = Message.builder().value(TEST_PAYLOAD).mediaType(ANY.withCharset(CUSTOM_ENCODING)).build();

    Transformer transformer = mock(Transformer.class);
    when(transformer.isSourceDataTypeSupported(any())).thenReturn(true);
    DataType outputDataType = DataType.builder().type(Integer.class).mediaType(APPLICATION_XML).build();
    when(transformer.getReturnDataType()).thenReturn(outputDataType);
    when(transformer.transform(anyString())).thenReturn(1);

    CoreEvent muleEvent = mock(CoreEvent.class);

    Message result = transformationService.applyTransformers(message, muleEvent, singletonList(transformer));

    assertDataType(result, Integer.class, APPLICATION_XML, CUSTOM_ENCODING);
  }

  @Test
  public void maintainsCurrentDataTypeClassWhenTransformerOutputTypeIsObject() throws Exception {
    Message message = of(TEST_PAYLOAD);

    Transformer transformer = mock(Transformer.class);
    when(transformer.isSourceDataTypeSupported(any())).thenReturn(true);
    DataType outputDataType = DataType.builder().type(Object.class).mediaType(ANY).build();
    when(transformer.getReturnDataType()).thenReturn(outputDataType);
    when(transformer.transform(message)).thenReturn(TEST_PAYLOAD);

    CoreEvent muleEvent = mock(CoreEvent.class);

    Message result = transformationService.applyTransformers(message, muleEvent, singletonList(transformer));

    assertDataType(result, String.class, ANY, DEFAULT_ENCODING);
  }

  @Test
  public void maintainsDataTypeOnGetPayloadTransformation() throws Exception {
    InputStream payload = mock(InputStream.class);
    Message message = Message.builder().value(payload).mediaType(APPLICATION_XML_CUSTOM).build();

    MuleRegistry muleRegistry = mock(MuleRegistry.class);
    when(muleContext.getRegistry()).thenReturn(muleRegistry);
    Transformer transformer = mock(Transformer.class);
    when(transformer.transform(anyObject(), anyObject())).thenReturn(TEST_PAYLOAD);
    when(muleRegistry.lookupTransformer(any(), any())).thenReturn(transformer);

    assertThat(message.getPayload().getDataType().getMediaType().getPrimaryType(), equalTo(APPLICATION_XML.getPrimaryType()));
    assertThat(message.getPayload().getDataType().getMediaType().getSubType(), equalTo(APPLICATION_XML.getSubType()));
    assertThat(message.getPayload().getDataType().getMediaType().getCharset().get(), equalTo(CUSTOM_ENCODING));
  }

  @Test
  public void setsDefaultOutboundPropertyDataType() throws Exception {
    InternalMessage message =
        InternalMessage.builder().value(TEST_PAYLOAD).addOutboundProperty(TEST_PROPERTY, TEST_PAYLOAD)
            .build();

    assertDefaultOutboundPropertyDataType(message);
  }

  @Test
  public void setsCustomOutboundPropertyDataType() throws Exception {
    MediaType mediaType = APPLICATION_XML_CUSTOM;
    InternalMessage message =
        InternalMessage.builder().value(TEST_PAYLOAD)
            .addOutboundProperty(TEST_PROPERTY, TEST_PAYLOAD, mediaType).build();

    assertOutboundPropertyDataType(message, DataType.builder().type(String.class).mediaType(mediaType).build());
  }

  @Test
  public void setsDefaultOutboundScopePropertyDataType() throws Exception {
    InternalMessage message =
        InternalMessage.builder().value(TEST_PAYLOAD).addOutboundProperty(TEST_PROPERTY, TEST_PAYLOAD)
            .build();

    assertDefaultOutboundPropertyDataType(message);
  }

  @Test
  public void setsDefaultInboundPropertyDataType() throws Exception {
    InternalMessage message =
        InternalMessage.builder().value(TEST_PAYLOAD).addInboundProperty(TEST_PROPERTY, TEST_PAYLOAD).build();

    assertDefaultInboundPropertyDataType(message);
  }

  @Test
  public void setsCustomInboundPropertyDataType() throws Exception {
    MediaType mediaType = APPLICATION_XML_CUSTOM;
    InternalMessage message =
        InternalMessage.builder().value(TEST_PAYLOAD)
            .addInboundProperty(TEST_PROPERTY, TEST_PAYLOAD, mediaType).build();
    assertInboundPropertyDataType(message, DataType.builder().type(String.class).mediaType(mediaType).build());
  }

  // TODO

  @Test
  public void setsDefaultInboundScopePropertyDataType() throws Exception {
    InternalMessage message =
        InternalMessage.builder().value(TEST_PAYLOAD).addInboundProperty(TEST_PROPERTY, TEST_PAYLOAD).build();

    assertDefaultInboundPropertyDataType(message);
  }

  @Test
  public void setsDefaultFlowVariableDataType() throws Exception {
    CoreEvent muleEvent = CoreEvent.builder(testEvent()).addVariable(TEST_PROPERTY, TEST_PAYLOAD).build();

    assertVariableDataType(muleEvent, STRING);
  }

  @Test
  public void setsCustomFlowVariableDataType() throws Exception {
    DataType dataType = DataType.builder().type(String.class).mediaType(APPLICATION_XML).charset(CUSTOM_ENCODING).build();

    CoreEvent muleEvent = CoreEvent.builder(testEvent()).addVariable(TEST_PROPERTY, TEST_PAYLOAD, dataType).build();

    assertVariableDataType(muleEvent, dataType);
  }

  @Test
  public void setsDefaultSessionVariableDataType() throws Exception {
    ((PrivilegedEvent) testEvent()).getSession().setProperty(TEST_PROPERTY, TEST_PAYLOAD);

    assertSessionVariableDataType(testEvent(), STRING);
  }

  @Test
  public void setsCustomSessionVariableDataType() throws Exception {
    DataType dataType = DataType.builder().type(String.class).mediaType(APPLICATION_XML).charset(CUSTOM_ENCODING).build();

    ((PrivilegedEvent) testEvent()).getSession().setProperty(TEST_PROPERTY, TEST_PAYLOAD, dataType);

    assertSessionVariableDataType(testEvent(), dataType);
  }

  @Test
  public void setsCustomPropertyDataType() throws Exception {
    MediaType mediaType = APPLICATION_XML_CUSTOM;
    InternalMessage message =
        InternalMessage.builder().value(TEST_PAYLOAD)
            .addOutboundProperty(TEST_PROPERTY, TEST_PAYLOAD, mediaType).build();

    assertOutboundPropertyDataType(message, DataType.builder(STRING).mediaType(mediaType).build());
  }

  private void assertEmptyDataType(Message muleMessage) {
    assertThat(muleMessage.getPayload().getDataType().getMediaType().getCharset().isPresent(), is(false));
  }

  private void assertCustomEncoding(Message muleMessage) {
    assertThat(muleMessage.getPayload().getDataType().getMediaType().getCharset().get(), is(CUSTOM_ENCODING));
  }

  private void assertDataType(Message muleMessage, Class<?> type, MediaType mimeType, Charset encoding) {
    assertThat(muleMessage.getPayload().getDataType(), like(type, mimeType, encoding));
  }

  private void assertDataType(Message muleMessage, DataType dataType) {
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

  private void assertVariableDataType(CoreEvent event, DataType dataType) {
    DataType actualDataType = event.getVariables().get(TEST_PROPERTY).getDataType();
    assertThat(actualDataType, like(dataType));
  }

  private void assertSessionVariableDataType(CoreEvent event, DataType dataType) {
    DataType actualDataType = ((PrivilegedEvent) event).getSession().getPropertyDataType(TEST_PROPERTY);
    assertThat(actualDataType, like(dataType));
  }
}
