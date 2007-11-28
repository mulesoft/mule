/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ftp;

import org.mule.extras.client.MuleClient;
import org.mule.providers.ftp.server.NamedPayload;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;

public class FtpFunctionalTestCase extends AbstractFtpServerTestCase
{

    private static int PORT = 60198;

    public FtpFunctionalTestCase()
    {
        super(PORT);
    }

    protected String getConfigResources()
    {
        return "ftp-functional-test.xml";
    }

    public void testSendAndRequest() throws Exception
    {
        Map properties = new HashMap();
        MuleClient client = new MuleClient();
        client.dispatch("ftp://anonymous:email@localhost:" + PORT, TEST_MESSAGE, properties);
        NamedPayload payload = awaitUpload();
        assertNotNull(payload);
        assertEquals(TEST_MESSAGE, new String(payload.getPayload()));
        logger.info("received message OK!");
        UMOMessage retrieved = client.request("ftp://anonymous:email@localhost:" + PORT, getTimeout());
        assertNotNull(retrieved);
        assertEquals(retrieved.getPayloadAsString(), TEST_MESSAGE);
    }

}
