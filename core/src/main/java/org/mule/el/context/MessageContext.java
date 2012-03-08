/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.context;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;

public class MessageContext
{
    protected MuleMessage message;

    public String getCorrelationId()
    {
        return message.getCorrelationId();
    }

    public int getCorrelationSequence()
    {
        return message.getCorrelationSequence();
    }

    public int getCorrelationGroupSize()
    {
        return message.getCorrelationGroupSize();
    }

    public DataType<?> getDataType()
    {
        return message.getDataType();
    }

    public MessageContext(MuleMessage message)
    {
        this.message = message;
    }

    public Object getPayload()
    {
        return message.getPayload();
    }

    public void setPayload(Object payload)
    {
        message.setPayload(payload);
    }

}
