/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import javax.mail.event.MessageCountEvent;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.transport.email.RetrieveMessageReceiver;

public class RetrieveMessageReceiverPollCounter extends RetrieveMessageReceiver
{
    private int numberOfPolls;
    public RetrieveMessageReceiverPollCounter(Connector connector,
            FlowConstruct flowConstruct, InboundEndpoint endpoint,
            long checkFrequency, boolean backupEnabled, String backupFolder)
            throws CreateException
    {
        super(connector, flowConstruct, endpoint, checkFrequency, backupEnabled,
                backupFolder);
        numberOfPolls = 0;
    }
    
    @Override
    public void messagesAdded(MessageCountEvent event) 
    {
        super.messagesAdded(event);
        synchronized (event)
        {
            numberOfPolls++;
        }
    }
    
    synchronized public boolean pollNumbersGreaterThan(int n)
    {
        return numberOfPolls > n;
    }
    
    synchronized public int getNumberOfMadePolls(){
        return numberOfPolls;
    }
}
