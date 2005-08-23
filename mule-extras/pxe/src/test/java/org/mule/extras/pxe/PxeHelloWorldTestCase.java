/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.pxe;

import org.dom4j.io.XMLWriter;
import org.mule.MuleManager;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.tck.NamedTestCase;
import org.mule.umo.UMOMessage;
import org.mule.util.ClassHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class PxeHelloWorldTestCase extends NamedTestCase
{
    public void setUp() throws Exception
    {
        if (MuleManager.isInstanciated()) {
            MuleManager.getInstance().dispose();
        }

        MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
        builder.configure("hello-pxe-mule-config.xml");
    }

    protected void tearDown() throws Exception
    {
        MuleManager.getInstance().dispose();
    }

    public void testMessageSend() throws Exception
    {
        MuleClient client = new MuleClient();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document soap = dbf.newDocumentBuilder().parse(
                ClassHelper.getResourceAsStream("helloRequest.soap", getClass()));
		Map props = new HashMap();
        UMOMessage result = client.send("vm://pxe.in", new DOMSource(soap.getDocumentElement()), props);
        assertNotNull(result);
        StringWriter w = new StringWriter();
        XMLWriter writer = new XMLWriter(w);
        writer.write((Element)result.getPayload());
        String xml = w.toString();
        System.out.println(xml);
        assertTrue(xml.indexOf("Hello World") > -1);
    }
}
