/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    public PayloadTestCase(Variant variant)
    {
        super(variant);
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
