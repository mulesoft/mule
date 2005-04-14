/*
 * $Header$ $Revision$ $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *
 */

package org.mule.providers.vm;

import org.mule.tck.providers.AbstractMessageReceiverTestCase;
import org.mule.umo.provider.UMOMessageReceiver;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class VMMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{

    VMMessageReceiver receiver;

    protected void setUp() throws Exception
    {
        super.setUp();
        receiver = new VMMessageReceiver(connector, component, endpoint, null);
    }

//    public void testSyncReceive() throws Exception
//    {
//        doReceiveTest(true);
//    }
//
//    public void testAsyncReceive() throws Exception
//    {
//        doReceiveTest(false);
//    }
//
//    public void doReceiveTest(boolean synchronous) throws Exception
//    {
//        MuleManager.getConfiguration().setSynchronous(synchronous);
//        Mock mockComponent = new Mock(UMOComponent.class);
//        Mock mockConnector = new Mock(UMOConnector.class);
//        Mock mockConnector2 = new Mock(UMOConnector.class, "mockConnector2");
//        Mock mockDispatcher = new Mock(UMOMessageDispatcher.class);
//        Mock mockProviderDe = new Mock(UMOMessageDispatcher.class);
//        Mock mockSession = new Mock(UMOSession.class);
//        Mock mockEvent = getMockEvent();
//
//        UMODescriptor descriptor = getTestDescriptor("XsltTransaformerTestCase", "Bla");
//        mockConnector.expectAndReturn("getMessageAdapter", C.ANY_ARGS, new VMMessageAdapter(new MuleMessage("blah", null)));
//
//        mockConnector2.expectAndReturn("getDispatcher", (UMOMessageDispatcher)mockDispatcher.proxy());
//
//        //The routeMessage method needs the descriptor to obtain a refernece to the
//        //inbound router
//        mockComponent.expectAndReturn("getDescriptor", descriptor);
//
//        UMOEndpoint endpoint = getTestEndpoint("XsltTransaformerTestCase", UMOImmutableEndpoint.PROVIDER_TYPE_SENDER);
//        UMOEndpoint endpoint2 = getTestEndpoint("test2", UMOImmutableEndpoint.PROVIDER_TYPE_RECEIVER);
//
//        //the inbound router will need the inbound endpoint set to route the event
//        descriptor.setInboundEndpoint(provider2);
//
//        endpoint.setConnector((UMOConnector)mockConnector2.proxy());
//
//        //sending the event synchronously
//        mockEvent.expectAndReturn("isSynchronous", synchronous);
//        mockEvent.expectAndReturn("getParams", null);
//
//        //The transformed message will be accessed when the event is dispatched to the next component
//        mockEvent.expectAndReturn("getTransformedMessage", "testMessage");
//
//        //need to relax the event arg because a new event with a uid is created on dispatch or send
//        if (synchronous)
//        {
//            mockDispatcher.expect("sendEvent", C.isA(UMOEvent.class));
//        }
//        else
//        {
//            mockDispatcher.expect("dispatchEvent", C.isA(UMOEvent.class));
//        }
//
//        receiver.create((UMOConnector) mockConnector.proxy(), (UMOComponent) mockComponent.proxy(), endpoint);
//        if (synchronous)
//        {
//            receiver.onCall((UMOEvent) mockEvent.proxy());
//        }
//        else
//        {
//            receiver.onEvent((UMOEvent) mockEvent.proxy());
//        }
//
//        mockSession.verify();
//        mockEvent.verify();
//        mockComponent.verify();
//        mockConnector.verify();
//    }

    /*
	 * (non-Javadoc)
	 *
	 * @see org.mule.tck.providers.AbstractMessageReceiverTestCase#getMessageReceiver()
	 */
    public UMOMessageReceiver getMessageReceiver()
    {
        return receiver;
    }
}
