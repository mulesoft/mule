/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */
package org.mule.providers.oracle.jms;

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
