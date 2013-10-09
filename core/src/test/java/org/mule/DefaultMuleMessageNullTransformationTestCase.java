/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class DefaultMuleMessageNullTransformationTestCase extends AbstractMuleTestCase
{

    @Test
    public void transformerIsNeverCalledWithANullValue() throws MuleException
    {
        MuleContext muleContext = mock(MuleContext.class);
        MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
        when(muleConfiguration.isCacheMessageOriginalPayload()).thenReturn(false);
        when(muleContext.getConfiguration()).thenReturn(muleConfiguration);

        Transformer transformer1 = mock(Transformer.class);
        when(transformer1.transform(any(Object.class))).thenReturn(null);
        when(transformer1.isSourceDataTypeSupported(any(DataType.class))).thenReturn(true);

        Transformer transformer2 = mock(Transformer.class);
        when(transformer2.transform(any(Object.class))).thenReturn("foo");
        when(transformer2.isSourceDataTypeSupported(any(DataType.class))).thenReturn(true);

        DefaultMuleMessage message = new DefaultMuleMessage(null, muleContext);
        message.applyTransformers(null, transformer1, transformer2);

        assertEquals("foo", message.getPayload());
        verify(transformer1, never()).transform(null);
        verify(transformer1, never()).isAcceptNull();
        verify(transformer2, never()).transform(null);
        verify(transformer2, never()).isAcceptNull();
    }
}
