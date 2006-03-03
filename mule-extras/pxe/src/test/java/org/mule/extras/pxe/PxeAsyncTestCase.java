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

import com.fs.utils.DOMUtils;

import org.mule.MuleManager;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOMessage;
import org.mule.util.ClassHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class PxeAsyncTestCase extends AbstractMuleTestCase
{
    public void doSetUp() throws Exception
    {
        if (MuleManager.isInstanciated()) {
            MuleManager.getInstance().dispose();
        }

        MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
        builder.configure("async-pxe-mule-config.xml");
    }

    protected void doTearDown() throws Exception
    {
        MuleManager.getInstance().dispose();
    }

    public void testMessageSyncSend() throws Exception
    {
        MuleClient client = new MuleClient();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document soap = dbf.newDocumentBuilder().parse(
                ClassHelper.getResourceAsStream("asyncRequest.xml", getClass()));
		Map props = new HashMap();
        UMOMessage result = client.sendDirect("pxe", null, new DOMSource(soap.getDocumentElement()), props);
        assertNotNull(result);
        String xml = DOMUtils.domToString(((Element)result.getPayload()));
        System.out.println(xml);
        //todo apart from there being no exception how do I verify the result??
    }

    public void testMessageAsyncSend() throws Exception
    {
        MuleClient client = new MuleClient();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document soap = dbf.newDocumentBuilder().parse(
                ClassHelper.getResourceAsStream("asyncRequest.xml", getClass()));
		Map props = new HashMap();
        client.dispatchDirect("pxe", new DOMSource(soap.getDocumentElement()), props);
        //todo apart from there being no exception how do I verify the result??
    }
}
