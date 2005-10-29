package org.mule.test.integration.providers.jms.oracle;

import javax.jms.JMSException;

import oracle.AQ.AQException;

import org.mule.umo.UMOException;
import org.mule.test.integration.providers.jms.oracle.util.AQUtil;
import org.mule.test.integration.providers.jms.oracle.util.MuleUtil;

/**
 * Tests the connector against a live Oracle database using standard JMS messages.
 * 
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class StandardPayloadIntegrationTestCase extends AbstractIntegrationTestCase {
    
    public void testCreateAndDropQueue() throws AQException, JMSException {	    
	   AQUtil.createOrReplaceQueue(session, connector.getUsername(), TestConfig.QUEUE_RAW, "RAW");
	   AQUtil.dropQueue(session, connector.getUsername(), TestConfig.QUEUE_RAW, /*force*/false);
	} 
    
	public void testTextMessage() throws JMSException, AQException, UMOException {
	    AQUtil.createOrReplaceTextQueue(session, connector.getUsername(), TestConfig.QUEUE_TEXT);

	    MuleUtil.sendMessage("jms://" + TestConfig.QUEUE_TEXT, TestConfig.TEXT_MESSAGE);
        assertEquals(TestConfig.TEXT_MESSAGE, MuleUtil.receiveMessage("jms://" + TestConfig.QUEUE_TEXT, "JMSMessageToObject"));
 	    
        AQUtil.dropQueue(session, connector.getUsername(), TestConfig.QUEUE_TEXT, /*force*/false);
    }
}
