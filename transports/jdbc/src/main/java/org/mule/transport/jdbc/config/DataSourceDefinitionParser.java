/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.config;

import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;

import java.sql.Connection;
import java.util.Map;

public class DataSourceDefinitionParser extends MuleOrphanDefinitionParser
{
    private static final Map<String, Integer> TRANSACTION_ISOLATION_MAPPING;

    static
    {
        TRANSACTION_ISOLATION_MAPPING = new java.util.HashMap<String, Integer>();
        TRANSACTION_ISOLATION_MAPPING.put("UNSPECIFIED", Integer.valueOf(-1)); // this is the default in xapool
        TRANSACTION_ISOLATION_MAPPING.put("NONE", Integer.valueOf(Connection.TRANSACTION_NONE));
        TRANSACTION_ISOLATION_MAPPING.put("READ_COMMITTED", Integer.valueOf(Connection.TRANSACTION_READ_COMMITTED));
        TRANSACTION_ISOLATION_MAPPING.put("READ_UNCOMMITTED", Integer.valueOf(Connection.TRANSACTION_READ_UNCOMMITTED));
        TRANSACTION_ISOLATION_MAPPING.put("REPEATABLE_READ", Integer.valueOf(Connection.TRANSACTION_REPEATABLE_READ));
        TRANSACTION_ISOLATION_MAPPING.put("SERIALIZABLE", Integer.valueOf(Connection.TRANSACTION_SERIALIZABLE));
    }

    public DataSourceDefinitionParser(Class<? extends AbstractDataSourceFactoryBean> poolFactoryClass)
    {
        super(poolFactoryClass, true);
        addIgnored("name");
        addMapping("transactionIsolation", TRANSACTION_ISOLATION_MAPPING);
    }
}
