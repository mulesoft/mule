/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.DataType.ANY_MIME_TYPE;
import static org.mule.runtime.api.metadata.DataType.STRING_DATA_TYPE;
import static org.mule.runtime.core.api.config.MuleProperties.CONTENT_TYPE_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_ENCODING_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE;
import static org.mule.runtime.core.transformer.types.MimeTypes.ANY;
import static org.mule.runtime.core.transformer.types.MimeTypes.APPLICATION_XML;
import static org.mule.tck.MuleTestUtils.getTestEvent;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;

import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MutableMuleMessage;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.transformer.types.DataTypeFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.InputStream;
import java.util.Collections;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

@SmallTest
public class MuleMessageDataTypePropagationTestCase extends AbstractMuleTestCase
{

    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final String CUSTOM_ENCODING = "UTF-16";
    public static final String TEST_PROPERTY = "testProperty";
    public static final String TEST = "test";
    public static final String CUSTOM_MIME_TYPE = "text/plain";
    public static final String CUSTOM_CONTENT_TYPE = CUSTOM_MIME_TYPE + "; charset=" + CUSTOM_ENCODING;

    private MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
    private TransformationService transformationService;

    @Before
    public void setUp() throws Exception
    {
        when(muleContext.getConfiguration().getDefaultEncoding()).thenReturn(DEFAULT_ENCODING);
        when(muleContext.getRegistry().lookupObject(OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE)).thenReturn(mock(RetryPolicyTemplate.class));
        transformationService = new TransformationService(muleContext);
    }

