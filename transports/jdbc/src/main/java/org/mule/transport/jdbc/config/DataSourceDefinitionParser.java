/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
