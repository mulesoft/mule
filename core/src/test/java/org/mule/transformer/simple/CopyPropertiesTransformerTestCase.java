/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
    private static final Object PROPERTY_VALUE = new Object();

    @Mock
    private MuleContext mockMuleContext;
    @Mock
    private MuleMessage mockMuleMessage;
    @Mock
    private ExpressionManager mockExpressionManager;

    @Before
    public void setUp() throws Exception
    {
        when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
        Mockito.when(mockExpressionManager.parse(anyString(), Mockito.any(MuleMessage.class))).thenAnswer(
                new Answer<String>()
                {
                    @Override
                    public String answer(InvocationOnMock invocation) throws Throwable
                    {

                        return (String) invocation.getArguments()[0];
                    }
                });
    }

    @Test
    public void testCopySingleProperty() throws TransformerException, InitialisationException
    {
        CopyPropertiesTransformer copyPropertiesTransformer = createCopyPropertiesTransformer(INBOUND_PROPERTY_KEY);
        when(mockMuleMessage.getInboundProperty(INBOUND_PROPERTY_KEY)).thenReturn(PROPERTY_VALUE);
        when(mockMuleMessage.getPropertyDataType(INBOUND_PROPERTY_KEY, PropertyScope.INBOUND)).thenReturn(PROPERTY_DATA_TYPE);

        copyPropertiesTransformer.transform(mockMuleMessage, ENCODING);

        verify(mockMuleMessage).getInboundProperty(INBOUND_PROPERTY_KEY);
        verify(mockMuleMessage).setOutboundProperty(INBOUND_PROPERTY_KEY, PROPERTY_VALUE, PROPERTY_DATA_TYPE);
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
        when(mockMuleMessage.getPropertyDataType(anyString(), Matchers.<PropertyScope>anyObject())).thenReturn(PROPERTY_DATA_TYPE);

        copyPropertiesTransformer.transform(mockMuleMessage, ENCODING);

        verify(mockMuleMessage, times(0)).setOutboundProperty("SomeVar", PROPERTY_VALUE, PROPERTY_DATA_TYPE);
        verify(mockMuleMessage).setOutboundProperty("MULE_ID", PROPERTY_VALUE, PROPERTY_DATA_TYPE);
        verify(mockMuleMessage).setOutboundProperty("MULE_CORRELATION_ID", PROPERTY_VALUE, PROPERTY_DATA_TYPE);
        verify(mockMuleMessage).setOutboundProperty("MULE_GROUP_ID", PROPERTY_VALUE, PROPERTY_DATA_TYPE);
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
