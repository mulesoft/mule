/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.oracle.jms;

import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.XaTransactedJmsMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.CreateException;
import org.mule.umo.provider.UMOConnector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OracleJmsMessageReceiver extends XaTransactedJmsMessageReceiver
{

    public OracleJmsMessageReceiver(UMOConnector connector, UMOComponent component, UMOImmutableEndpoint endpoint)
            throws CreateException
    {
        super(connector, component, endpoint);
    }

    /**
     * Save a copy of the endpoint's properties within the OracleJmsSupport object.
     *
     * @see OracleJmsSupport#endpointProperties
     */
    protected void createConsumer() throws Exception
    {
        ((OracleJmsSupport) ((JmsConnector) getConnector()).getJmsSupport()).setEndpointProperties(endpoint.getProperties());
        super.createConsumer();
    }

    public void poll() throws Exception
    {
        log.trace("Polling...");
        super.poll();
    }

    private static Log log = LogFactory.getLog(OracleJmsMessageReceiver.class);
}
