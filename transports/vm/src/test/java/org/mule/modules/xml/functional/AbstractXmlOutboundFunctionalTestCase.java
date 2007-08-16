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
import org.mule.umo.UMOException;

import java.io.IOException;

import org.dom4j.Document;
import org.dom4j.Element;

public abstract class AbstractXmlOutboundFunctionalTestCase extends AbstractXmlFunctionalTestCase
{

    public static final String SERVICE_SPLITTER = "service splitter";
    public static final String ROUND_ROBIN_DET = "round robin deterministic";
    public static final String ROUND_ROBIN_INDET = "round robin indeterministic";
    public static final String SPLITTER_ENDPOINT_PREFIX = "service";
    public static final String ROUND_ROBIN_ENDPOINT_PREFIX = "robin";

    protected String getConfigResources()
    {
        return "xml/xml-outbound-functional-test.xml";
    }

    protected void doSend(String endpoint) throws IOException, UMOException
    {
        String xml = getConfigAsString();
        MuleClient client = new MuleClient();
        client.dispatch(endpoint, xml, null);
    }

    protected void assertService(String prefix, int index, String service) throws UMOException, IOException
    {
        MuleClient client = new MuleClient();
        UMOMessage response = client.receive(prefix + index, TIMEOUT);
        assertNotNull(response);
        assertNotNull(response.getPayload());
        assertTrue(response.getPayload().getClass().getName(), response.getPayload() instanceof Document);
        Document document = (Document) response.getPayload();
        assertEquals("service", document.getRootElement().getName());
        Element element = document.getRootElement();
        assertEquals(service, element.attributeValue("name"));
    }

}