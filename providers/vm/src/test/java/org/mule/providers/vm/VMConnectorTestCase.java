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

package org.mule.providers.vm;

import org.mule.impl.MuleMessage;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class VMConnectorTestCase extends AbstractConnectorTestCase
{

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractConnectorTestCase#createConnector()
     */
    public UMOConnector getConnector() throws Exception
    {
        VMConnector conn = new VMConnector();
        conn.setName("TestVM");
        conn.initialise();
        return conn;
    }

    public String getTestEndpointURI()
    {
        return "vm://test.queue";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractConnectorTestCase#testDispatch()
     */
    public void testDispatch() throws Exception
    {
        // MuleManager.getConfiguration().setSynchronous(false);
        // UMOConnector connector = getConnector();
        // Mock mockComponent = new Mock(UMOComponent.class);
        // UMOEndpoint= getTestEndpoint("testProvider",
        // UMOEndpointTYPE_RECEIVER);
        // UMOEndpoint = getTestEndpoint("testProvider2",
        // UMOEndpointTYPE_RECEIVER);
        // descriptor.setInboundEndpoint(provider2);
        // UMOEvent event = getTestEvent(getValidMessage(), endpoint);
        // mockComponent.expectAndReturn("getDescriptor", descriptor);
        // event.setSynchronous(false);
        // //need to relax this arg because a new event with a uid is create on
        // dispatch
        // mockComponent.expect("dispatchEvent", C.isA(UMOEvent.class));
        // //mockComponent.expectAndReturn("getDescriptor", descriptor);
        //
        // connector.registerListener((UMOComponent) mockComponent.proxy(),
        // endpoint);
        // connector.start();
        // connector.getDispatcher("dummy").dispatch(event);
        //
        // mockComponent.verify();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractConnectorTestCase#testSend()
     */
    public void testSend() throws Exception
    {
        // MuleManager.getConfiguration().setSynchronous(true);
        // UMOConnector connector = getConnector();
        // Mock mockComponent = new Mock(UMOComponent.class);
        // UMOEndpoint= getTestEndpoint("testProvider",
        // UMOEndpointTYPE_RECEIVER);
        // UMOEndpoint = getTestEndpoint("testProvider2",
        // UMOEndpointTYPE_RECEIVER);
        // descriptor.setInboundEndpoint(provider2);
        // UMOEvent event = getTestEvent(getValidMessage(), endpoint);
        // mockComponent.expectAndReturn("getDescriptors", descriptor);
        //
        // event.setSynchronous(true);
        // //need to relax this arg because a new event with a uid is create on
        // dispatch
        // mockComponent.expect("sendEvent", C.isA(UMOEvent.class));
        // mockComponent.expectAndReturn("getDescriptor", descriptor);
        //
        // connector.registerListener((UMOComponent) mockComponent.proxy(),
        // endpoint);
        // connector.start();
        // connector.getDispatcher("dummy").send(event);
        //
        // mockComponent.verify();
    }

    public Object getValidMessage() throws Exception
    {
        return new MuleMessage("TestMessage");
    }
}
