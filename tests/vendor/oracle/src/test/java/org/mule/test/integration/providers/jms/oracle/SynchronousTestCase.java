package org.mule.test.integration.providers.jms.oracle;

import org.mule.test.integration.providers.jms.oracle.util.AQUtil;
import org.mule.test.integration.providers.jms.oracle.util.MuleUtil;

/**
 * Makes sure the Oracle JMS connector does not fail when sending a synchronous message.
 *
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class SynchronousTestCase extends AbstractIntegrationTestCase {

    protected String getConfigurationFiles() {
        return "jms-connector-config.xml";
    }

    public void setUp() throws Exception {
        super.setUp();
        AQUtil.createOrReplaceTextQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_TEXT, false);
    }

    public synchronized void tearDown() throws Exception {
        wait(2000);
        AQUtil.dropQueue(jmsSession, jmsConnector.getUsername(), TestConfig.QUEUE_TEXT, /*force*/false);

    }

    public void testTextMessage() throws Exception {
        muleClient.send("jms://" + TestConfig.QUEUE_TEXT, TestConfig.TEXT_MESSAGE, null);
        assertEquals(TestConfig.TEXT_MESSAGE, muleClient.receive("jms://" + TestConfig.QUEUE_TEXT, "JMSMessageToObject", MuleUtil.MULE_RECEIVE_TIMEOUT).getPayloadAsString());
    }
}
