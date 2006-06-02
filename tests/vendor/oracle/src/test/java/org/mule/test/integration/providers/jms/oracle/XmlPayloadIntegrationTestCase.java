package org.mule.test.integration.providers.jms.oracle;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
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
                     MuleUtil.receiveXmlMessageAsString(muleClient, TestConfig.QUEUE_XML));

        AQUtil.dropQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_XML, /*force*/false);
    }

    public void testLargeXmlMessage() throws Exception {
        AQUtil.createOrReplaceXmlQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_XML, false);

        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        final InputStream is = currentClassLoader.getResourceAsStream(TestConfig.XML_MESSAGE_FILE);
        assertNotNull("Test resource not found.", is);
        String xml = IOUtils.toString(is);

        MuleUtil.sendXmlMessageToQueue(muleClient, TestConfig.QUEUE_XML, xml);

        assertXMLEqual(xml, MuleUtil.receiveXmlMessageAsString(muleClient, TestConfig.QUEUE_XML));

        AQUtil.dropQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_XML, /*force*/false);
    }
}
