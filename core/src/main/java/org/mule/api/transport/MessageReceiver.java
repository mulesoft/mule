/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.transport;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.source.MessageSource;
import org.mule.api.transaction.Transaction;

import java.io.OutputStream;

/**
 * <code>MessageReceiver</code> is used to receive data from an external system.
 * Typically an implementation of this interface will also implement the listener
 * interface for the external system. For example to listen to a JMS destination the
 * developer would also implement javax.jms.MessageListener. The endpoint (which
 * creates the MessageReceiver) will then register the receiver with the JMS
 * server. Where a listener interface is not availiable the derived
 * <code>MessageReceiver</code> will implement the code necessary to receive
 * data from the external system. For example, the file endpoint will poll a
 * specified directory for its data.
 */
public interface MessageReceiver extends Connectable, MessageSource
{
    /**
     * @return the endpoint from which we are receiving events 
     */
    InboundEndpoint getEndpoint();

    /**
     * @return the service associated with the receiver
     */
    FlowConstruct getFlowConstruct();

    /**
     * @param endpoint the endpoint to listen on
     * @see ImmutableEndpoint
     */
    void setEndpoint(InboundEndpoint endpoint);

    /**
     * The endpointUri that this receiver listens on
     */
    EndpointURI getEndpointURI();

    String getReceiverKey();

    void setReceiverKey(String key);

    MuleEvent routeMessage(MuleMessage message) throws MuleException;

    MuleEvent routeMessage(MuleMessage message, Transaction trans) throws MuleException;

    MuleEvent routeMessage(MuleMessage message, Transaction trans, OutputStream outputStream)
        throws MuleException;
    
    MuleMessage createMuleMessage(Object transportMessage, String encoding) throws MuleException;

    MuleMessage createMuleMessage(Object transportMessage) throws MuleException;
}
