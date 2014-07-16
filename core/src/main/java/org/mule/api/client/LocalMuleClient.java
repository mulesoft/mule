/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.client;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;

import java.util.Map;

/**
 * Extends {@link MuleClient} adding methods that allow the use of an endpoint
 * instance.
 */
public interface LocalMuleClient extends MuleClient
{

    /**
     * Sends an event synchronously to a endpointUri via a Mule server and a
     * resulting message is returned.
     * 
     * @param endpoint
     * @param payload the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload. In
     *            the case of Jms you could set the JMSReplyTo property in these
     *            properties.
     * @return A return message, this could be <code>null</code> if the the
     *         components invoked explicitly sets a return as <code>null</code>.
     * @throws org.mule.api.MuleException
     */
    MuleMessage process(OutboundEndpoint endpoint, Object payload, Map<String, Object> messageProperties)
        throws MuleException;

    /**
     * Sends an event synchronously to a endpointUri via a Mule server and a
     * resulting message is returned.
     * 
     * @param endpoint
     * @param message the Message for the event
     * @return A return message, this could be <code>null</code> if the the
     *         components invoked explicitly sets a return as <code>null</code>.
     * @throws org.mule.api.MuleException
     */
    MuleMessage process(OutboundEndpoint endpoint, MuleMessage message) throws MuleException;

    /**
     * Will receive an event from an endpointUri determined by the URL.
     * 
     * @param endpoint the Mule URL used to determine the destination and transport
     *            of the message
     * @param timeout how long to block waiting to receive the event, if set to 0 the
     *            receive will not wait at all and if set to -1 the receive will wait
     *            forever
     * @return the message received or <code>null</code> if no message was received
     * @throws org.mule.api.MuleException
     */
    MuleMessage request(InboundEndpoint endpoint, long timeout) throws MuleException;

    /**
     * Will register the specified processor as a listener for the inbound endpoint.
     * This may be implemented by subscription or polling depending on the transport
     * implementation
     * 
     * @param endpoint
     * @param processor
     * @throws MuleException
     */
    // void receive(InboundEndpoint endpoint, MessageProcessor processor) throws
    // MuleException;

}
