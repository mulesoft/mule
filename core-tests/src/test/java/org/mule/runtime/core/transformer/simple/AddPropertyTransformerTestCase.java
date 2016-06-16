/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import static org.hamcrest.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.tck.size.SmallTest;

import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;

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
        verify(event, times(1)).setMessage(argThat(new ArgumentMatcher<MuleMessage>()
        {
            @Override
            public boolean matches(Object argument)
            {
                return ((MuleMessage) argument).getOutboundProperty(key).equals(value);
            }
        }));
    }

    @Override
    protected void verifyNotAdded(MuleEvent event)
    {
        verify(event, times(0)).setMessage(argThat(any(MuleMessage.class)));
    }

    @Override
    protected void verifyRemoved(MuleEvent event, String key)
    {
        verify(event, times(1)).setMessage(argThat(new ArgumentMatcher<MuleMessage>()
        {
            @Override
            public boolean matches(Object argument)
            {
                return ((MuleMessage) argument).getOutboundProperty(key) ==  null;
            }
        }));
    }
}
