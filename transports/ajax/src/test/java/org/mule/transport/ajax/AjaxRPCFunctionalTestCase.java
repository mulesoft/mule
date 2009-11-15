/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ajax;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.util.Map;

public class AjaxRPCFunctionalTestCase extends FunctionalTestCase
{
    public static final String TEST_JSON_MESSAGE = "{\"payload\" : {\"value1\" : \"foo\", \"value2\" : \"bar\"}, \"replyTo\" : \"/service/response\"}";
    @Override
    protected String getConfigResources()
    {
        return "ajax-rpc-test.xml";
    }

    public void testDispatchReceiveSimple() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("endpoint1", TEST_JSON_MESSAGE, null);

        MuleMessage result = client.request("result", 5000L);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Map);
        assertEquals("foo", ((Map)result.getPayload()).get("value1"));
        assertEquals("bar", ((Map)result.getPayload()).get("value2"));
    }
}