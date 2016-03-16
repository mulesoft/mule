/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import org.mule.PropertyScope;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.tck.size.SmallTest;

import java.util.HashSet;

@SmallTest
public class RemovePropertyTransformerTestCase extends AbstractRemoveVariablePropertyTransformerTestCase
{
    public RemovePropertyTransformerTestCase()
    {
        super(new RemovePropertyTransformer());
    }

    @Override
    protected void addMockedPropeerties(MuleEvent mockEvent, HashSet properties)
    {
        MuleMessage mockMessage = mockEvent.getMessage();
        when(mockMessage.getOutboundPropertyNames()).thenReturn(properties);
    }

    @Override
    protected void verifyRemoved(MuleEvent mockEvent, String key)
    {
        verify(mockEvent.getMessage()).removeProperty(key, PropertyScope.OUTBOUND);
    }

    @Override
    protected void verifyNotRemoved(MuleEvent mockEvent, String key)
    {
        verify(mockEvent.getMessage(), times(0)).removeProperty(key, PropertyScope.OUTBOUND);
    }
}
