/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;

import org.junit.Test;
import org.mockito.Mockito;

public class PayloadTestCase extends AbstractELTestCase
{
    public PayloadTestCase(Variant variant, String mvelOptimizer)
    {
        super(variant, mvelOptimizer);
    }

    @Test
    public void payload() throws Exception
    {
        MuleMessage mockMessage = Mockito.mock(MuleMessage.class);
        Object payload = new Object();
        Mockito.when(mockMessage.getPayload()).thenReturn(payload);
        assertSame(payload, evaluate("payload", mockMessage));
    }

    @Test
    public void assignPayload() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        evaluate("payload = 'foo'", message);
        assertEquals("foo", message.getPayload());
    }

}
