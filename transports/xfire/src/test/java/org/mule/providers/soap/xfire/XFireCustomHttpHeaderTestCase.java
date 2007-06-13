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

import org.mule.config.MuleProperties;
import org.mule.extras.client.MuleClient;
import org.mule.impl.internal.notifications.MessageNotification;
import org.mule.impl.internal.notifications.MessageNotificationListener;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;
import org.mule.umo.manager.UMOServerNotification;

import java.util.HashMap;

import junit.framework.Assert;

public class XFireCustomHttpHeaderTestCase extends FunctionalTestCase implements MessageNotificationListener
{
    protected static final String endpointAddress = "http://localhost:10181/services/TestComponent?method=onReceive";

    private UMOMessage notificationMsg = null;

    // @Override
    protected void doSetUp() throws Exception
    {
        managementContext.registerListener(this);
    }

    // @Override
    protected void doTearDown() throws Exception
    {
        managementContext.unregisterListener(this);
    }

    public void testXfire() throws Exception
    {
        Object payload = new Object[]{"Test String"};
        String myProperty = "myProperty";

        managementContext.registerListener(this);
        MuleClient client = new MuleClient();

        HashMap props = new HashMap();
        props.put(MuleProperties.MULE_USER_PROPERTY, "alan");
        props.put(MuleProperties.MULE_METHOD_PROPERTY, "sayHello");
        props.put(myProperty, myProperty);

        UMOMessage reply = client.send("xfire:" + endpointAddress, payload, props);

        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertEquals("Test String Received", reply.getPayloadAsString());

        // make sure all notifications have trickled in
        Thread.sleep(3000);

        // make sure we received a notification on xfire
        assertNotNull(notificationMsg);

        // MULE_USER should be allowed in
        assertEquals("alan", notificationMsg.getProperty(MuleProperties.MULE_USER_PROPERTY));

        // mule properties should be removed
        assertNull(notificationMsg.getProperty(MuleProperties.MULE_IGNORE_METHOD_PROPERTY));

        // custom properties should be allowed in
        assertEquals(myProperty, notificationMsg.getProperty(myProperty));
    }

    public void onNotification(UMOServerNotification notification)
    {
        if (notification instanceof MessageNotification)
        {
            String uri = ((MessageNotification)notification).getEndpoint().getEndpointURI().toString();
            if (endpointAddress.equals(uri))
            {
                notificationMsg = (UMOMessage)notification.getSource();
            }
        }
        else
        {
            Assert.fail("invalid notification: " + notification);
        }
    }

    protected String getConfigResources()
    {
        return "xfire-headers-conf.xml";
    }
}
