/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.jdbc;


import org.mule.impl.endpoint.EndpointURIEndpointBuilder;
import org.mule.providers.SimpleRetryConnectionStrategy;
import org.mule.providers.jdbc.JdbcConnector;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpointBuilder;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;

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

    public UMOConnector createConnector() throws Exception
    {
        connector = (JdbcConnector)super.createConnector();
        SimpleRetryConnectionStrategy strategy = new SimpleRetryConnectionStrategy();
        strategy.setRetryCount(10);
        strategy.setRetryFrequency(1000);
        strategy.setDoThreading(true);
        //TODO RM* URGENT : connector.setConnectionStrategy(strategy);
        return connector;
    }

    public void testReconnection() throws Exception
    {

        UMOComponent component = getTestComponent("anOrange", Orange.class);
        component.setModel(model);
        managementContext.getRegistry().registerComponent(component, managementContext);
        UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("jdbc://test?sql=SELECT * FROM TABLE", managementContext);
        endpointBuilder.setName("test");
        endpointBuilder.setConnector(connector);
        UMOImmutableEndpoint endpoint = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            endpointBuilder, managementContext);
        managementContext.start();
        connector.registerListener(component, endpoint);

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
