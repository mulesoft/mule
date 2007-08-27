/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.jms.oracle;

import java.io.InputStream;

import org.mule.util.IOUtils;
import org.mule.test.integration.providers.jms.oracle.util.AQUtil;
import org.mule.test.integration.providers.jms.oracle.util.MuleUtil;

/**
 * Tests the connector against a live Oracle database using native XML messages.
 *
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 * @see <a href="http://www.lc.leidenuniv.nl/awcourse/oracle/appdev.920/a96620/toc.htm">Oracle9i XML Database Developer's Guide - Oracle XML DB</a>
 */
public class XmlPayloadIntegrationTestCase extends AbstractIntegrationTestCase {

    protected String getConfigurationFiles() {
        return "jms-connector-config.xml, xml-transformers-config.xml";
    }

    public void testSmallXmlMessage() throws Exception {
        AQUtil.createOrReplaceXmlQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_XML, false);

        MuleUtil.sendXmlMessageToQueue(muleClient, TestConfig.QUEUE_XML, TestConfig.XML_MESSAGE);
        assertXMLEqual(TestConfig.XML_MESSAGE,
                     MuleUtil.receiveXmlMessageAsString(muleClient, TestConfig.QUEUE_XML, 2000));

        AQUtil.dropQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_XML, /*force*/false);
    }

    public void testLargeXmlMessage() throws Exception {
        AQUtil.createOrReplaceXmlQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_XML, false);

        InputStream is = IOUtils.getResourceAsStream(TestConfig.XML_MESSAGE_FILE, getClass());
        assertNotNull("Test resource not found.", is);
        String xml = IOUtils.toString(is);

        MuleUtil.sendXmlMessageToQueue(muleClient, TestConfig.QUEUE_XML, xml);

        assertXMLEqual(xml, MuleUtil.receiveXmlMessageAsString(muleClient, TestConfig.QUEUE_XML, 2000));

        AQUtil.dropQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_XML, /*force*/false);
    }

    public void testI18NXmlMessage() throws Exception {
        AQUtil.createOrReplaceXmlQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_XML, false);

        MuleUtil.sendXmlMessageToQueue(muleClient, TestConfig.QUEUE_XML, TestConfig.I18N_MESSAGE);
        assertXMLEqual(TestConfig.I18N_MESSAGE,
                     MuleUtil.receiveXmlMessageAsString(muleClient, TestConfig.QUEUE_XML, 2000));

        AQUtil.dropQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_XML, /*force*/false);
    }
}
