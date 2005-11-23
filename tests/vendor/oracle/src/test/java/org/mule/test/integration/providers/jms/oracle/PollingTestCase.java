package org.mule.test.integration.providers.jms.oracle;

import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.providers.oracle.jms.util.AQUtil;
import org.mule.providers.oracle.jms.util.MuleUtil;
import org.mule.umo.provider.UMOMessageReceiver;

/**
 * Makes sure the Oracle AQ connector continues to process new incoming messages after 
 * startup.
 * 
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class PollingTestCase extends AbstractIntegrationTestCase {
	
    protected String getConfigurationFiles() {
    	return "jms-connector-config.xml, pass-through-config.xml";
    }
    
    public void setUp() throws Exception {   	
    	super.setUp();
	    
    	AQUtil.createOrReplaceTextQueue(session, connector.getUsername(), TestConfig.QUEUE_TEXT);
	    AQUtil.createOrReplaceTextQueue(session, connector.getUsername(), TestConfig.QUEUE_TEXT2);

	    // We have to start the model _after_ the queues have been created, otherwise
	    // the connector will try to create them dynamically.
	    manager.getModel().startComponent("PassThrough");
    }
    
    public void tearDown() throws Exception {   	   	
    	// We delete the receivers in order to drop all connections to the Oracle queues.
    	// Otherwise we'll get an "ORA-00054: resource busy and acquire with NOWAIT specified" 
    	// exception when trying to drop the queues.
	    UMOMessageReceiver receiver;
	    Iterator it = connector.getReceivers().values().iterator();
	    while (it.hasNext()) {
	    	receiver = (UMOMessageReceiver) it.next();
	    	connector.destroyReceiver(receiver, receiver.getEndpoint());
	    }
    	
	    // TODO For some reason there are still open connections at this point which 
	    // prevent dropping the queues.
	    //AQUtil.dropQueue(session, connector.getUsername(), TestConfig.QUEUE_TEXT2, /*force*/false);
        //AQUtil.dropQueue(session, connector.getUsername(), TestConfig.QUEUE_TEXT, /*force*/false);    	
    	
        super.tearDown();
    }
    
	public void testTextMessage() throws Exception {    
	    MuleUtil.sendMessage("jms://" + TestConfig.QUEUE_TEXT, TestConfig.TEXT_MESSAGE);
        assertEquals(TestConfig.TEXT_MESSAGE, MuleUtil.receiveMessage("jms://" + TestConfig.QUEUE_TEXT2, "JMSMessageToObject"));
	    MuleUtil.sendMessage("jms://" + TestConfig.QUEUE_TEXT, TestConfig.TEXT_MESSAGE);
        assertEquals(TestConfig.TEXT_MESSAGE, MuleUtil.receiveMessage("jms://" + TestConfig.QUEUE_TEXT2, "JMSMessageToObject"));
	    MuleUtil.sendMessage("jms://" + TestConfig.QUEUE_TEXT, TestConfig.TEXT_MESSAGE);
        assertEquals(TestConfig.TEXT_MESSAGE, MuleUtil.receiveMessage("jms://" + TestConfig.QUEUE_TEXT2, "JMSMessageToObject"));
	}

	private static Log log = LogFactory.getLog(PollingTestCase.class);
}
