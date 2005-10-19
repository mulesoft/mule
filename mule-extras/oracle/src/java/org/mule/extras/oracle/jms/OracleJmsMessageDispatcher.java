package org.mule.extras.oracle.jms;

import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.JmsMessageDispatcher;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;

/**
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class OracleJmsMessageDispatcher extends JmsMessageDispatcher {
	
    public OracleJmsMessageDispatcher(JmsConnector connector) {
        super(connector);
    }

	/** Save a copy of the endpoint's properties within the OracleJmsSupport object.
	 * @see OracleJmsSupport#endpointProperties */
    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception {
    	((OracleJmsSupport) ((JmsConnector) getConnector()).getJmsSupport()).setEndpointProperties(endpointUri.getParams());
    	return super.receive(endpointUri, timeout);
	}
}
