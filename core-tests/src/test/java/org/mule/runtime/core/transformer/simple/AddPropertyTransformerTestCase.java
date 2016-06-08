/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.api.metadata.DataType;
import org.mule.tck.size.SmallTest;

import org.mockito.ArgumentCaptor;

@SmallTest
public class AddPropertyTransformerTestCase extends AbstractAddVariablePropertyTransformerTestCase
{

    public AddPropertyTransformerTestCase()
    {
        super(new AddPropertyTransformer());
    }

    @Override
    protected void verifyAdded(MuleEvent event, String key, String value, ArgumentCaptor<DataType> dataTypeCaptor)
    {
        verify(event.getMessage()).setOutboundProperty(argThat(equalTo(key)), argThat(equalTo(value)), dataTypeCaptor.capture());
    }

    @Override
    protected void verifyNotAdded(MuleEvent event)
    {
        verify(event.getMessage(), times(0)).setOutboundProperty((String) isNull(), anyString()
        );
    }

    @Override
    protected void verifyRemoved(MuleEvent event, String key)
    {
        verify(event.getMessage()).removeOutboundProperty(key);
    }
}
