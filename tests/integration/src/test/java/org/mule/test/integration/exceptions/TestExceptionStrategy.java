/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.exceptions;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.message.DefaultExceptionPayload;

public class TestExceptionStrategy extends DefaultMessagingExceptionStrategy
{
    @Override
    public MuleMessage getReturnMessage(Exception e)
    {
        MuleMessage message = new DefaultMuleMessage("Ka-boom!", muleContext);
        message.setExceptionPayload(new DefaultExceptionPayload(e));
        return message;
    }
}
