/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
