/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.oracle.jms;

import com.mockobjects.dynamic.Mock;

import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.providers.AbstractConnector;
import org.mule.tck.providers.AbstractMessageReceiverTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageReceiver;

/**
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class OracleJmsMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{

    private OracleJmsConnector connector = null;

    public UMOConnector getConnector() throws Exception
    {
        if (connector == null)
        {
            connector = new OracleJmsConnector();
            connector.setName("TestConnector");
            connector.setUrl("jdbc:oracle:oci:@TEST_DB");
            connector.setUsername("scott");
            connector.setPassword("tiger");
            connector.initialise();
        }
        return connector;
    }

    public UMOMessageReceiver getMessageReceiver() throws Exception
    {
        getConnector();
        endpoint = new MuleEndpoint("jms://TEST_QUEUE", true);
        endpoint.setConnector(getConnector());
        Mock mockComponent = new Mock(UMOComponent.class);
        return new OracleJmsMessageReceiver((AbstractConnector)endpoint.getConnector(),
            (UMOComponent)mockComponent.proxy(), endpoint);
    }

    public UMOEndpoint getEndpoint() throws Exception
    {
        endpoint = new MuleEndpoint("jms://TEST_QUEUE", true);
        endpoint.setConnector(getConnector());
        return endpoint;
    }
}
