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
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import static org.mule.runtime.core.api.config.MuleProperties.CONTENT_TYPE_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_ENCODING_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;
import static org.mule.tck.MuleTestUtils.getTestEvent;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;

import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
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
public class MuleMessageDataTypePropagationTestCase extends AbstractMuleTestCase
{

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
    public void setUp() throws Exception
    {
        when(muleContext.getConfiguration().getDefaultEncoding()).thenReturn(DEFAULT_ENCODING.name());
        when(muleContext.getRegistry().lookupObject(OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE)).thenReturn(mock(RetryPolicyTemplate.class));
        transformationService = new TransformationService(muleContext);
    }

    @Test
    public void defaultEmptyEncodingWithNoProperty() throws Exception
    {
        MuleMessage message = MuleMessage.builder().payload(TEST).build();

        assertEmptyDataType(message);
        assertThat(message.getOutboundPropertyNames(), not(hasItem(MULE_ENCODING_PROPERTY)));
    }

    @Test
    public void usesCustomEncodingWithNoProperty() throws Exception
    {
        MuleMessage message = MuleMessage.builder().payload(TEST).mediaType(ANY.withCharset(CUSTOM_ENCODING)).build();

        assertThat(message.getDataType().getMediaType().getCharset().get(), equalTo(CUSTOM_ENCODING));
        assertThat(message.getOutboundPropertyNames(), not(hasItem(MULE_ENCODING_PROPERTY)));
    }

    @Test
    public void doesNotUseEncodingFromInboundProperty() throws Exception
    {
        MuleMessage message = MuleMessage.builder().payload(TEST).addInboundProperty(MULE_ENCODING_PROPERTY,
                                                                                     CUSTOM_ENCODING.name()).build();
        assertCustomEncoding(message);
    }

    @Test
    public void doesNotUseEncodingFromOutboundProperty() throws Exception
    {
        MuleMessage muleMessage = MuleMessage.builder().payload(TEST).addOutboundProperty(MULE_ENCODING_PROPERTY,
                                                                                          CUSTOM_ENCODING.name())
                .build();
        assertCustomEncoding(muleMessage);
    }

    @Test
    public void doesNotUseContentTyperomInboundProperty() throws Exception
    {
        MuleMessage message = MuleMessage.builder().payload(TEST).addInboundProperty(CONTENT_TYPE_PROPERTY,
                STRING.getMediaType().toString()).build();
        assertEmptyDataType(message);
    }

    @Test
    public void doesNotUseContentTypeFromOutboundProperty() throws Exception
    {
        MuleMessage muleMessage = MuleMessage.builder().payload(TEST).addOutboundProperty(CONTENT_TYPE_PROPERTY,
                STRING.getMediaType().toString()).build();
        assertEmptyDataType(muleMessage);
    }

    @Test
    public void setsDataTypeFromPreviousMessageOnCreation() throws Exception
    {
        MuleMessage message = MuleMessage.builder().payload(1).mediaType(APPLICATION_XML_CUSTOM).build();

        assertDataType(MuleMessage.builder(message).build(), message.getDataType());
    }

    @Test
    public void overridesDataTypeFromPreviousMessageOnCreation() throws Exception
    {
        MuleMessage message = MuleMessage.builder().payload(1).mediaType(APPLICATION_XML_CUSTOM).build();

        assertDataType(MuleMessage.builder(message).payload("TEST").build(), String.class, APPLICATION_XML,
                       CUSTOM_ENCODING);
    }

    @Test
    public void updatesDataTypeWhenPayloadIsReplacedWithNullPayload() throws Exception
    {
        MuleMessage muleMessage = MuleMessage.builder().payload(TEST).build();

        assertDataType(MuleMessage.builder(muleMessage).payload(NullPayload.getInstance()).build(), Object.class, ANY, null);
    }

    @Test
    public void setsNullPayloadWithDataType() throws Exception
    {
        MuleMessage muleMessage = MuleMessage.builder().payload(TEST).build();

        assertDataType(MuleMessage.builder(muleMessage).payload(NullPayload.getInstance()).mediaType
                (APPLICATION_XML_CUSTOM).build(), Object.class, APPLICATION_XML, CUSTOM_ENCODING);
    }

    @Test
    public void setsPayloadWithDataType() throws Exception
    {
        MuleMessage muleMessage = MuleMessage.builder().payload(TEST).build();

        assertDataType(MuleMessage.builder(muleMessage).payload(1).mediaType(APPLICATION_XML_CUSTOM).build(), Integer
                .class, APPLICATION_XML, CUSTOM_ENCODING);
    }

    @Test
    public void updatesTypeOnTransformation() throws Exception
    {
        MuleMessage message = MuleMessage.builder().payload(1).mediaType(APPLICATION_XML_DEFAULT).build();

        Transformer transformer = mock(Transformer.class);
        when(transformer.isSourceDataTypeSupported(org.mockito.Matchers.<DataType<?>>any())).thenReturn(true);
        DataType outputDataType = DataType.builder().type(Integer.class).mediaType(ANY).charset(DEFAULT_ENCODING).build();
        when(transformer.getReturnDataType()).thenReturn(outputDataType);
        when(transformer.transform(anyObject())).thenReturn(1);

        MuleEvent muleEvent = mock(MuleEvent.class);

        MuleMessage result = transformationService.applyTransformers(message, muleEvent,
                                                                                   singletonList(transformer));

        assertDataType(result, Integer.class, APPLICATION_XML, DEFAULT_ENCODING);
    }

