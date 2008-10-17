/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.retry;

import org.mule.api.MuleMessage;
import org.mule.api.retry.RetryContext;

import java.util.Map;

/**
 * The RetryContext is used to store any data which carries over from 
 * attempt to attempt such as response messages.
 */
public class DefaultRetryContext implements RetryContext
{
    private MuleMessage[] returnMessages;
    private Map metaInfo;
    private String description;

    public DefaultRetryContext(String description)
    {
        this.description = description;
    }

    public Map getMetaInfo()
    {
        return metaInfo;
    }

    public void setMetaInfo(Map metaInfo)
    {
        this.metaInfo = metaInfo;
    }

    public MuleMessage[] getReturnMessages()
    {
        return returnMessages;
    }

    public MuleMessage getFirstReturnMessage()
    {
        return (returnMessages == null ? null : returnMessages[0]);
    }

    public void setReturnMessages(MuleMessage[] returnMessages)
    {
        this.returnMessages = returnMessages;
    }

    public void addReturnMessage(MuleMessage result)
    {
        if(returnMessages ==null)
        {
            returnMessages = new MuleMessage[]{result};
        }
        else
        {
            MuleMessage[] newReturnMessages = new MuleMessage[returnMessages.length+1];
            System.arraycopy(newReturnMessages, 0, returnMessages, 0, 1);
            returnMessages = newReturnMessages;
        }
    }

    public String getDescription()
    {
        return description;
    }
}
