/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.ClassOrRefDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildSingletonMapDefinitionParser;
import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.config.spring.parsers.specific.ObjectFactoryWrapper;
import org.mule.config.spring.parsers.specific.TransactionDefinitionParser;
import org.mule.config.spring.parsers.specific.properties.NestedMapDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.jdbc.JdbcConnector;
import org.mule.transport.jdbc.JdbcTransactionFactory;
import org.mule.transport.jdbc.store.JdbcObjectStore;

/**
 * Registers Bean Definition Parsers for the "jdbc" namespace.
 */
public class JdbcNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public static final String QUERY_KEY = "queryKey";
    public static final String[] ADDRESS_ATTRIBUTES = new String[] {QUERY_KEY};
    public static final String SQL_STATEMENT_FACTORY_PROPERTY = "sqlStatementStrategyFactory";

    @Override
    public void init()
    {
        logger.warn(getDeprecationWarning());
        registerStandardTransportEndpoints(JdbcConnector.JDBC, ADDRESS_ATTRIBUTES).addAlias(QUERY_KEY, URIBuilder.PATH);
        registerConnectorDefinitionParser(JdbcConnector.class, JdbcConnector.JDBC);
        registerBeanDefinitionParser("dataSource", new ObjectFactoryWrapper("dataSourceFactory"));
        registerBeanDefinitionParser(SQL_STATEMENT_FACTORY_PROPERTY, new ClassOrRefDefinitionParser(SQL_STATEMENT_FACTORY_PROPERTY));
        MuleDefinitionParser connectorQuery = new ChildSingletonMapDefinitionParser("query");
        MuleDefinitionParser endpointQuery = new NestedMapDefinitionParser("properties", "queries");
        endpointQuery.addCollection("properties");
        registerMuleBeanDefinitionParser("query", new ParentContextDefinitionParser("connector", connectorQuery).otherwise(endpointQuery));
        registerBeanDefinitionParser("extractors", new ParentDefinitionParser());
        registerBeanDefinitionParser("transaction", new TransactionDefinitionParser(JdbcTransactionFactory.class));
        registerBeanDefinitionParser("object-store", new ChildDefinitionParser("store", JdbcObjectStore.class));
        registerDataSourceDefinitionParsers();
    }

    protected String getDeprecationWarning()
    {
        return "JDBC transport is deprecated and will be removed in Mule 4.0. Use the DB module instead";
    }

    protected void registerDataSourceDefinitionParsers()
    {
        registerDerbyDataSourceDefinitionParser();
        registerMysqlDataSourceDefinitionParser();
        registerInstanceDatasource("oracle-data-source", OracleDataSourceFactoryBean.class);
        registerPostgresqlDataSourceDefinitionParser();
        registerDb2DataSourceDefinitionParser();
        registerInstanceDatasource("mssql-data-source", MssqlDatasourceFactoryBean.class);
    }

    protected void registerDerbyDataSourceDefinitionParser()
    {
        DataSourceDefinitionParser parser = new DataSourceDefinitionParser(DerbyDataSourceFactoryBean.class);

        // make sure that either url or database is configured
        parser.registerPreProcessor(new CheckDatabaseOrUrl());

        registerBeanDefinitionParser("derby-data-source", parser);
    }

    protected void registerMysqlDataSourceDefinitionParser()
    {
        registerHostAndPortTypeDefinitionParser(MysqlDataSourceFactoryBean.class,
                                                "mysql-data-source");
    }

    protected void registerDb2DataSourceDefinitionParser()
    {
        DataSourceDefinitionParser parser = new DataSourceDefinitionParser(Db2DatasourceFactoryBean.class);

        // make sure that either url or host/port are configured
        String[][] attributeGroups = new String[][] {
                new String[] {"url"},
                new String[] {"host", "port", "database"}
        };
        CheckExclusiveAttributes attributeCheck = new CheckExclusiveAttributes(attributeGroups);
        parser.registerPreProcessor(attributeCheck);

        // make sure that either url or database is configured
        parser.registerPreProcessor(new CheckDatabaseOrUrl());

        registerBeanDefinitionParser("db2-data-source", parser);
    }

    private void registerInstanceDatasource(String elementName, Class<? extends AbstractDataSourceFactoryBean> poolFactoryClass)
    {
        DataSourceDefinitionParser parser = new DataSourceDefinitionParser(poolFactoryClass);

        String[][] attributeGroups = new String[][] {
                new String[] {"url"},
                new String[] {"host", "port", "instance"}
        };
        CheckExclusiveAttributes attributeCheck = new CheckExclusiveAttributes(attributeGroups);
        parser.registerPreProcessor(attributeCheck);

        registerBeanDefinitionParser(elementName, parser);
    }

    protected void registerPostgresqlDataSourceDefinitionParser()
    {
        registerHostAndPortTypeDefinitionParser(PostgresqlDataSourceFactoryBean.class,
                                                "postgresql-data-source");
    }

    protected void registerHostAndPortTypeDefinitionParser(Class<? extends AbstractDataSourceFactoryBean> poolFactoryClass,
                                                           String elementName)
    {
        DataSourceDefinitionParser parser = new DataSourceDefinitionParser(poolFactoryClass);

        // make sure that either url or host/port are configured
        String[][] attributeGroups = new String[][] {
                new String[] {"url"},
                new String[] {"host", "port"}
        };
        CheckExclusiveAttributes attributeCheck = new CheckExclusiveAttributes(attributeGroups);
        parser.registerPreProcessor(attributeCheck);

        // make sure that either url or database is configured
        parser.registerPreProcessor(new CheckDatabaseOrUrl());

        registerBeanDefinitionParser(elementName, parser);

    }
}
