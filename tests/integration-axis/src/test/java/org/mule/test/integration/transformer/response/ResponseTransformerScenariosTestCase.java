/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.transformer.response;

import org.mule.tck.FunctionalTestCase;

public class ResponseTransformerScenariosTestCase extends FunctionalTestCase
{
    public ResponseTransformerScenariosTestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/transformer/response/response-transformer-scenarios.xml";
    }

    // ***** RESPONSE ENDPONTS ON INBOUND ENDPOINTS USED FOR SYNC RESPONSE AFTER ROUTING *****
    // Applied by DefaultInternalMessageListener

    // TODO Not sure how to implement with axis

    // public void testAxisSyncResponseTransformer() throws Exception
    // {
    // MuleClient client = new MuleClient();
    // MuleMessage message = client.send("axis:http://localhost:4445/services/AxisSync?method=echo",
    // "request",
    // null);
    // assertNotNull(message);
    // assertEquals("request" + "customResponse", message.getPayloadAsString());
    // }
}
