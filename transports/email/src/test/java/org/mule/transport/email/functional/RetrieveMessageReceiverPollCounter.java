/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
