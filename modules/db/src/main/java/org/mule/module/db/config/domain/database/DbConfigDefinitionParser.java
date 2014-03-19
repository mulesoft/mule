/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.config.domain.database;

import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;

import java.sql.Connection;
import java.util.Map;

public class DbConfigDefinitionParser extends MuleOrphanDefinitionParser
{

    private static final Map<String, Integer> TRANSACTION_ISOLATION_MAPPING;

    static
    {
        TRANSACTION_ISOLATION_MAPPING = new java.util.HashMap<String, Integer>();
        TRANSACTION_ISOLATION_MAPPING.put("", -1); // this is the default in xapool
        TRANSACTION_ISOLATION_MAPPING.put("NONE", Connection.TRANSACTION_NONE);
        TRANSACTION_ISOLATION_MAPPING.put("READ_COMMITTED", Connection.TRANSACTION_READ_COMMITTED);
        TRANSACTION_ISOLATION_MAPPING.put("READ_UNCOMMITTED", Connection.TRANSACTION_READ_UNCOMMITTED);
        TRANSACTION_ISOLATION_MAPPING.put("REPEATABLE_READ", Connection.TRANSACTION_REPEATABLE_READ);
        TRANSACTION_ISOLATION_MAPPING.put("SERIALIZABLE", Connection.TRANSACTION_SERIALIZABLE);
    }

    public static final String TRANSACTION_ISOLATION_ATTRIBUTE = "transactionIsolation";
    public static final String URL_ATTRIBUTE = "url";
    public static final String DRIVER_ATTRIBUTE = "driver";
    public static final String HOST_ATTRIBUTE = "host";
    public static final String PORT_ATTRIBUTE = "port";
    public static final String DATABASE_ATTRIBUTE = "database";
    public static final String LOGIN_TIMEOUT_ATTRIBUTE = "connectionTimeout";
    public static final String DATA_SOURCE_REF_ATTRIBUTE = "dataSource-ref";
    public static final String USE_XA_TRANSACTIONS_ATTRIBUTE = "useXaTransactions";

    public DbConfigDefinitionParser(Class<? extends DbConfigFactoryBean> poolFactoryClass, CheckExclusiveAttributes exclusiveAttributes)
    {
        super(poolFactoryClass, true);

        addMapping(TRANSACTION_ISOLATION_ATTRIBUTE, TRANSACTION_ISOLATION_MAPPING);

        registerPreProcessor(exclusiveAttributes);
    }

}
