/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class FilterTest extends FunctionalTestCase
{
    public void testFilter() throws Exception
    {
        MuleClient client = new MuleClient();

        MuleMessage result = client.send("http://localhost:9002/bar/foo", "test", null);
        assertEquals("test received", result.getPayloadAsString());

        result = client.send("http://localhost:9002/baz", "test", null);
        assertEquals("test received", result.getPayloadAsString());

        result = client.send("http://localhost:9002/quo", "test", null);
        assertEquals("test", result.getPayloadAsString());
    }

    @Override
    protected String getConfigResources()
    {
        return "filter-conf.xml";
    }

}
