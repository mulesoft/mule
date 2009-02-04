/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.retry;

import org.mule.api.MuleMessage;

import java.util.Map;

/**
 * The RetryContext is used to store any data which carries over from 
 * attempt to attempt such as response messages.
 */
public interface RetryContext
{
    String FAILED_RECEIVER = "failedReceiver";
    String FAILED_DISPATCHER = "failedDispatcher";
    String FAILED_REQUESTER = "failedRequester";
    
    Map getMetaInfo();

    void setMetaInfo(Map metaInfo);

    MuleMessage[] getReturnMessages();

    MuleMessage getFirstReturnMessage();

    void setReturnMessages(MuleMessage[] returnMessages);

    void addReturnMessage(MuleMessage result);

    String getDescription();
}
