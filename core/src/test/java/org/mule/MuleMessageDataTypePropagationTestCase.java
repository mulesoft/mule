/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

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
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import static org.mule.transformer.types.MimeTypes.ANY;
import static org.mule.transformer.types.MimeTypes.APPLICATION_XML;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.TypedValue;
import org.mule.transport.NullPayload;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;

import javax.activation.DataHandler;

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

    @Before
    public void setUp() throws Exception
    {
        when(muleContext.getConfiguration().getDefaultEncoding()).thenReturn(DEFAULT_ENCODING);
    }

    @Test
    public void usesDefaultEncodingWithNoProperty() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);

        assertThat(muleMessage.getEncoding(), equalTo(DEFAULT_ENCODING));
        assertThat(muleMessage.getPropertyNames(), not(hasItem(MuleProperties.MULE_ENCODING_PROPERTY)));
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
        muleMessage.setProperty(MuleProperties.MULE_ENCODING_PROPERTY, CUSTOM_ENCODING, PropertyScope.OUTBOUND);

        assertThat(muleMessage.getEncoding(), equalTo(CUSTOM_ENCODING));
        assertThat(muleMessage.getPropertyNames(), hasItem(MuleProperties.MULE_ENCODING_PROPERTY));
        assertThat(muleMessage.getProperty(MuleProperties.MULE_ENCODING_PROPERTY), Matchers.<Object>equalTo(CUSTOM_ENCODING));
    }

    @Test
    public void updatesEncodingWithProperty() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setProperty(MuleProperties.MULE_ENCODING_PROPERTY, CUSTOM_ENCODING);

        assertThat(muleMessage.getEncoding(), equalTo(CUSTOM_ENCODING));
        assertThat(muleMessage.getPropertyNames(), hasItem(MuleProperties.MULE_ENCODING_PROPERTY));
        assertThat(muleMessage.getProperty(MuleProperties.MULE_ENCODING_PROPERTY), Matchers.<Object>equalTo(CUSTOM_ENCODING));
    }

    @Test
    public void setsDefaultDataTypeOnCreation() throws Exception
    {
        assertDefaultDataType(new DefaultMuleMessage(TEST, muleContext));
        assertDefaultDataType(new DefaultMuleMessage(TEST, Collections.<String, Object>emptyMap(), muleContext));
        assertDefaultDataType(new DefaultMuleMessage(TEST, Collections.<String, Object>emptyMap(), Collections.<String, DataHandler>emptyMap(), muleContext));
        assertDefaultDataType(new DefaultMuleMessage(TEST, Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap(), Collections.<String, DataHandler>emptyMap(), muleContext));
    }

    @Test
    public void setsDataTypeFromPreviousMessageOnCreation() throws Exception
    {
        DefaultMuleMessage previousMuleMessage = new DefaultMuleMessage(1, muleContext);
        DataType dataType = DataTypeFactory.create(Long.class, APPLICATION_XML);
        dataType.setEncoding("UTF-16");
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

        DataType<Integer> newDataType = DataTypeFactory.create(Integer.class, APPLICATION_XML);
        newDataType.setEncoding(CUSTOM_ENCODING);

        muleMessage.setPayload(null, newDataType);

        assertThat(muleMessage.getPayload(), Matchers.<Object>equalTo(NullPayload.getInstance()));
        assertDataType(muleMessage, Integer.class, APPLICATION_XML, CUSTOM_ENCODING);
    }

    @Test
    public void setsPayloadWithDataType() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);

        DataType<Integer> newDataType = DataTypeFactory.create(Integer.class, APPLICATION_XML);
        newDataType.setEncoding(CUSTOM_ENCODING);

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
        DataType outputDataType = DataTypeFactory.create(Integer.class, ANY);
        outputDataType.setEncoding(null);
        when(transformer.getReturnDataType()).thenReturn(outputDataType);

        MuleEvent muleEvent = mock(MuleEvent.class);

        muleMessage.applyAllTransformers(muleEvent, Collections.singletonList(transformer));

        assertDataType(muleMessage, Integer.class, APPLICATION_XML, DEFAULT_ENCODING);
    }

    @Test
    public void updatesEncodingOnTransformation() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setMimeType(APPLICATION_XML);

        Transformer transformer = mock(Transformer.class);
        when(transformer.isSourceDataTypeSupported(org.mockito.Matchers.<DataType<?>>any())).thenReturn(true);
        DataType outputDataType = DataTypeFactory.create(Integer.class, null);
        outputDataType.setEncoding(CUSTOM_ENCODING);
        when(transformer.getReturnDataType()).thenReturn(outputDataType);

        MuleEvent muleEvent = mock(MuleEvent.class);

        muleMessage.applyAllTransformers(muleEvent, Collections.singletonList(transformer));

        assertDataType(muleMessage, Integer.class, APPLICATION_XML, CUSTOM_ENCODING);
    }

    @Test
    public void updatesMimeTypeOnTransformation() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setMimeType(ANY);
        muleMessage.setEncoding(CUSTOM_ENCODING);

        Transformer transformer = mock(Transformer.class);
        when(transformer.isSourceDataTypeSupported(org.mockito.Matchers.<DataType<?>>any())).thenReturn(true);
        DataType outputDataType = DataTypeFactory.create(Integer.class, APPLICATION_XML);
        outputDataType.setEncoding(null);
        when(transformer.getReturnDataType()).thenReturn(outputDataType);

        MuleEvent muleEvent = mock(MuleEvent.class);

        muleMessage.applyAllTransformers(muleEvent, Collections.singletonList(transformer));

        assertDataType(muleMessage, Integer.class, APPLICATION_XML, CUSTOM_ENCODING);
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

        muleMessage.applyAllTransformers(muleEvent, Collections.singletonList(transformer));

        assertDataType(muleMessage, originalDataType);
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

        muleMessage.getPayloadAsString();

        assertThat(muleMessage.getDataType().getMimeType(), equalTo(APPLICATION_XML));
        assertThat(muleMessage.getDataType().getEncoding(), equalTo(CUSTOM_ENCODING));
    }

    @Test
    public void setsDefaultPropertyDataType() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setProperty(TEST_PROPERTY, TEST);

        assertDefaultPropertyDataType(muleMessage, PropertyScope.OUTBOUND);
    }

    @Test
    public void setsDefaultOutboundPropertyDataType() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setOutboundProperty(TEST_PROPERTY, TEST);

        assertDefaultPropertyDataType(muleMessage, PropertyScope.OUTBOUND);
    }

    @Test
    public void setsCustomOutboundPropertyDataType() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        DataType dataType = DataTypeFactory.create(String.class, APPLICATION_XML);
        dataType.setEncoding(CUSTOM_ENCODING);

        muleMessage.setOutboundProperty(TEST_PROPERTY, TEST, dataType);

        assertPropertyDataType(muleMessage, PropertyScope.OUTBOUND, dataType);
    }

    @Test
    public void setsDefaultOutboundScopePropertyDataType() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setProperty(TEST_PROPERTY, TEST, PropertyScope.OUTBOUND);

        assertDefaultPropertyDataType(muleMessage, PropertyScope.OUTBOUND);
    }

    @Test
    public void setsDefaultInboundPropertyDataType() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setInboundProperty(TEST_PROPERTY, TEST);

        assertDefaultPropertyDataType(muleMessage, PropertyScope.INBOUND);
    }

    @Test
    public void setsCustomInboundPropertyDataType() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        DataType dataType = DataTypeFactory.create(String.class, APPLICATION_XML);
        dataType.setEncoding(CUSTOM_ENCODING);

        muleMessage.setInboundProperty(TEST_PROPERTY, TEST, dataType);

        assertPropertyDataType(muleMessage, PropertyScope.INBOUND, dataType);
    }

    @Test
    public void setsDefaultInboundScopePropertyDataType() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setProperty(TEST_PROPERTY, TEST, PropertyScope.INBOUND);

        assertDefaultPropertyDataType(muleMessage, PropertyScope.INBOUND);
    }

    @Test
    public void setsDefaultInvocationPropertyDataType() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setInvocationProperty(TEST_PROPERTY, TEST);

        assertDefaultPropertyDataType(muleMessage, PropertyScope.INVOCATION);
    }

    @Test
    public void setsCustomInvocationPropertyDataType() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        DataType dataType = DataTypeFactory.create(String.class, APPLICATION_XML);
        dataType.setEncoding(CUSTOM_ENCODING);

        muleMessage.setInvocationProperty(TEST_PROPERTY, TEST, dataType);

        assertPropertyDataType(muleMessage, PropertyScope.INVOCATION, dataType);
    }

    @Test
    public void setsDefaultInvocationScopePropertyDataType() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setProperty(TEST_PROPERTY, TEST, PropertyScope.INVOCATION);

        assertDefaultPropertyDataType(muleMessage, PropertyScope.INVOCATION);
    }

    @Test
    public void setsDefaultSessionPropertyDataType() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setSessionProperties(new HashMap<String, TypedValue>());
        muleMessage.setProperty(TEST_PROPERTY, TEST, PropertyScope.SESSION);

        assertDefaultPropertyDataType(muleMessage, PropertyScope.SESSION);
    }

    @Test
    public void setsDataTypeWhenCreatesInboundMessage() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        DataType dataType = DataTypeFactory.create(String.class, APPLICATION_XML);
        dataType.setEncoding(CUSTOM_ENCODING);

        muleMessage.setProperty(TEST_PROPERTY, TEST, PropertyScope.OUTBOUND, dataType);

        DefaultMuleMessage inboundMessage = (DefaultMuleMessage) muleMessage.createInboundMessage();

        assertPropertyDataType(inboundMessage, PropertyScope.INBOUND, dataType);
    }

    @Test
    public void setsCustomPropertyDataType() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        DataType dataType = DataTypeFactory.create(String.class, APPLICATION_XML);
        dataType.setEncoding(CUSTOM_ENCODING);

        muleMessage.setProperty(TEST_PROPERTY, TEST, PropertyScope.OUTBOUND, dataType);

        assertPropertyDataType(muleMessage, PropertyScope.OUTBOUND, dataType);
    }

    @Test
    public void updatesDataTypeWithContentTypeProperty() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setProperty(MuleProperties.CONTENT_TYPE_PROPERTY, CUSTOM_CONTENT_TYPE);

        assertDataType(muleMessage, String.class, CUSTOM_MIME_TYPE, CUSTOM_ENCODING);
    }

    @Test
    public void updatesDataTypeWithContentTypePropertyAndScope() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.setProperty(MuleProperties.CONTENT_TYPE_PROPERTY, CUSTOM_CONTENT_TYPE, PropertyScope.OUTBOUND);

        assertDataType(muleMessage, String.class, CUSTOM_MIME_TYPE, CUSTOM_ENCODING);
    }

    @Test
    public void updatesDataTypeWithContentTypeInProperties() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.addProperties(Collections.<String, Object>singletonMap(MuleProperties.CONTENT_TYPE_PROPERTY, CUSTOM_CONTENT_TYPE));

        assertDataType(muleMessage, String.class, CUSTOM_MIME_TYPE, CUSTOM_ENCODING);
    }

    @Test
    public void updatesDataTypeWithContentTypeInInboundProperties() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST, muleContext);
        muleMessage.addInboundProperties(Collections.<String, Object>singletonMap(MuleProperties.CONTENT_TYPE_PROPERTY, CUSTOM_CONTENT_TYPE));

        assertDataType(muleMessage, String.class, CUSTOM_MIME_TYPE, CUSTOM_ENCODING);
    }

    private void assertDefaultDataType(MuleMessage muleMessage)
    {
        assertDataType(muleMessage, String.class, DataType.ANY_MIME_TYPE, DEFAULT_ENCODING);
    }

    private void assertDataType(MuleMessage muleMessage, Class type, String mimeType, String encoding)
    {
        assertThat(muleMessage.getDataType(), like(type, mimeType, encoding));
    }

    private void assertDataType(MuleMessage muleMessage, DataType<?> dataType)
    {
        assertThat(muleMessage.getDataType(), like(dataType));
    }

    private void assertDefaultPropertyDataType(DefaultMuleMessage muleMessage, PropertyScope scope)
    {
        assertPropertyDataType(muleMessage, scope, DataType.STRING_DATA_TYPE);
    }

    private void assertPropertyDataType(DefaultMuleMessage muleMessage, PropertyScope scope, DataType dataType)
    {
        DataType<?> actualDataType = muleMessage.getPropertyDataType(TEST_PROPERTY, scope);
        assertThat(actualDataType, like(dataType));
    }
}
