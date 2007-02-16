/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.routing.outbound;

import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.routing.outbound.FilteringListMessageSplitter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;

import com.mockobjects.constraint.Constraint;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class FilterListMessageSplitterRouterTestCase extends AbstractMuleTestCase
{
    public void testMessageSpltterRouter() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();

        UMOEndpoint endpoint1 = getTestEndpoint("Test1endpoint", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint1.setEndpointURI(new MuleEndpointURI("test://endpointUri.1"));
        endpoint1.setFilter(new PayloadTypeFilter(Apple.class));
        UMOEndpoint endpoint2 = getTestEndpoint("Test2Endpoint", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint2.setEndpointURI(new MuleEndpointURI("test://endpointUri.2"));
        endpoint2.setFilter(new PayloadTypeFilter(Orange.class));
        UMOEndpoint endpoint3 = getTestEndpoint("Test3Endpoint", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint3.setEndpointURI(new MuleEndpointURI("test://endpointUri.3"));

        FilteringListMessageSplitter router = new FilteringListMessageSplitter();
        router.setFilter(new PayloadTypeFilter(List.class));
        router.addEndpoint(endpoint1);
        router.addEndpoint(endpoint2);
        router.addEndpoint(endpoint3);

        List payload = new ArrayList();
        payload.add(new Apple());
        payload.add(new Apple());
        payload.add(new Orange());
        payload.add(new String());
        UMOMessage message = new MuleMessage(payload);

        assertTrue(router.isMatch(message));
        session.expect("dispatchEvent", C.args(new PayloadConstraint(Apple.class), C.eq(endpoint1)));
        session.expect("dispatchEvent", C.args(new PayloadConstraint(Apple.class), C.eq(endpoint1)));
        session.expect("dispatchEvent", C.args(new PayloadConstraint(Orange.class), C.eq(endpoint2)));
        session.expect("dispatchEvent", C.args(new PayloadConstraint(String.class), C.eq(endpoint3)));
        router.route(message, (UMOSession)session.proxy(), false);
        session.verify();

        message = new MuleMessage(payload);

        session.expectAndReturn("sendEvent", C.args(new PayloadConstraint(Apple.class), C.eq(endpoint1)),
            message);
        session.expectAndReturn("sendEvent", C.args(new PayloadConstraint(Apple.class), C.eq(endpoint1)),
            message);
        session.expectAndReturn("sendEvent", C.args(new PayloadConstraint(Orange.class), C.eq(endpoint2)),
            message);
        session.expectAndReturn("sendEvent", C.args(new PayloadConstraint(String.class), C.eq(endpoint3)),
            message);
        UMOMessage result = router.route(message, (UMOSession)session.proxy(), true);
        assertNotNull(result);
        assertEquals(message, result);
        session.verify();
    }

    private class PayloadConstraint implements Constraint
    {
        private Class type;

        public PayloadConstraint(Class type)
        {
            this.type = type;
        }

        public boolean eval(Object o)
        {
            return ((UMOMessage)o).getPayload().getClass().equals(type);
        }
    }
}
