/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.impl.MuleMessage;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;

import java.util.ArrayList;
import java.util.List;

public class FilteringListMessageSplitterTestCase extends AbstractMuleTestCase
{
    public void testCorrelationGroupSizePropertySet() throws Exception
    {
        UMOComponent testComponent = getTestComponent(getTestDescriptor("test", Apple.class.getName()));
        UMOSession session = getTestSession(testComponent);

        UMOEndpoint endpoint = getTestEndpoint("Test1Provider", UMOEndpoint.ENDPOINT_TYPE_SENDER);

        FilteringListMessageSplitter router = new FilteringListMessageSplitter();
        router.setFilter(null);
        router.addEndpoint(endpoint);

        List payload = new ArrayList();
        payload.add("one");
        payload.add("two");
        payload.add("three");
        payload.add("four");

        UMOMessage message = new MuleMessage(payload);

        UMOMessage result = router.route(message, session, true);
        assertNotNull(result);

        assertEquals("Correlation group size has not been set.", 4, result.getCorrelationGroupSize());
    }
}
