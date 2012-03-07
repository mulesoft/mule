/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el;

import static org.junit.Assert.assertEquals;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;

import org.junit.Test;

public class MessageTestCase extends AbstractELTestCase
{
    public MessageTestCase(Variant variant)
    {
        super(variant);
    }

    @Test
    public void message() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        assertEquals(message, evaluate("message", message));
    }

    @Test
    public void assignToMessage() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        assertImmutableVariable("message='foo'", message);
    }

}
