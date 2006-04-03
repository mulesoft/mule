package org.mule.test.integration.providers.jms.oracle;

import org.mule.test.integration.providers.jms.oracle.util.AQUtil;
import org.mule.test.integration.providers.jms.oracle.util.MuleUtil;

/**
 * Tests the {@code payloadFactory} property when set globally for the connector
 * (instead of per endpoint).
 *
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
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
                muleClient.receive("jms://" + TestConfig.QUEUE_XML, "JMSMessageToObject", MuleUtil.MULE_RECEIVE_TIMEOUT).getPayloadAsString());

        AQUtil.dropQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_XML, /*force*/false);
    }
}
