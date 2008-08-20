/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.mule;

import org.mule.api.MessagingException;
import org.mule.api.MuleException;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageAdapter;
import org.mule.api.transport.MessageDispatcher;
import org.mule.api.transport.MessageReceiver;
import org.mule.transport.AbstractConnector;
import org.mule.transport.AbstractMessageAdapter;
import org.mule.transport.AbstractMessageDispatcherFactory;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.service.TransportServiceDescriptor;

/**
 * <code>TestConnector</code> use a mock connector
 */
public class TestConnector extends AbstractConnector
{

    public static final String TEST = "test";

    private String someProperty;

    public TestConnector()
    {
        super();
        setDispatcherFactory(new AbstractMessageDispatcherFactory()
        {
            public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
            {
                return new TestMessageDispatcher(endpoint);
            }
        });
    }

    protected void doInitialise() throws InitialisationException
    {
        // template method
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect() throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

    protected void doStart() throws MuleException
    {
        // template method
    }

    protected void doStop() throws MuleException
    {
        // template method
    }

    public String getProtocol()
    {
        return TEST;
    }

    public MessageAdapter getMessageAdapter(Object message) throws MessagingException
    {
        return new DummyMessageAdapter(message);
    }

    public String getSomeProperty()
    {
        return someProperty;
    }

    public void setSomeProperty(String someProperty)
    {
        this.someProperty = someProperty;
    }

    public MessageReceiver createReceiver(Service service, InboundEndpoint endpoint) throws Exception
    {
        MessageReceiver receiver = new AbstractMessageReceiver(this, service, endpoint)
        {

            protected void doInitialise() throws InitialisationException
            {
                //nothing to do
            }

            protected void doConnect() throws Exception
            {
                // nothing to do
            }

            protected void doDisconnect() throws Exception
            {
                // nothing to do
            }

            protected void doStart() throws MuleException
            {
                // nothing to do
            }

            protected void doStop() throws MuleException
            {
                // nothing to do
            }

            protected void doDispose()
            {
                // nothing to do               
            }
        };
        return receiver;
    }

    /**
     * Open up the access to the service descriptor for testing purposes.
     */
    public TransportServiceDescriptor getServiceDescriptor()
    {
        return super.getServiceDescriptor();
    }

    public void destroyReceiver(MessageReceiver receiver, InboundEndpoint endpoint) throws Exception
    {
        // nothing to do
    }

    public class DummyMessageAdapter extends AbstractMessageAdapter
    {
        /**
         * Serial version
         */
        private static final long serialVersionUID = -2304322766342059136L;

        private Object message = new String("DummyMessage");

        public DummyMessageAdapter(Object message)
        {
            this.message = message;
        }

        public Object getPayload()
        {
            return message;
        }

        public byte[] getPayloadAsBytes() throws Exception
        {

            return message.toString().getBytes();
        }

        public String getPayloadAsString(String encoding) throws Exception
        {
            return message.toString();
        }

        public void setPayload(Object payload)
        {
            this.message = payload;
        }

        public ThreadSafeAccess newThreadCopy()
        {
            return this;
        }
    }

}
