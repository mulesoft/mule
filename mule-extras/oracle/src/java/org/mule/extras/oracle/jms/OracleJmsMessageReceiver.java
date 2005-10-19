package org.mule.extras.oracle.jms;

import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.JmsMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;

/**
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class OracleJmsMessageReceiver extends JmsMessageReceiver {
	
	public OracleJmsMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint) throws InitialisationException {
		super(connector, component, endpoint);
	}

	/** Save a copy of the endpoint's properties within the OracleJmsSupport object.
	 * @see OracleJmsSupport#endpointProperties */
	protected void createConsumer() throws Exception {
    	((OracleJmsSupport) ((JmsConnector) getConnector()).getJmsSupport()).setEndpointProperties(endpoint.getProperties());
		super.createConsumer();
	}
}
