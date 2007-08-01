/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.xml.functional;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;

public class XmlFilterFunctionalTestCase extends FunctionalTestCase
{

    public static final String STRING_MESSAGE = "Hello world";
    public static final long TIMEOUT = 1000L;

    protected String getConfigResources()
    {
        return "xml/xml-filter-functional-test.xml";
    }

    public void testNotXml() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("in", STRING_MESSAGE, null);
        UMOMessage response = client.receive("notxml", TIMEOUT);
        assertNotNull(response);
        assertNotNull(response.getPayload());
        assertEquals(STRING_MESSAGE, response.getPayloadAsString());
    }

    public void testOther() throws Exception
    {
        doTestXml("other", "vm/many-sends-test.xml");
    }

    public void testSelf() throws Exception
    {
        doTestXml("self", getConfigResources());
    }

    public void doTestXml(String endpoint, String resource) throws Exception
    {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        assertNotNull(resource, is);
        String xml = IOUtils.toString(is);
        MuleClient client = new MuleClient();
        client.dispatch("in", xml, null);
        UMOMessage response = client.receive(endpoint, TIMEOUT);
        assertNotNull(response);
        assertNotNull(response.getPayload());
        assertEquals(xml, response.getPayloadAsString());
    }

}
