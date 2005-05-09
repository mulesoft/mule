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
package org.mule.test.util.queue;

import org.mule.impl.MuleMessage;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.util.queue.SerialisationPersistence;

import EDU.oswego.cs.dl.util.concurrent.BoundedBuffer;
import EDU.oswego.cs.dl.util.concurrent.BoundedChannel;

import com.mockobjects.dynamic.Mock;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class SerializationPeristenceTestCase extends AbstractMuleTestCase {
	
	public void testSerialization() throws Exception {
		SerialisationPersistence sp = new SerialisationPersistence();
		BoundedChannel bc = new BoundedBuffer();
		sp.initialise(bc, "myName");
		Mock mockEvent = getMockEvent();
		Mock mockEndpoint = getMockEndpoint();
		Mock mockEndpointUri = getMockEndpointURI();
		Mock mockMessage = new Mock(UMOMessage.class, "umoMessage");
		Mock mockComponent = new Mock(UMOComponent.class, "umoComponent");
		Mock mockDescriptor = getMockDescriptor();
		UMOEvent event = (UMOEvent) mockEvent.proxy();
		UMOEndpoint endpoint = (UMOEndpoint) mockEndpoint.proxy();
		mockEvent.expectAndReturn("getEndpoint", endpoint);
		mockEvent.expectAndReturn("getEndpoint", endpoint);
		mockEvent.expectAndReturn("getId", "myId");
		mockEvent.expectAndReturn("getId", "myId");
		mockEvent.expectAndReturn("getId", "myId");
		mockEvent.expectAndReturn("isSynchronous", true);
		mockEvent.expectAndReturn("isStopFurtherProcessing", true);
		mockEvent.expectAndReturn("getMessage", new MuleMessage("myMessage", null));
		mockEvent.expectAndReturn("getComponent", mockComponent.proxy());
		mockEvent.expectAndReturn("getTimeout", new Integer(1000));
		mockComponent.expectAndReturn("getDescriptor", mockDescriptor.proxy());
		mockDescriptor.expectAndReturn("getName", "myComponent");
		mockEndpoint.expectAndReturn("getEndpointURI", mockEndpointUri.proxy());
		mockEndpoint.expectAndReturn("getEndpointURI", mockEndpointUri.proxy());
		mockEndpoint.expectAndReturn("getName", "myEndpointName");
		mockEndpointUri.expectAndReturn("getAddress", "myAddress");
		sp.store("myName", event);
		sp.remove("myName", event);
	}

}
