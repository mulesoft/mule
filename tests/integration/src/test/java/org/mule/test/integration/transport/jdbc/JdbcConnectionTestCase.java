/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.transport.jdbc;


import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transport.SimpleRetryConnectionStrategy;
import org.mule.transport.jdbc.JdbcConnector;

import javax.sql.DataSource;

/**
 * This test must be run manually. See the comments inline in testReconnection
 */
public class JdbcConnectionTestCase extends AbstractJdbcFunctionalTestCase
{

    protected JdbcConnector connector;

    protected void emptyTable() throws Exception
    {
        // TODO this overrides super.emptyTable() - is this correct?
        // the entire test seems to be incomplete, see the comments below..
    }

    public Connector createConnector() throws Exception
    {
        connector = (JdbcConnector)super.createConnector();
        SimpleRetryConnectionStrategy strategy = new SimpleRetryConnectionStrategy();
        strategy.setRetryCount(10);
        strategy.setRetryFrequency(1000);
        strategy.setDoThreading(true);
        connector.setConnectionStrategy(strategy);
        return connector;
    }

    public void testReconnection() throws Exception
    {

        Service service = getTestService("anOrange", Orange.class);
        service.setModel(model);
        muleContext.getRegistry().registerService(service);
        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("jdbc://test?sql=SELECT * FROM TABLE", muleContext);
        endpointBuilder.setName("test");
        endpointBuilder.setConnector(connector);
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            endpointBuilder);
        muleContext.start();
        connector.registerListener(service, endpoint);

        // The derbydb instance should be put offline before starting test
        // The receiver should try to connect to the database
        //
        // Then put derbydb online.
        // Check that the receiver reconnect and polls the database
        //
        // Put derbydb offline.
        // The receiver should try to connect to the database.
        Thread.sleep(1000);
    }
    
    protected DataSource createDataSource() throws Exception
    {
        return createClientDataSource();
    }

}
