/*
 * $Id:StaticRecipientListRouterTestCase.java 5937 2007-04-09 22:35:04Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.impl.MuleMessage;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.RoutingException;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.List;

public class StaticRecipientListRouterTestCase extends AbstractMuleTestCase
{
    public void testRecipientListRouter() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        UMOEndpoint endpoint1 = getTestEndpoint("Test1Provider", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        assertNotNull(endpoint1);

        List recipients = new ArrayList();
        recipients.add("test://recipient1");
        recipients.add("test://recipient2");
        StaticRecipientList router = new StaticRecipientList();
        router.setRecipients(recipients);

        List endpoints = new ArrayList();
        endpoints.add(endpoint1);
        router.setEndpoints(endpoints);
        router.setManagementContext(managementContext);

        assertEquals(2, router.getRecipients().size());

        UMOMessage message = new MuleMessage("test event");
        assertTrue(router.isMatch(message));
        // note this router clones endpoints so that the endpointUri can be
        // changed

        // The static recipient list router duplicates the message for each endpoint
        // so we can't
        // check for equality on the arguments passed to the dispatch / send methods
        session.expect("dispatchEvent", C.args(C.isA(UMOMessage.class), C.isA(UMOEndpoint.class)));
        session.expect("dispatchEvent", C.args(C.isA(UMOMessage.class), C.isA(UMOEndpoint.class)));
        router.route(message, (UMOSession)session.proxy(), false);
        session.verify();

        message = new MuleMessage("test event");
        router.getRecipients().add("test://recipient3");
        session.expectAndReturn("sendEvent", C.args(C.isA(UMOMessage.class), C.isA(UMOEndpoint.class)),
            message);
        session.expectAndReturn("sendEvent", C.args(C.isA(UMOMessage.class), C.isA(UMOEndpoint.class)),
            message);
        session.expectAndReturn("sendEvent", C.args(C.isA(UMOMessage.class), C.isA(UMOEndpoint.class)),
            message);
        UMOMessage result = router.route(message, (UMOSession)session.proxy(), true);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof List);
        assertEquals(3, ((List)result.getPayload()).size());
        session.verify();

    }

    public void testBadRecipientListRouter() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();

        UMOEndpoint endpoint1 = getTestEndpoint("Test1Provider", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        assertNotNull(endpoint1);

        List recipients = new ArrayList();
        recipients.add("malformed-endpointUri-recipient1");
        StaticRecipientList router = new StaticRecipientList();
        router.setRecipients(recipients);

        List endpoints = new ArrayList();
        endpoints.add(endpoint1);
        router.setEndpoints(endpoints);

        assertEquals(1, router.getRecipients().size());

        UMOMessage message = new MuleMessage("test event");
        assertTrue(router.isMatch(message));
        try
        {
            router.route(message, (UMOSession)session.proxy(), false);
            fail("Should not allow malformed endpointUri");
        }
        catch (RoutingException e)
        {
            // ignore
        }
        session.verify();
    }

}