    @Test
    public void usesDefaultEncodingWithNoProperty() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);

        assertThat(muleMessage.getEncoding(), equalTo(DEFAULT_ENCODING));
        assertThat(muleMessage.getOutboundPropertyNames(), not(hasItem(MULE_ENCODING_PROPERTY)));
    }

    @Test
    public void usesCustomEncodingWithProperty() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setEncoding(CUSTOM_ENCODING);

        assertThat(muleMessage.getEncoding(), equalTo(CUSTOM_ENCODING));
    }

    @Test
    public void updatesEncodingWithPropertyAndScope() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setOutboundProperty(MULE_ENCODING_PROPERTY, CUSTOM_ENCODING);

        assertThat(muleMessage.getEncoding(), equalTo(CUSTOM_ENCODING));
        assertThat(muleMessage.getOutboundPropertyNames(), hasItem(MULE_ENCODING_PROPERTY));
        assertThat(muleMessage.getOutboundProperty(MULE_ENCODING_PROPERTY), Matchers.<Object>equalTo(CUSTOM_ENCODING));
    }

    @Test
    public void setsDefaultDataTypeOnCreation() throws Exception
    {
        assertDefaultDataType(new DefaultMuleMessage(TEST, muleContext));
        assertDefaultDataType(new DefaultMuleMessage(TEST, Collections.emptyMap(), muleContext));
        assertDefaultDataType(new DefaultMuleMessage(TEST, Collections.emptyMap(), Collections.emptyMap(), muleContext));
        assertDefaultDataType(new DefaultMuleMessage(TEST, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), muleContext));
    }

    @Test
    public void setsDataTypeFromPreviousMessageOnCreation() throws Exception
    {
        DefaultMuleMessage previousMuleMessage = new DefaultMuleMessage(1, muleContext);
        DataType dataType = DataTypeFactory.create(Long.class, APPLICATION_XML, "UTF-16");
        previousMuleMessage.setDataType(dataType);

        assertDataType(new DefaultMuleMessage(previousMuleMessage), previousMuleMessage.getDataType());
    }

    @Test
    public void overridesDataTypeFromPreviousMessageOnCreation() throws Exception
    {
        DefaultMuleMessage previousMuleMessage = new DefaultMuleMessage(1, muleContext);
        previousMuleMessage.setEncoding(CUSTOM_ENCODING);
        previousMuleMessage.setMimeType(APPLICATION_XML);

        assertDataType(new DefaultMuleMessage(TEST, previousMuleMessage, muleContext), String.class, APPLICATION_XML, CUSTOM_ENCODING);
    }

    @Test
    public void updatesDataTypeWhenPayloadIsNullified() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setEncoding(CUSTOM_ENCODING);
        muleMessage.setMimeType(APPLICATION_XML);
        muleMessage.setPayload(null);

        assertDataType(muleMessage, Object.class, ANY, null);
    }

    @Test
    public void setsNullPayloadWithDataType() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);

        DataType<Integer> newDataType = DataTypeFactory.create(Integer.class, APPLICATION_XML, CUSTOM_ENCODING);

        muleMessage.setPayload(null, newDataType);

        assertThat(muleMessage.getPayload(), Matchers.<Object>equalTo(NullPayload.getInstance()));
        assertDataType(muleMessage, Integer.class, APPLICATION_XML, CUSTOM_ENCODING);
    }

    @Test
    public void setsPayloadWithDataType() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);

        DataType<Integer> newDataType = DataTypeFactory.create(Integer.class, APPLICATION_XML, CUSTOM_ENCODING);

        muleMessage.setPayload(TEST, newDataType);

        assertThat(muleMessage.getPayload(), Matchers.<Object>equalTo(TEST));
        assertDataType(muleMessage, Integer.class, APPLICATION_XML, CUSTOM_ENCODING);
    }

    @Test
    public void updatesDataTypeWhenPayloadChanges() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setEncoding(CUSTOM_ENCODING);
        muleMessage.setMimeType(APPLICATION_XML);
        muleMessage.setPayload(1);

        assertDataType(muleMessage, Integer.class, ANY, null);
    }

    @Test
    public void updatesTypeOnTransformation() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setMimeType(APPLICATION_XML);

        Transformer transformer = mock(Transformer.class);
        when(transformer.isSourceDataTypeSupported(org.mockito.Matchers.<DataType<?>>any())).thenReturn(true);
        DataType outputDataType = DataTypeFactory.create(Integer.class, ANY, null);
        when(transformer.getReturnDataType()).thenReturn(outputDataType);
        when(transformer.transform(anyObject())).thenReturn(1);

        MuleEvent muleEvent = mock(MuleEvent.class);

        MuleMessage result = transformationService.applyTransformers(muleMessage, muleEvent,
                                                                                   Collections.singletonList(transformer));

        assertDataType(result, Integer.class, APPLICATION_XML, DEFAULT_ENCODING);
    }

    @Test
    public void updatesEncodingOnTransformation() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setMimeType(APPLICATION_XML);

        Transformer transformer = mock(Transformer.class);
        when(transformer.isSourceDataTypeSupported(org.mockito.Matchers.<DataType<?>>any())).thenReturn(true);
        DataType outputDataType = DataTypeFactory.create(Integer.class, null, CUSTOM_ENCODING);
        when(transformer.getReturnDataType()).thenReturn(outputDataType);
        when(transformer.transform(anyObject())).thenReturn(Integer.valueOf(1));

        MuleEvent muleEvent = mock(MuleEvent.class);

        MuleMessage result = transformationService.applyTransformers(muleMessage, muleEvent,
                                                                                   Collections.singletonList(transformer));

        assertDataType(result, Integer.class, APPLICATION_XML, CUSTOM_ENCODING);
    }

    @Test
    public void maintainsDataTypeSubClassTypeTransformation() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(new ClassCastException(TEST), muleContext);
        DataType originalDataType = DataTypeFactory.create(RuntimeException.class, ANY);
        muleMessage.setDataType(originalDataType);

        Transformer transformer = mock(Transformer.class);
        DataType outputDataType = DataTypeFactory.create(Object.class, ANY);
        when(transformer.getReturnDataType()).thenReturn(outputDataType);
        when(transformer.isSourceDataTypeSupported(DataTypeFactory.create(muleMessage.getPayload().getClass()))).thenReturn(true);
        when(transformer.transform(anyObject())).thenReturn(new ArithmeticException(TEST));

        MuleEvent muleEvent = mock(MuleEvent.class);

        MuleMessage result = transformationService.applyTransformers(muleMessage, muleEvent,
                                                                     Collections.singletonList(transformer));

        assertThat(result.getDataType().getType(), Matchers.<Class<?>>equalTo(RuntimeException.class));
    }

    @Test
    public void doesNotmaintainDataTypeDueToNotAssignableTransformation() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(new ClassCastException(TEST), muleContext);
        DataType originalDataType = DataTypeFactory.create(RuntimeException.class, ANY);
        muleMessage.setDataType(originalDataType);

        Transformer transformer = mock(Transformer.class);
        DataType outputDataType = DataTypeFactory.create(Object.class, ANY);
        when(transformer.getReturnDataType()).thenReturn(outputDataType);
        when(transformer.isSourceDataTypeSupported(DataTypeFactory.create(muleMessage.getPayload().getClass()))).thenReturn(true);
        when(transformer.transform(anyObject())).thenReturn(new AssertionError(TEST));

        MuleEvent muleEvent = mock(MuleEvent.class);

        MuleMessage result = transformationService.applyTransformers(muleMessage, muleEvent,
                                                                     Collections.singletonList(transformer));

        assertThat(result.getDataType().getType(), Matchers.<Class<?>>equalTo(Object.class));
    }

    @Test
    public void updatesMimeTypeOnTransformation() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setMimeType(ANY);
        muleMessage.setEncoding(CUSTOM_ENCODING);

        Transformer transformer = mock(Transformer.class);
        when(transformer.isSourceDataTypeSupported(org.mockito.Matchers.<DataType<?>>any())).thenReturn(true);
        DataType outputDataType = DataTypeFactory.create(Integer.class, APPLICATION_XML, null);
        when(transformer.getReturnDataType()).thenReturn(outputDataType);

        MuleEvent muleEvent = mock(MuleEvent.class);

        MuleMessage result = transformationService.applyTransformers(muleMessage, muleEvent, Collections.singletonList(transformer));

        assertDataType(result, Integer.class, APPLICATION_XML, CUSTOM_ENCODING);
    }

    @Test
    public void maintainsCurrentDataTypeClassWhenTransformerOutputTypeIsObject() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        DataType<?> originalDataType = muleMessage.getDataType();

        Transformer transformer = mock(Transformer.class);
        when(transformer.isSourceDataTypeSupported(org.mockito.Matchers.<DataType<?>>any())).thenReturn(true);
        DataType outputDataType = DataTypeFactory.create(Object.class, ANY);
        when(transformer.getReturnDataType()).thenReturn(outputDataType);
        when(transformer.transform(muleMessage)).thenReturn(TEST);

        MuleEvent muleEvent = mock(MuleEvent.class);

        MuleMessage result = transformationService.applyTransformers(muleMessage, muleEvent, Collections.singletonList(transformer));

        assertDataType(result, originalDataType);
    }

    @Test
    public void setsDataType() throws Exception
    {
        DataType<Integer> dataType = DataTypeFactory.create(Integer.class);

        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setDataType(dataType);

        assertThat(muleMessage.getDataType(), like(dataType));
    }

    @Test
    public void maintainsDataTypeOnGetPayloadTransformation() throws Exception
    {
        InputStream payload = mock(InputStream.class);
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(payload, muleContext);
        muleMessage.setMimeType(APPLICATION_XML);
        muleMessage.setEncoding(CUSTOM_ENCODING);

        MuleRegistry muleRegistry = mock(MuleRegistry.class);
        when(muleContext.getRegistry()).thenReturn(muleRegistry);
        Transformer transformer = mock(Transformer.class);
        when(transformer.transform(anyObject(), anyString())).thenReturn(TEST);
        when(muleRegistry.lookupTransformer(argThat(any(DataType.class)), argThat(any(DataType.class)))).thenReturn(transformer);

        assertThat(muleMessage.getDataType().getMimeType(), equalTo(APPLICATION_XML));
        assertThat(muleMessage.getDataType().getEncoding(), equalTo(CUSTOM_ENCODING));
    }

    @Test
    public void setsDefaultOutboundPropertyDataType() throws Exception
    {
        MutableMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setOutboundProperty(TEST_PROPERTY, TEST);

        assertDefaultOutboundPropertyDataType(muleMessage);
    }

    @Test
    public void setsCustomOutboundPropertyDataType() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        DataType dataType = DataTypeFactory.create(String.class, APPLICATION_XML, CUSTOM_ENCODING);

        muleMessage.setOutboundProperty(TEST_PROPERTY, TEST, dataType);

        assertOutboundPropertyDataType(muleMessage, dataType);
    }

    @Test
    public void setsDefaultOutboundScopePropertyDataType() throws Exception
    {
        MutableMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setOutboundProperty(TEST_PROPERTY, TEST);

        assertDefaultOutboundPropertyDataType(muleMessage);
    }

    @Test
    public void setsDefaultInboundPropertyDataType() throws Exception
    {
        MutableMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setInboundProperty(TEST_PROPERTY, TEST);

        assertDefaultInboundPropertyDataType(muleMessage);
    }

    @Test
    public void setsCustomInboundPropertyDataType() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        DataType dataType = DataTypeFactory.create(String.class, APPLICATION_XML, CUSTOM_ENCODING);

        muleMessage.setInboundProperty(TEST_PROPERTY, TEST, dataType);

        assertInboundPropertyDataType(muleMessage, dataType);
    }

    @Test
    public void setsDefaultInboundScopePropertyDataType() throws Exception
    {
        MutableMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setInboundProperty(TEST_PROPERTY, TEST);

        assertDefaultInboundPropertyDataType(muleMessage);
    }

    @Test
    public void setsDefaultFlowVariableDataType() throws Exception
    {
        MuleEvent muleEvent = getTestEvent(TEST, muleContext);
        muleEvent.setFlowVariable(TEST_PROPERTY, TEST);

        assertVariableDataType(muleEvent, STRING_DATA_TYPE);
    }

    @Test
    public void setsCustomFlowVariableDataType() throws Exception
    {
        MuleEvent muleEvent = getTestEvent(TEST, muleContext);
        DataType dataType = DataTypeFactory.create(String.class, APPLICATION_XML, CUSTOM_ENCODING);

        muleEvent.setFlowVariable(TEST_PROPERTY, TEST, dataType);

        assertVariableDataType(muleEvent, dataType);
    }

    @Test
    public void setsDefaultSessionVariableDataType() throws Exception
    {
        MuleEvent muleEvent = getTestEvent(TEST, muleContext);
        muleEvent.getSession().setProperty(TEST_PROPERTY, TEST);

        assertSessionVariableDataType(muleEvent, STRING_DATA_TYPE);
    }

    @Test
    public void setsCustomSessionVariableDataType() throws Exception
    {
        MuleEvent muleEvent = getTestEvent(TEST, muleContext);
        DataType dataType = DataTypeFactory.create(String.class, APPLICATION_XML, CUSTOM_ENCODING);

        muleEvent.getSession().setProperty(TEST_PROPERTY, TEST, dataType);

        assertSessionVariableDataType(muleEvent, dataType);
    }

    @Test
    public void setsDataTypeWhenCreatesInboundMessage() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        DataType dataType = DataTypeFactory.create(String.class, APPLICATION_XML, CUSTOM_ENCODING);

        muleMessage.setOutboundProperty(TEST_PROPERTY, TEST, dataType);

        MuleMessage inboundMessage = (MuleMessage) muleMessage.createInboundMessage();

        assertInboundPropertyDataType(inboundMessage, dataType);
    }

    @Test
    public void setsCustomPropertyDataType() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        DataType dataType = DataTypeFactory.create(String.class, APPLICATION_XML, CUSTOM_ENCODING);

        muleMessage.setOutboundProperty(TEST_PROPERTY, TEST, dataType);

        assertOutboundPropertyDataType(muleMessage, dataType);
    }

    @Test
    public void updatesDataTypeWithContentTypePropertyAndScope() throws Exception
    {
        MutableMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setOutboundProperty(CONTENT_TYPE_PROPERTY, CUSTOM_CONTENT_TYPE);

        assertDataType(muleMessage, String.class, CUSTOM_MIME_TYPE, CUSTOM_ENCODING);
    }

    @Test
    public void updatesDataTypeWithContentTypeInInboundProperties() throws Exception
    {
        MutableMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.addInboundProperties(Collections.singletonMap(CONTENT_TYPE_PROPERTY, CUSTOM_CONTENT_TYPE));

        assertDataType(muleMessage, String.class, CUSTOM_MIME_TYPE, CUSTOM_ENCODING);
    }

    private void assertDefaultDataType(MuleMessage muleMessage)
    {
        assertDataType(muleMessage, String.class, ANY_MIME_TYPE, DEFAULT_ENCODING);
    }

    private void assertDataType(MuleMessage muleMessage, Class type, String mimeType, String encoding)
    {
        assertThat(muleMessage.getDataType(), like(type, mimeType, encoding));
    }

    private void assertDataType(MuleMessage muleMessage, DataType<?> dataType)
    {
        assertThat(muleMessage.getDataType(), like(dataType));
    }

    private void assertDefaultInboundPropertyDataType(MuleMessage muleMessage)
    {
        assertInboundPropertyDataType(muleMessage, STRING_DATA_TYPE);
    }

    private void assertDefaultOutboundPropertyDataType(MuleMessage muleMessage)
    {
        assertOutboundPropertyDataType(muleMessage, STRING_DATA_TYPE);
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
