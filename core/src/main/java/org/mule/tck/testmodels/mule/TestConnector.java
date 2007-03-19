/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.mule;

import org.mule.providers.AbstractConnector;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.providers.AbstractMessageDispatcherFactory;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.OutputHandler;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.umo.provider.UMOStreamMessageAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <code>TestConnector</code> use a mock connector
 */
public class TestConnector extends AbstractConnector
{

    private String someProperty;

    public TestConnector()
    {
        super();
        setDispatcherFactory(new AbstractMessageDispatcherFactory()
        {
            public UMOMessageDispatcher create(UMOImmutableEndpoint endpoint) throws UMOException
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

    protected void doStart() throws UMOException
    {
        // template method
    }

    protected void doStop() throws UMOException
    {
        // template method
    }

    public String getProtocol()
    {
        return "test";
    }

    public UMOMessageAdapter getMessageAdapter(Object message) throws MessagingException
    {
        return new DummyMessageAdapter(message);
    }

    public UMOStreamMessageAdapter getStreamMessageAdapter(InputStream in, OutputStream out)
        throws MessagingException
    {
        return new DummyMessageAdapter(in);
    }


    public String getSomeProperty()
    {
        return someProperty;
    }

    public void setSomeProperty(String someProperty)
    {
        this.someProperty = someProperty;
    }

    public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        UMOMessageReceiver receiver = new AbstractMessageReceiver(this, component, endpoint)
        {
            protected void doConnect() throws Exception
            {
                // nothing to do
            }

            protected void doDisconnect() throws Exception
            {
                // nothing to do
            }

            protected void doStart() throws UMOException
            {
                // nothing to do
            }

            protected void doStop() throws UMOException
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

    public void destroyReceiver(UMOMessageReceiver receiver, UMOEndpoint endpoint) throws Exception
    {
        // nothing to do
    }

    public class DummyMessageAdapter extends AbstractMessageAdapter implements UMOStreamMessageAdapter
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

        public InputStream getInputStream()
        {
            return null;
        }

        public OutputStream getOutputStream()
        {
            return null;
        }

        public void write(UMOEvent event) throws IOException
        {
            // nothing to do
        }

        public OutputHandler getOutputHandler()
        {
            return null;
        }

        public void setOutputHandler(OutputHandler handler)
        {
            // nothing to do
        }

        public void release()
        {
            // nothing to do
        }
    }

}
