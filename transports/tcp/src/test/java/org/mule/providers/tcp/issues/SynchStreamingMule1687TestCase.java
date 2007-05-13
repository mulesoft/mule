/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.issues;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.HashMap;


public class SynchStreamingMule1687TestCase extends FunctionalTestCase
{

    public static final String TEST_MESSAGE = "Test TCP Request";

    public SynchStreamingMule1687TestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "tcp-synch-streaming-test.xml";
    }

    public void testSendAndReceive() throws Exception
    {
        MuleClient client = new MuleClient();

        UMOMessage message = client.send("tcp://localhost:65432", TEST_MESSAGE, new HashMap());

        assertNotNull(message);
    }

}

