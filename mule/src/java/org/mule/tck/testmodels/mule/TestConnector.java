/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.tck.testmodels.mule;

import org.mule.providers.AbstractConnector;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UMOMessageReceiver;

/**
 * <p>
 * <code>TestConnector</code> us a mock connector
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TestConnector extends AbstractConnector
{

    /**
     * 
     */
    public TestConnector()
    {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.AbstractConnector#doDispose()
     */
    protected void doDispose()
    {

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.AbstractConnector#doInitialise()
     */
    public void doInitialise() throws InitialisationException
    {

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnector#getProtocol()
     */
    public String getProtocol()
    {
        return "test";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.AbstractConnector#doStart()
     */
    protected void doStart() throws UMOException
    {

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.AbstractConnector#doStop()
     */
    protected void doStop() throws UMOException
    {

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnector#getMessageAdapter(java.lang.Object)
     */
    public UMOMessageAdapter getMessageAdapter(Object message) throws MessagingException
    {
        return new DummyMessageAdapter(message);
    }

    public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        UMOMessageReceiver receiver = new AbstractMessageReceiver(this, component, endpoint) {
            public void doConnect() throws Exception
            {
            }

            public void doDisconnect() throws Exception
            {
            }
        };
        return receiver;
    }

    public void destroyReceiver(UMOMessageReceiver receiver, UMOEndpoint endpoint) throws Exception
    {

    }

    public class DummyMessageAdapter extends AbstractMessageAdapter
    {
        private Object message = new String("DummyMessage");

        public DummyMessageAdapter(Object message)
        {
            this.message = message;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.mule.umo.provider.UMOMessageAdapter#getPayload()
         */
        public Object getPayload()
        {
            return message;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.mule.umo.provider.UMOMessageAdapter#getPayloadAsBytes()
         */
        public byte[] getPayloadAsBytes() throws Exception
        {

            return message.toString().getBytes();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.mule.umo.provider.UMOMessageAdapter#getPayloadAsString()
         */
        public String getPayloadAsString(String encoding) throws Exception
        {
            return message.toString();
        }
    }

}
