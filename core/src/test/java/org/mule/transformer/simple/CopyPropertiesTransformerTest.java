/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class CopyPropertiesTransformerTest extends AbstractMuleTestCase
{
    public static final String ENCODING = "encoding";
    public static final String INBOUND_PROPERTY_KEY = "propKey";
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
        CopyPropertiesTransformer copyPropertiesTransformer = new CopyPropertiesTransformer();
        copyPropertiesTransformer.setMuleContext(mockMuleContext);
        copyPropertiesTransformer.setPropertyName(INBOUND_PROPERTY_KEY);
        copyPropertiesTransformer.initialise();
        when(mockMuleMessage.getInboundProperty(INBOUND_PROPERTY_KEY)).thenReturn(PROPERTY_VALUE);
        copyPropertiesTransformer.transform(mockMuleMessage, ENCODING);
        verify(mockMuleMessage).getInboundProperty(INBOUND_PROPERTY_KEY);
        verify(mockMuleMessage).setOutboundProperty(INBOUND_PROPERTY_KEY, PROPERTY_VALUE);
    }

    @Test
    public void testCopyNonExistentProperty() throws TransformerException, InitialisationException
    {
        CopyPropertiesTransformer copyPropertiesTransformer = new CopyPropertiesTransformer();
        copyPropertiesTransformer.setMuleContext(mockMuleContext);
        copyPropertiesTransformer.setPropertyName(INBOUND_PROPERTY_KEY);
        copyPropertiesTransformer.initialise();
        when(mockMuleMessage.getInboundProperty(INBOUND_PROPERTY_KEY)).thenReturn(null);
        copyPropertiesTransformer.transform(mockMuleMessage, ENCODING);
        verify(mockMuleMessage, times(0)).setOutboundProperty(Matchers.anyString(),Matchers.anyObject());
    }

    @Test
    @Ignore
    public void testCopyUsingRegex() throws InitialisationException, TransformerException
    {
        CopyPropertiesTransformer copyPropertiesTransformer = new CopyPropertiesTransformer();
        copyPropertiesTransformer.setMuleContext(mockMuleContext);
        copyPropertiesTransformer.setPropertyName("MULE_(.*)");
        copyPropertiesTransformer.initialise();
        when(mockMuleMessage.getInboundPropertyNames()).thenReturn(new HashSet<String>(Arrays.asList("MULE_ID", "MULE_CORRELATION_ID", "SomeVar", "MULE_GROUP_ID")));
        when(mockMuleMessage.getInboundProperty("MULE_ID")).thenReturn(PROPERTY_VALUE);
        when(mockMuleMessage.getInboundProperty("MULE_CORRELATION_ID")).thenReturn(PROPERTY_VALUE);
        when(mockMuleMessage.getInboundProperty("MULE_GROUP_ID")).thenReturn(PROPERTY_VALUE);
        when(mockMuleMessage.getInboundProperty("SomeVar")).thenReturn(PROPERTY_VALUE);
        copyPropertiesTransformer.transform(mockMuleMessage, ENCODING);
        verify(mockMuleMessage, times(0)).setOutboundProperty("SomeVar", PROPERTY_VALUE);
        verify(mockMuleMessage).setOutboundProperty("MULE_ID", PROPERTY_VALUE);
        verify(mockMuleMessage).setOutboundProperty("MULE_CORRELATION_ID", PROPERTY_VALUE);
        verify(mockMuleMessage).setOutboundProperty("MULE_GROUP_ID", PROPERTY_VALUE);
    }
}
