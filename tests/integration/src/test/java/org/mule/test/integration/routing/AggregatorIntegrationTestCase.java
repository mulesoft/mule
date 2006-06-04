/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.routing;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class AggregatorIntegrationTestCase extends FunctionalTestCase
{
    protected String getConfigResources() {
        return "org/mule/test/integration/routing/test-correlation-aggregator.xml";
    }

    public void testAggregator() throws UMOException
    {
        String message = "test";
        MuleClient client = new MuleClient();
        UMOMessage result = client.send("vm://distributor.queue", message, null);
        assertNotNull(result);
        assertEquals(message + message, result.getPayload());
    }
}
