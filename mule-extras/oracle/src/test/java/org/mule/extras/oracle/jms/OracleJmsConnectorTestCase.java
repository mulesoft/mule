package org.mule.extras.oracle.jms;

import java.util.Enumeration;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.TextMessage;

import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

import com.mockobjects.dynamic.Mock;

/**
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson </a>
 */
public class OracleJmsConnectorTestCase extends AbstractConnectorTestCase {
	
	private OracleJmsConnector connector;

	public UMOConnector getConnector() throws Exception {
		if (connector == null) {
			connector = new OracleJmsConnector();
			connector.setName("TestConnector");
			connector.setUrl("jdbc:oracle:oci:@TEST_DB");
			connector.setUsername("scott");
			connector.setPassword("tiger");

			Mock connectionFactory = new Mock(ConnectionFactory.class);
			Mock connection = new Mock(Connection.class);
			connectionFactory.expectAndReturn("createConnection",
					(Connection) connection.proxy());
			connection.expect("close");
			connection.expect("start");
			connection.expect("stop");
			connection.expect("stop");
			connection.expect("setClientID", "mule.TestConnector");
			connector.setConnectionFactory((ConnectionFactory) connectionFactory.proxy());
			connector.initialise();
		}
		return connector;
	}

	public String getTestEndpointURI() {
		return "jms://TEST_QUEUE";
	}

	public Object getValidMessage() throws Exception {
		return getMessage();
	}

	public static Object getMessage() throws Exception {
		Mock message = new Mock(TextMessage.class);
		message.expectAndReturn("getText", "Test JMS Message");
		message.expectAndReturn("getText", "Test JMS Message");
		message.expectAndReturn("getJMSCorrelationID", null);
		message.expectAndReturn("getJMSMessageID", "1234567890");
		message.expectAndReturn("getJMSDeliveryMode", new Integer(1));
		message.expectAndReturn("getJMSDestination", null);
		message.expectAndReturn("getJMSPriority", new Integer(4));
		message.expectAndReturn("getJMSRedelivered", new Boolean(false));
		message.expectAndReturn("getJMSReplyTo", null);
		message.expectAndReturn("getJMSExpiration", new Long(0));
		message.expectAndReturn("getJMSTimestamp", new Long(0));
		message.expectAndReturn("getJMSType", null);
		message.expectAndReturn("getPropertyNames", new Enumeration() {

			public boolean hasMoreElements() {
				return false;
			}

			public Object nextElement() {
				return null;
			}
		});
		return (TextMessage) message.proxy();
	}
}
