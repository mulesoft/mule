package org.mule.test.integration.providers.jms.oracle;

import org.mule.providers.oracle.jms.util.AQUtil;
import org.mule.providers.oracle.jms.util.MuleUtil;
import org.mule.providers.oracle.jms.util.Util;

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
	    AQUtil.createOrReplaceXmlQueue(session, connector.getUsername(), TestConfig.QUEUE_XML);

	    MuleUtil.sendXmlMessageToQueue(TestConfig.QUEUE_XML, TestConfig.XML_MESSAGE);
        assertXMLEqual(TestConfig.XML_MESSAGE, 
        			 MuleUtil.receiveXmlMessageAsString(TestConfig.QUEUE_XML));

        AQUtil.dropQueue(session, connector.getUsername(), TestConfig.QUEUE_XML, /*force*/false);
    }

    public void testLargeXmlMessage() throws Exception {
	    AQUtil.createOrReplaceXmlQueue(session, connector.getUsername(), TestConfig.QUEUE_XML);
		
    	String xml = Util.getResourceAsString(TestConfig.XML_MESSAGE_FILE, getClass());
		MuleUtil.sendXmlMessageToQueue(TestConfig.QUEUE_XML, xml);
    	
		assertXMLEqual(xml, MuleUtil.receiveXmlMessageAsString(TestConfig.QUEUE_XML));
		
        AQUtil.dropQueue(session, connector.getUsername(), TestConfig.QUEUE_XML, /*force*/false);
    }
}
