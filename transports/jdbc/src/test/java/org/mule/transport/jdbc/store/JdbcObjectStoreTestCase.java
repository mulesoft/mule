/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.store;

import java.io.Serializable;

import org.apache.commons.dbutils.QueryRunner;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.store.ObjectStore;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.util.store.AbstractObjectStoreContractTestCase;

public class JdbcObjectStoreTestCase extends AbstractObjectStoreContractTestCase
{

    public JdbcObjectStoreTestCase()
    {
        this.setStartContext(true);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        JdbcObjectStore<?> store = muleContext.getRegistry().get("jdbcObjectStore");
        QueryRunner qr = store.getJdbcConnector().getQueryRunner();
        
        try
        {
            qr.update(store.getJdbcConnector().getConnection(), "DELETE FROM IDS");
        }
        catch (Exception e)
        {
        }
        
        try
        {
            qr.update(store.getJdbcConnector().getConnection(),
                "CREATE TABLE IDS(K VARCHAR(255) NOT NULL PRIMARY KEY, VALUE VARCHAR(255))");
        }
        catch (Exception e)
        {
        }
        
        logger.debug("Table created");
    }

    @Override
    public ObjectStore getObjectStore()
    {
        JdbcObjectStore<?> store = muleContext.getRegistry().get("jdbcObjectStore");
        return store;
    }

    @Override
    public Serializable getStorableValue()
    {
        return "1";
    }

    @Override
    protected String getConfigurationResources()
    {
        return "jdbc-connector.xml,jdbc-store.xml";
    }

    @Override
    protected ConfigurationBuilder getBuilder() throws Exception
    {
        return new SpringXmlConfigurationBuilder(getConfigurationResources());
    }
}
