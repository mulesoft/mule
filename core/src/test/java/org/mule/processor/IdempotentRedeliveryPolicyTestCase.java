/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

import junit.framework.Assert;

@SmallTest
public class IdempotentRedeliveryPolicyTestCase extends AbstractMuleTestCase
{

    @Test
    public void messageDigestFailure() throws Exception
    {
        IdempotentRedeliveryPolicy irp = new IdempotentRedeliveryPolicy();
        irp.setUseSecureHash(true);
        irp.setFlowConstruct(mock(FlowConstruct.class));
        irp.initialise();

        MuleMessage message = mock(MuleMessage.class);
        when(message.getPayload()).thenReturn(new Object());
        MuleEvent event = mock(MuleEvent.class);
        when(event.getMessage()).thenReturn(message);
        MuleEvent process = irp.process(event);
        Assert.assertNull(process);
    }
}


