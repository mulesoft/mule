/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.xml.functional;

import org.mule.extras.client.MuleClient;
import org.mule.umo.UMOMessage;

import java.util.Random;

public class XmlFilterFunctionalTestCase extends AbstractXmlFunctionalTestCase
{

    public static final int MAX_COUNT = 100;
    public static final String STRING_MESSAGE = "Hello world";

    protected String getConfigResources()
    {
        return "xml/xml-filter-functional-test.xml";
    }

    public void testNotXml() throws Exception
    {
        logger.debug("not xml");
        MuleClient client = new MuleClient();
        client.dispatch("in", STRING_MESSAGE, null);
        UMOMessage response = client.request("notxml", TIMEOUT);
        assertNotNull(response);
        assertNotNull(response.getPayload());
        assertEquals(STRING_MESSAGE, response.getPayloadAsString());
    }

    public void testOther() throws Exception
    {
        logger.debug("other");
        doTestXml("other", getResourceAsString("issues/many-sends-mule-1758-test.xml"));
    }

    public void testSelf() throws Exception
    {
        logger.debug("self");
        doTestXml("self", getConfigAsString());
    }

    public void doTestXml(String endpoint, String xml) throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("in", xml, null);
        UMOMessage response = client.request(endpoint, TIMEOUT);
        assertNotNull(response);
        assertNotNull(response.getPayload());
        assertEquals(xml, response.getPayloadAsString());
    }

    public void testMany() throws Exception
    {
        Random random = new Random();
        for (int i = 0; i < MAX_COUNT; ++i)
        {
            switch (random.nextInt(3))
            {
            case 0:
                testNotXml();
                break;
            case 1:
                testOther();
                break;
            case 2:
                testSelf();
                break;
            default:
                throw new IllegalStateException("Bad case");
            }
        }
    }

}
