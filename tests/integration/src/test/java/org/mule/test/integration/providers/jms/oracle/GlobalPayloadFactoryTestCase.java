package org.mule.test.integration.providers.jms.oracle;

import java.io.IOException;

import javax.jms.JMSException;
import javax.xml.parsers.ParserConfigurationException;

import oracle.AQ.AQException;

import org.mule.umo.UMOException;
import org.mule.test.integration.providers.jms.oracle.util.AQUtil;
import org.mule.test.integration.providers.jms.oracle.util.MuleUtil;
import org.xml.sax.SAXException;

import org.mule.providers.oracle.jms.*;

/**
 * Tests the {@code payloadFactory} property when set globally for the connector 
 * (instead of per endpoint).
 * 
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class GlobalPayloadFactoryTestCase extends AbstractIntegrationTestCase {
    
    public void testGlobalPayloadFactoryProperty() throws JMSException, AQException, UMOException, SAXException, IOException, ParserConfigurationException {
	    AQUtil.createOrReplaceXmlQueue(session, connector.getUsername(), TestConfig.QUEUE_XML);

	    MuleUtil.sendXmlMessageToQueue(TestConfig.QUEUE_XML, TestConfig.XML_MESSAGE);
        assertXMLEqual(TestConfig.XML_MESSAGE, 
        		(String) MuleUtil.receiveMessage("jms://" + TestConfig.QUEUE_XML, "XMLMessageToString"));

        AQUtil.dropQueue(session, connector.getUsername(), TestConfig.QUEUE_XML, /*force*/false);
    }
    
    protected String getServerConfiguration() {
    	return 
    	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<!DOCTYPE mule-configuration PUBLIC \"-//SymphonySoft //DTD mule-configuration XML V1.0//EN\" \"http://www.symphonysoft.com/dtds/mule/mule-configuration.dtd\">" +
    	"<mule-configuration id=\"TestConfiguration\" version=\"1.0\">" +
    	"<connector name=\"TestConnector\" className=\"org.mule.providers.oracle.jms.OracleJmsConnector\">" +
    	    "<properties>" +
    			"<property name=\"url\" value=\"" + TestConfig.DB_URL + "\" />" + 
    	        "<property name=\"username\" value=\"" + TestConfig.DB_USER + "\" />" +
    	        "<property name=\"password\" value=\"" + TestConfig.DB_PASSWORD + "\" />" +
    			"<property name=\"" + OracleJmsConnector.PAYLOADFACTORY_PROPERTY + "\" value=\"oracle.xdb.XMLTypeFactory\" />" +
    	    "</properties>" +
    	"</connector>" +
        "<transformers>" +
            "<transformer name=\"ObjectToJMSMessage\" className=\"org.mule.providers.jms.transformers.ObjectToJMSMessage\" returnClass=\"javax.jms.Message\" />" +
            "<transformer name=\"JMSMessageToObject\" className=\"org.mule.providers.jms.transformers.JMSMessageToObject\" returnClass=\"java.lang.Object\" />" +
            "<transformer name=\"StringToXMLMessage\" className=\"org.mule.providers.oracle.jms.transformers.StringToXMLMessage\" returnClass=\"oracle.jms.AdtMessage\" />" +
            "<transformer name=\"XMLMessageToString\" className=\"org.mule.providers.oracle.jms.transformers.XMLMessageToString\" returnClass=\"java.lang.String\" />" +
            "<transformer name=\"XMLMessageToDOM\" className=\"org.mule.providers.oracle.jms.transformers.XMLMessageToDOM\" returnClass=\"org.w3c.dom.Document\" />" +
            "<transformer name=\"XMLMessageToStream\" className=\"org.mule.providers.oracle.jms.transformers.XMLMessageToStream\" returnClass=\"java.io.InputStream\" />" +
        "</transformers>" +
        "</mule-configuration>";
    }
}
