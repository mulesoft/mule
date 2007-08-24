/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases.sync;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class TcpToFileTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/usecases/sync/tcp-to-file.xml";
    }

    public void testSyncResponse() throws Exception
    {
        MuleClient client = new MuleClient();
        String payload = "payload";

        client.sendNoReceive("tcp://localhost:4444", payload, null);
        // let Mule carry the payload
        Thread.sleep(1000);

        UMOMessage msg = client.receive("file://temp/tests/mule", 5000);
        assertNotNull(msg);
        assertEquals(payload, msg.getPayload());
    }
}
