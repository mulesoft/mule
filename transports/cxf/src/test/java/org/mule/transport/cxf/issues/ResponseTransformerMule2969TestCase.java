/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.issues;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.StringAppendTestTransformer;

public class ResponseTransformerMule2969TestCase extends FunctionalTestCase
{
    
    protected String getConfigResources()
    {
        return "response-transformer-mule2969.xml";
    }

    public void testSyncResponse() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("cxf:http://localhost:4444/services/CXFService?method=echo", "request",
            null);
        assertNotNull(message);
        assertEquals("request" + StringAppendTestTransformer.DEFAULT_TEXT, message.getPayloadAsString());
    }
}
