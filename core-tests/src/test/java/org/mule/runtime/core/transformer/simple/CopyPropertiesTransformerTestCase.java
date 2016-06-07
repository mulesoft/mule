/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.SimpleDataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.expression.ExpressionManager;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.util.StringUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class CopyPropertiesTransformerTestCase extends AbstractMuleTestCase
{
    public static final String ENCODING = "encoding";
    public static final String INBOUND_PROPERTY_KEY = "propKey";
    public static final DataType PROPERTY_DATA_TYPE = DataType.STRING_DATA_TYPE;
    private static final Serializable PROPERTY_VALUE = StringUtils.EMPTY;

    @Mock
    private MuleContext mockMuleContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleMessage mockMuleMessage;
    @Mock
    private ExpressionManager mockExpressionManager;

    @Before
    public void setUp() throws Exception
    {
        when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
        Mockito.when(mockExpressionManager.parse(anyString(), Mockito.any(MuleEvent.class))).thenAnswer(
                new Answer<String>()
                {
                    @Override
                    public String answer(InvocationOnMock invocation) throws Throwable
                    {

                        return (String) invocation.getArguments()[0];
                    }
                });
        when(mockMuleMessage.getDataType()).thenReturn(new SimpleDataType(String.class));
    }

    @Test
    public void testCopySingleProperty() throws TransformerException, InitialisationException
    {
        CopyPropertiesTransformer copyPropertiesTransformer = createCopyPropertiesTransformer(INBOUND_PROPERTY_KEY);
        when(mockMuleMessage.getInboundProperty(INBOUND_PROPERTY_KEY)).thenReturn(PROPERTY_VALUE);
        when(mockMuleMessage.getInboundPropertyDataType(INBOUND_PROPERTY_KEY)).thenReturn(PROPERTY_DATA_TYPE);

        copyPropertiesTransformer.transform(mockMuleMessage, ENCODING);

        verify(mockMuleMessage).getInboundProperty(INBOUND_PROPERTY_KEY);
        verify(mockMuleMessage).copyProperty(INBOUND_PROPERTY_KEY);
    }

    @Test
    public void testCopyNonExistentProperty() throws TransformerException, InitialisationException
    {
        CopyPropertiesTransformer copyPropertiesTransformer = createCopyPropertiesTransformer(INBOUND_PROPERTY_KEY);
        when(mockMuleMessage.getInboundProperty(INBOUND_PROPERTY_KEY)).thenReturn(null);

        copyPropertiesTransformer.transform(mockMuleMessage, ENCODING);

        verify(mockMuleMessage, times(0)).setOutboundProperty(Matchers.anyString(),Matchers.anyObject());
    }

    @Test
    public void testCopyUsingRegex() throws InitialisationException, TransformerException
    {
        CopyPropertiesTransformer copyPropertiesTransformer = createCopyPropertiesTransformer("MULE_*");
        when(mockMuleMessage.getInboundPropertyNames()).thenReturn(new HashSet<>(Arrays.asList("MULE_ID", "MULE_CORRELATION_ID", "SomeVar", "MULE_GROUP_ID")));
        when(mockMuleMessage.getInboundProperty("MULE_ID")).thenReturn(PROPERTY_VALUE);
        when(mockMuleMessage.getInboundProperty("MULE_CORRELATION_ID")).thenReturn(PROPERTY_VALUE);
        when(mockMuleMessage.getInboundProperty("MULE_GROUP_ID")).thenReturn(PROPERTY_VALUE);
        when(mockMuleMessage.getInboundProperty("SomeVar")).thenReturn(PROPERTY_VALUE);
        when(mockMuleMessage.getInboundPropertyDataType(anyString())).thenReturn(PROPERTY_DATA_TYPE);

        copyPropertiesTransformer.transform(mockMuleMessage, ENCODING);

        verify(mockMuleMessage, times(0)).setOutboundProperty("SomeVar", PROPERTY_VALUE, PROPERTY_DATA_TYPE);
        verify(mockMuleMessage).copyProperty("MULE_ID");
        verify(mockMuleMessage).copyProperty("MULE_CORRELATION_ID");
        verify(mockMuleMessage).copyProperty("MULE_GROUP_ID");
    }

    public CopyPropertiesTransformer createCopyPropertiesTransformer(String inboundPropertyKey) throws InitialisationException
    {
        CopyPropertiesTransformer copyPropertiesTransformer = new CopyPropertiesTransformer();
        copyPropertiesTransformer.setMuleContext(mockMuleContext);
        copyPropertiesTransformer.setPropertyName(inboundPropertyKey);
        copyPropertiesTransformer.initialise();

        return copyPropertiesTransformer;
    }
}
