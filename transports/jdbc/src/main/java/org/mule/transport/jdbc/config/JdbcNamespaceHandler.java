/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildSingletonMapDefinitionParser;
import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.specific.ObjectFactoryWrapper;
import org.mule.config.spring.parsers.specific.TransactionDefinitionParser;
import org.mule.config.spring.parsers.specific.properties.NestedMapDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.jdbc.JdbcConnector;
import org.mule.transport.jdbc.JdbcTransactionFactory;

/** Registers Bean Definition Parsers for the "jdbc" namespace. */
public class JdbcNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public static final String QUERY_KEY = "queryKey";
    public static final String[] ADDRESS_ATTRIBUTES = new String[]{QUERY_KEY};

    public void init()
    {
        registerStandardTransportEndpoints(JdbcConnector.JDBC, ADDRESS_ATTRIBUTES).addAlias(QUERY_KEY, URIBuilder.PATH);
        registerConnector(JdbcConnector.class);
        registerBeanDefinitionParser("dataSource", new ObjectFactoryWrapper("dataSourceFactory"));
        MuleDefinitionParser connectorQuery = new ChildSingletonMapDefinitionParser("query");
        MuleDefinitionParser endpointQuery = new NestedMapDefinitionParser("properties", "queries");
        endpointQuery.addCollection("properties");
        registerMuleBeanDefinitionParser("query", new ParentContextDefinitionParser("connector", connectorQuery).otherwise(endpointQuery));
        registerBeanDefinitionParser("extractors", new ParentDefinitionParser());
        registerBeanDefinitionParser("transaction", new TransactionDefinitionParser(JdbcTransactionFactory.class));
    }

}
