/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.impl.internal.notifications.MessageNotification;
import org.mule.impl.internal.notifications.MessageNotificationListener;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;
import org.mule.umo.manager.UMOServerNotification;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class XFireCustomHttpHeaderTestCase extends FunctionalTestCase implements MessageNotificationListener{

    private List notifications;
    private final String myProperty = new String("myProperty");

    public void testXfire() throws Exception
    {
        notifications = new Vector(5);

        MuleManager.getInstance().registerListener(this);
        MuleClient client = new MuleClient();
        HashMap props = new HashMap();
        props.put(MuleProperties.MULE_USER_PROPERTY, "alan");
        props.put(MuleProperties.MULE_METHOD_PROPERTY, "sayHello");
        props.put(myProperty, myProperty);

        UMOMessage reply = client.send("xfire:http://localhost:10181/services/TestComponent?method=onReceive", new MuleMessage(new Object[] {"Test String"}) , props);
        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertEquals(reply.getPayloadAsString(), "Test String Received");

        //should have received all notifications before the reply arrives back to the client
        UMOMessage msgNotification = (UMOMessage)notifications.get(0);

        //MULE_USER    should  be allowed in
        assertEquals(msgNotification.getProperty(MuleProperties.MULE_USER_PROPERTY), "alan");

        //mule properties should be removed
        assertNull(msgNotification.getProperty(MuleProperties.MULE_METHOD_PROPERTY));

        //custom properties should be allowed in
        assertEquals(msgNotification.getProperty(myProperty),myProperty);
    }

    public synchronized void onNotification(UMOServerNotification notification)
    {
        MessageNotification msgNotification = (MessageNotification)notification;
        UMOMessage msg = (UMOMessage)msgNotification.getSource();
        notifications.add(msg);
    }

    protected String getConfigResources() {

        return "mule-xfire-conf.xml";
    }

}
