package org.mule.test.integration.providers.jms.oracle;

import javax.jms.JMSException;

import oracle.AQ.AQException;

import org.mule.providers.oracle.jms.util.AQUtil;
import org.mule.providers.oracle.jms.util.MuleUtil;
import org.mule.umo.UMOException;

/**
 * Makes sure the Oracle JMS connector does not fail when sending a synchronous message.
 * 
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class SynchronousTestCase extends AbstractIntegrationTestCase {
    
    protected String getConfigurationFiles() {
    	return "jms-connector-config.xml";
    }

	public void testTextMessage() throws JMSException, AQException, UMOException {
	    AQUtil.createOrReplaceTextQueue(session, connector.getUsername(), TestConfig.QUEUE_TEXT);

	    MuleUtil.sendSynchronousMessage("jms://" + TestConfig.QUEUE_TEXT, TestConfig.TEXT_MESSAGE, null);
        assertEquals(TestConfig.TEXT_MESSAGE, MuleUtil.receiveMessage("jms://" + TestConfig.QUEUE_TEXT, "JMSMessageToObject"));
 	    
        AQUtil.dropQueue(session, connector.getUsername(), TestConfig.QUEUE_TEXT, /*force*/false);
    }
}