    @Test
    public void updatesEncodingOnTransformation() throws Exception
    {
        MuleMessage message = MuleMessage.builder().payload(TEST).mediaType(APPLICATION_XML_DEFAULT).build();

        Transformer transformer = mock(Transformer.class);
        when(transformer.isSourceDataTypeSupported(org.mockito.Matchers.<DataType<?>>any())).thenReturn(true);
        DataType outputDataType = DataType.builder().type(Integer.class).charset(CUSTOM_ENCODING).build();
        when(transformer.getReturnDataType()).thenReturn(outputDataType);
        when(transformer.transform(anyObject())).thenReturn(Integer.valueOf(1));

        MuleEvent muleEvent = mock(MuleEvent.class);

        MuleMessage result = transformationService.applyTransformers(message, muleEvent,
                                                                                   singletonList(transformer));

        assertDataType(result, Integer.class, APPLICATION_XML, CUSTOM_ENCODING);
    }

    @Test
    public void updatesMimeTypeOnTransformation() throws Exception
    {
        MuleMessage message = MuleMessage.builder().payload(TEST).mediaType(ANY.withCharset(CUSTOM_ENCODING)).build();

        Transformer transformer = mock(Transformer.class);
        when(transformer.isSourceDataTypeSupported(org.mockito.Matchers.<DataType<?>>any())).thenReturn(true);
        DataType outputDataType = DataType.builder().type(Integer.class).mediaType(APPLICATION_XML).build();
        when(transformer.getReturnDataType()).thenReturn(outputDataType);
        when(transformer.transform(anyString())).thenReturn(1);

        MuleEvent muleEvent = mock(MuleEvent.class);

        MuleMessage result = transformationService.applyTransformers(message, muleEvent, singletonList(transformer));

        assertDataType(result, Integer.class, APPLICATION_XML, CUSTOM_ENCODING);
    }

    @Test
    public void maintainsCurrentDataTypeClassWhenTransformerOutputTypeIsObject() throws Exception
    {
        MuleMessage message = MuleMessage.builder().payload(TEST).build();

        Transformer transformer = mock(Transformer.class);
        when(transformer.isSourceDataTypeSupported(org.mockito.Matchers.<DataType<?>>any())).thenReturn(true);
        DataType outputDataType = DataType.builder().type(Object.class).mediaType(ANY).build();
        when(transformer.getReturnDataType()).thenReturn(outputDataType);
        when(transformer.transform(message)).thenReturn(TEST);

        MuleEvent muleEvent = mock(MuleEvent.class);

        MuleMessage result = transformationService.applyTransformers(message, muleEvent, singletonList(transformer));

        assertDataType(result, String.class, ANY, DEFAULT_ENCODING);
    }

    @Test
    public void maintainsDataTypeOnGetPayloadTransformation() throws Exception
    {
        InputStream payload = mock(InputStream.class);
        MuleMessage message = MuleMessage.builder().payload(payload).mediaType(APPLICATION_XML_CUSTOM).build();

        MuleRegistry muleRegistry = mock(MuleRegistry.class);
        when(muleContext.getRegistry()).thenReturn(muleRegistry);
        Transformer transformer = mock(Transformer.class);
        when(transformer.transform(anyObject(), anyObject())).thenReturn(TEST);
        when(muleRegistry.lookupTransformer(argThat(any(DataType.class)), argThat(any(DataType.class)))).thenReturn(transformer);

        assertThat(message.getDataType().getMediaType().getPrimaryType(), equalTo(APPLICATION_XML.getPrimaryType()));
        assertThat(message.getDataType().getMediaType().getSubType(), equalTo(APPLICATION_XML.getSubType()));
        assertThat(message.getDataType().getMediaType().getCharset().get(), equalTo(CUSTOM_ENCODING));
    }

    @Test
    public void setsDefaultOutboundPropertyDataType() throws Exception
    {
        MuleMessage message = MuleMessage.builder().payload(TEST).addOutboundProperty(TEST_PROPERTY, TEST).build();

        assertDefaultOutboundPropertyDataType(message);
    }

    @Test
    public void setsCustomOutboundPropertyDataType() throws Exception
    {
        MediaType mediaType = APPLICATION_XML_CUSTOM;

        MuleMessage message = MuleMessage.builder().payload(TEST).addOutboundProperty(TEST_PROPERTY, TEST, mediaType)
                .build();

        assertOutboundPropertyDataType(message, DataType.builder().type(String.class).mediaType(mediaType).build());
    }

    @Test
    public void setsDefaultOutboundScopePropertyDataType() throws Exception
    {
        MuleMessage message = MuleMessage.builder().payload(TEST).addOutboundProperty(TEST_PROPERTY, TEST).build();

        assertDefaultOutboundPropertyDataType(message);
    }

