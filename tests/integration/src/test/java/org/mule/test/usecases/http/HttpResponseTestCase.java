/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MPL style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases.http;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class HttpResponseTestCase extends FunctionalTestCase
{
    public void testPayloadIsNotEmptyNoRemoteSynch() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage reply = client.send("http://localhost:8999", new MuleMessage("test"));
        assertNotNull(reply.getPayload());
        assertTrue(reply.getPayloadAsString().equals("test"));
    }

    public void testPayloadIsNotEmptyWithRemoteSynch() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage reply = client.send("http://localhost:8989", new MuleMessage("test"));
        assertNotNull(reply.getPayload());
        assertTrue(reply.getPayloadAsString().equals("test"));
    }

    protected String getConfigResources()
    {
        return "org/mule/test/usecases/http/http-response.xml";
    }
}
