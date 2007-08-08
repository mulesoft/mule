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
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

import org.dom4j.Document;

public class XmlTransformerFunctionalTestCase extends AbstractXmlFunctionalTestCase
{

    public static final String SIMPLE_XML = "<parent><child name='poot'/></parent>";

    protected String getConfigResources()
    {
        return "xml/xml-transformer-functional-test.xml";
    }

    protected MuleClient send() throws UMOException
    {
        MuleClient client = new MuleClient();
        client.dispatch("xml-in", SIMPLE_XML, null);
        return client;
    }

    public void testXmlOut() throws UMOException
    {
        String xml = (String) receive(send(), "xml-out", String.class);
        assertEquals(SIMPLE_XML, xml);
    }

    public void testXmlDomOut() throws UMOException
    {
        Document dom = (Document) receive(send(), "xml-dom-out", Document.class);
    }

    public void testDomXmlOut() throws UMOException
    {
        String xml = (String) receive(send(), "dom-xml-out", String.class);
        assertEquals(SIMPLE_XML, xml);
    }

    protected Object receive(MuleClient client, String endpoint, Class clazz) throws UMOException
    {
        UMOMessage message = client.receive(endpoint, TIMEOUT);
        assertNotNull(message);
        assertNotNull(message.getPayload());
        assertTrue(message.getPayload().getClass().getName(), clazz.isAssignableFrom(message.getPayload().getClass()));
        return message.getPayload();
    }

}
