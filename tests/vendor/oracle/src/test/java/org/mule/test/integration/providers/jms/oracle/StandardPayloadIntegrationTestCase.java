package org.mule.test.integration.providers.jms.oracle;

import javax.jms.JMSException;

import oracle.AQ.AQException;

import org.mule.test.integration.providers.jms.oracle.util.AQUtil;
import org.mule.test.integration.providers.jms.oracle.util.MuleUtil;
import org.mule.umo.UMOException;

/**
 * Tests the connector against a live Oracle database using standard JMS messages.
 *
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class StandardPayloadIntegrationTestCase extends AbstractIntegrationTestCase {

    protected String getConfigurationFiles() {
        return "jms-connector-config.xml";
    }

    public void testCreateAndDropQueue() throws AQException, JMSException {
       AQUtil.createOrReplaceQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_RAW, "RAW");
       AQUtil.dropQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_RAW, /*force*/false);
    }

    public void testTextMessage() throws Exception {
        AQUtil.createOrReplaceTextQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_TEXT, false);

        muleClient.dispatch("jms://" + TestConfig.QUEUE_TEXT, TestConfig.TEXT_MESSAGE, null);
        assertEquals(TestConfig.TEXT_MESSAGE, muleClient.receive("jms://" + TestConfig.QUEUE_TEXT, "JMSMessageToObject", MuleUtil.MULE_RECEIVE_TIMEOUT).getPayloadAsString());

        AQUtil.dropQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_TEXT, /*force*/false);
    }
}
