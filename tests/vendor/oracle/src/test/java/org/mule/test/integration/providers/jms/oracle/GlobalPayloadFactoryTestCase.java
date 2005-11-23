package org.mule.test.integration.providers.jms.oracle;

import java.io.IOException;

import javax.jms.JMSException;
import javax.xml.parsers.ParserConfigurationException;

import oracle.AQ.AQException;

import org.mule.providers.oracle.jms.util.AQUtil;
import org.mule.providers.oracle.jms.util.MuleUtil;
import org.mule.umo.UMOException;
import org.xml.sax.SAXException;

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

    public void testGlobalPayloadFactoryProperty() throws JMSException, AQException, UMOException, SAXException, IOException, ParserConfigurationException {
	    AQUtil.createOrReplaceXmlQueue(session, connector.getUsername(), TestConfig.QUEUE_XML);

	    MuleUtil.sendXmlMessageToQueue(TestConfig.QUEUE_XML, TestConfig.XML_MESSAGE);
        assertXMLEqual(TestConfig.XML_MESSAGE, 
        		(String) MuleUtil.receiveMessage("jms://" + TestConfig.QUEUE_XML, "XMLMessageToString"));

        AQUtil.dropQueue(session, connector.getUsername(), TestConfig.QUEUE_XML, /*force*/false);
    }
}
