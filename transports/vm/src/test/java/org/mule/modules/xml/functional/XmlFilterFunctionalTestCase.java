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
import org.mule.umo.UMOMessage;

public class XmlFilterFunctionalTestCase extends AbstractXmlFunctionalTestCase
{

    public static final String STRING_MESSAGE = "Hello world";

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
        doTestXml("other", getResourceAsString("issues/many-sends-mule-1758-test.xml"));
    }

    public void testSelf() throws Exception
    {
        doTestXml("self", getConfigAsString());
    }

    public void doTestXml(String endpoint, String xml) throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("in", xml, null);
        UMOMessage response = client.receive(endpoint, TIMEOUT);
        assertNotNull(response);
        assertNotNull(response.getPayload());
        assertEquals(xml, response.getPayloadAsString());
    }

}
