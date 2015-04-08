/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
import org.mule.transformer.types.DataTypeFactory;

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

        DataType dataType = DataTypeFactory.create(Object.class);

        Transformer transformer1 = mock(Transformer.class);
        when(transformer1.transform(any(Object.class))).thenReturn(null);
        when(transformer1.isSourceDataTypeSupported(any(DataType.class))).thenReturn(true);
        when(transformer1.getReturnDataType()).thenReturn(dataType);

        Transformer transformer2 = mock(Transformer.class);
        when(transformer2.transform(any(Object.class))).thenReturn("foo");
        when(transformer2.isSourceDataTypeSupported(any(DataType.class))).thenReturn(true);
        when(transformer2.getReturnDataType()).thenReturn(dataType);

        DefaultMuleMessage message = new DefaultMuleMessage(null, muleContext);
        message.applyTransformers(null, transformer1, transformer2);

        assertEquals("foo", message.getPayload());
        verify(transformer1, never()).transform(null);
        verify(transformer1, never()).isAcceptNull();
        verify(transformer2, never()).transform(null);
        verify(transformer2, never()).isAcceptNull();
    }
}
