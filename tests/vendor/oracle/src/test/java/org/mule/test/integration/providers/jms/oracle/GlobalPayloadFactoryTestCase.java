/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
public class GlobalPayloadFactoryTestCase extends AbstractIntegrationTestCase {

    protected String getConfigurationFiles() {
        return "jms-connector-xmlpayload-config.xml, xml-transformers-config.xml";
    }

    public synchronized void testGlobalPayloadFactoryProperty() throws Exception {
        AQUtil.createOrReplaceXmlQueue(jmsSession,  jmsConnector.getUsername(), TestConfig.QUEUE_XML, false);

        MuleUtil.sendXmlMessageToQueue(muleClient, TestConfig.QUEUE_XML, TestConfig.XML_MESSAGE);
        wait(2000);
        assertXMLEqual(TestConfig.XML_MESSAGE,
                MuleUtil.receiveXmlMessageAsString(muleClient, TestConfig.QUEUE_XML, 2000));

        AQUtil.dropQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_XML, /*force*/false);
    }
}
