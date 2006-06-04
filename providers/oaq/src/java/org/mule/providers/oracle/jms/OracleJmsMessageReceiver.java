/*
 * $Id$
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.TransactedJmsMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;

/**
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class OracleJmsMessageReceiver extends TransactedJmsMessageReceiver {

    public OracleJmsMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint) throws InitialisationException {
        super(connector, component, endpoint);
    }

    /** Save a copy of the endpoint's properties within the OracleJmsSupport object.
     * @see OracleJmsSupport#endpointProperties */
    protected void createConsumer() throws Exception {
        ((OracleJmsSupport) ((JmsConnector) getConnector()).getJmsSupport()).setEndpointProperties(endpoint.getProperties());
        super.createConsumer();
    }

    public void poll() throws Exception {
        log.trace("Polling...");
        super.poll();
    }

    private static Log log = LogFactory.getLog(OracleJmsMessageReceiver.class);
}