    @Test
    public void setsDefaultInboundPropertyDataType() throws Exception
    {
        MuleMessage message = MuleMessage.builder().payload(TEST).addInboundProperty(TEST_PROPERTY, TEST).build();

        assertDefaultInboundPropertyDataType(message);
    }

    @Test
    public void setsCustomInboundPropertyDataType() throws Exception
    {
        MediaType mediaType = APPLICATION_XML_CUSTOM;

        MuleMessage message = MuleMessage.builder().payload(TEST).addInboundProperty(TEST_PROPERTY, TEST, mediaType)
                .build();
        assertInboundPropertyDataType(message, DataType.builder().type(String.class).mediaType(mediaType).build());
    }

    // TODO

    @Test
    public void setsDefaultInboundScopePropertyDataType() throws Exception
    {
        MuleMessage message = MuleMessage.builder().payload(TEST).addInboundProperty(TEST_PROPERTY, TEST).build();

        assertDefaultInboundPropertyDataType(message);
    }

    @Test
    public void setsDefaultFlowVariableDataType() throws Exception
    {
        MuleEvent muleEvent = getTestEvent(TEST, muleContext);
        muleEvent.setFlowVariable(TEST_PROPERTY, TEST);

        assertVariableDataType(muleEvent, STRING);
    }

    @Test
    public void setsCustomFlowVariableDataType() throws Exception
    {
        MuleEvent muleEvent = getTestEvent(TEST, muleContext);
        DataType<String> dataType = DataType.builder().type(String.class).mediaType(APPLICATION_XML).charset(CUSTOM_ENCODING).build();

        muleEvent.setFlowVariable(TEST_PROPERTY, TEST, dataType);

        assertVariableDataType(muleEvent, dataType);
    }

    @Test
    public void setsDefaultSessionVariableDataType() throws Exception
    {
        MuleEvent muleEvent = getTestEvent(TEST, muleContext);
        muleEvent.getSession().setProperty(TEST_PROPERTY, TEST);

        assertSessionVariableDataType(muleEvent, STRING);
    }

    @Test
    public void setsCustomSessionVariableDataType() throws Exception
    {
        MuleEvent muleEvent = getTestEvent(TEST, muleContext);
        DataType<String> dataType = DataType.builder().type(String.class).mediaType(APPLICATION_XML).charset(CUSTOM_ENCODING).build();

        muleEvent.getSession().setProperty(TEST_PROPERTY, TEST, dataType);

        assertSessionVariableDataType(muleEvent, dataType);
    }

    @Test
    public void setsCustomPropertyDataType() throws Exception
    {
        MediaType mediaType = APPLICATION_XML_CUSTOM;

        MuleMessage message = MuleMessage.builder().payload(TEST).addOutboundProperty(TEST_PROPERTY, TEST, mediaType)
                .build();

        assertOutboundPropertyDataType(message, DataType.builder(STRING).mediaType(mediaType).build());
    }

    protected DataType<Object> buildDefaultEncodingDataType()
    {
        return DataType.builder(OBJECT).charset(getDefaultEncoding(muleContext)).build();
    }

    private void assertEmptyDataType(MuleMessage muleMessage)
    {
        assertThat(muleMessage.getDataType().getMediaType().getCharset().isPresent(), is(false));
    }

    private void assertCustomEncoding(MuleMessage muleMessage)
    {
        assertThat(muleMessage.getDataType().getMediaType().getCharset().get(), is(CUSTOM_ENCODING));
    }

    private void assertDataType(MuleMessage muleMessage, Class type, MediaType mimeType, Charset encoding)
    {
        assertThat(muleMessage.getDataType(), like(type, mimeType, encoding));
    }

    private void assertDataType(MuleMessage muleMessage, DataType<?> dataType)
    {
        assertThat(muleMessage.getDataType(), like(dataType));
    }

    private void assertDefaultInboundPropertyDataType(MuleMessage muleMessage)
    {
        assertInboundPropertyDataType(muleMessage, STRING);
    }

    private void assertDefaultOutboundPropertyDataType(MuleMessage muleMessage)
    {
        assertOutboundPropertyDataType(muleMessage, STRING);
    }

    private void assertInboundPropertyDataType(MuleMessage muleMessage, DataType dataType)
    {
        DataType<?> actualDataType = muleMessage.getInboundPropertyDataType(TEST_PROPERTY);
        assertThat(actualDataType, like(dataType));
    }

    private void assertOutboundPropertyDataType(MuleMessage muleMessage, DataType dataType)
    {
        DataType<?> actualDataType = muleMessage.getOutboundPropertyDataType(TEST_PROPERTY);
        assertThat(actualDataType, like(dataType));
    }

    private void assertVariableDataType(MuleEvent event, DataType dataType)
    {
        DataType<?> actualDataType = event.getFlowVariableDataType(TEST_PROPERTY);
        assertThat(actualDataType, like(dataType));
    }

    private void assertSessionVariableDataType(MuleEvent event, DataType dataType)
    {
        DataType<?> actualDataType = event.getSession().getPropertyDataType(TEST_PROPERTY);
        assertThat(actualDataType, like(dataType));
    }
}
