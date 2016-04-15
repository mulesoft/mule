/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.config;

import static org.mule.config.spring.handlers.AbstractMuleNamespaceHandler.IgnoredDefinitionParser;
import static org.mule.module.db.internal.config.domain.database.DbConfigDefinitionParser.DATABASE_ATTRIBUTE;
import static org.mule.module.db.internal.config.domain.database.DbConfigDefinitionParser.DATA_SOURCE_REF_ATTRIBUTE;
import static org.mule.module.db.internal.config.domain.database.DbConfigDefinitionParser.DRIVER_ATTRIBUTE;
import static org.mule.module.db.internal.config.domain.database.DbConfigDefinitionParser.HOST_ATTRIBUTE;
import static org.mule.module.db.internal.config.domain.database.DbConfigDefinitionParser.LOGIN_TIMEOUT_ATTRIBUTE;
import static org.mule.module.db.internal.config.domain.database.DbConfigDefinitionParser.PASSWORD_ATTRIBUTE;
import static org.mule.module.db.internal.config.domain.database.DbConfigDefinitionParser.PORT_ATTRIBUTE;
import static org.mule.module.db.internal.config.domain.database.DbConfigDefinitionParser.TRANSACTION_ISOLATION_ATTRIBUTE;
import static org.mule.module.db.internal.config.domain.database.DbConfigDefinitionParser.URL_ATTRIBUTE;
import static org.mule.module.db.internal.config.domain.database.DbConfigDefinitionParser.USER_ATTRIBUTE;
import static org.mule.module.db.internal.config.domain.database.DbConfigDefinitionParser.USE_XA_TRANSACTIONS_ATTRIBUTE;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributeAndText;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.config.spring.parsers.processors.CheckRequiredAttributes;
import org.mule.module.db.internal.config.domain.connection.PoolingProfileBeanDefinitionParser;
import org.mule.module.db.internal.config.domain.database.DbConfigDefinitionParser;
import org.mule.module.db.internal.config.domain.database.DbConfigResolverFactoryBean;
import org.mule.module.db.internal.config.domain.database.DerbyConfigResolverFactoryBean;
import org.mule.module.db.internal.config.domain.database.MySqlConfigResolverFactoryBean;
import org.mule.module.db.internal.config.domain.database.OracleConfigResolverFactoryBean;
import org.mule.module.db.internal.config.domain.param.InOutParamDefinitionDefinitionParser;
import org.mule.module.db.internal.config.domain.param.InputParamValueBeanDefinitionParser;
import org.mule.module.db.internal.config.domain.param.OutputParamDefinitionDefinitionParser;
import org.mule.module.db.internal.config.domain.query.QueryTemplateBeanDefinitionParser;
import org.mule.module.db.internal.config.processor.BulkExecuteProcessorBeanDefinitionParser;
import org.mule.module.db.internal.config.processor.DeleteProcessorBeanDefinitionParser;
import org.mule.module.db.internal.config.processor.ExecuteDdlProcessorBeanDefinitionParser;
import org.mule.module.db.internal.config.processor.InsertProcessorBeanDefinitionParser;
import org.mule.module.db.internal.config.processor.SelectProcessorDefinitionParser;
import org.mule.module.db.internal.config.processor.StoredProcedureProcessorBeanDefinitionParser;
import org.mule.module.db.internal.config.processor.UpdateProcessorBeanDefinitionParser;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class DbNamespaceHandler extends NamespaceHandlerSupport
{

    public void init()
    {
        registerBeanDefinitionParser("select", new SelectProcessorDefinitionParser());
        registerBeanDefinitionParser("update", new UpdateProcessorBeanDefinitionParser());
        registerBeanDefinitionParser("delete", new DeleteProcessorBeanDefinitionParser());
        registerBeanDefinitionParser("insert", new InsertProcessorBeanDefinitionParser());
        registerBeanDefinitionParser("execute-ddl", new ExecuteDdlProcessorBeanDefinitionParser());
        registerBeanDefinitionParser("stored-procedure", new StoredProcedureProcessorBeanDefinitionParser());

        BulkExecuteProcessorBeanDefinitionParser bulkExecuteProcessorBeanDefinitionParser = new BulkExecuteProcessorBeanDefinitionParser();
        bulkExecuteProcessorBeanDefinitionParser.registerPreProcessor(new CheckExclusiveAttributeAndText("file"));
        registerBeanDefinitionParser("bulk-execute", bulkExecuteProcessorBeanDefinitionParser);

        registerBeanDefinitionParser("in-param", new InputParamValueBeanDefinitionParser());
        registerBeanDefinitionParser("out-param", new OutputParamDefinitionDefinitionParser());
        registerBeanDefinitionParser("inout-param", new InOutParamDefinitionDefinitionParser());

        registerBeanDefinitionParser("template-query", new QueryTemplateBeanDefinitionParser());
        registerBeanDefinitionParser("template-query-ref", new QueryTemplateBeanDefinitionParser());

        registerConfigDefinitionParsers();

        registerBeanDefinitionParser("pooling-profile", new PoolingProfileBeanDefinitionParser());
    }

    private void registerConfigDefinitionParsers()
    {
        registerBeanDefinitionParser("generic-config", new DbConfigDefinitionParser(DbConfigResolverFactoryBean.class, new CheckExclusiveAttributes(new String[][] {
                new String[] {DRIVER_ATTRIBUTE, URL_ATTRIBUTE, LOGIN_TIMEOUT_ATTRIBUTE, TRANSACTION_ISOLATION_ATTRIBUTE, USE_XA_TRANSACTIONS_ATTRIBUTE},
                new String[] {DATA_SOURCE_REF_ATTRIBUTE}})));

        registerBeanDefinitionParser("derby-config", new DbConfigDefinitionParser(DerbyConfigResolverFactoryBean.class, new CheckExclusiveAttributes(new String[][] {
                new String[] {URL_ATTRIBUTE, LOGIN_TIMEOUT_ATTRIBUTE, TRANSACTION_ISOLATION_ATTRIBUTE, USE_XA_TRANSACTIONS_ATTRIBUTE},
                new String[] {DATA_SOURCE_REF_ATTRIBUTE}})));

        DbConfigDefinitionParser oracleDbConfigFactoryBean = new DbConfigDefinitionParser(OracleConfigResolverFactoryBean.class, new CheckExclusiveAttributes(new String[][] {
                new String[] {URL_ATTRIBUTE, LOGIN_TIMEOUT_ATTRIBUTE, TRANSACTION_ISOLATION_ATTRIBUTE, USE_XA_TRANSACTIONS_ATTRIBUTE},
                new String[] {DATA_SOURCE_REF_ATTRIBUTE}}));
        oracleDbConfigFactoryBean.registerPreProcessor(
                new CheckRequiredAttributes(new String[][] {
                        {DATA_SOURCE_REF_ATTRIBUTE},
                        {DATA_SOURCE_REF_ATTRIBUTE, USER_ATTRIBUTE, PASSWORD_ATTRIBUTE},
                        {URL_ATTRIBUTE},
                        {USER_ATTRIBUTE, PASSWORD_ATTRIBUTE, URL_ATTRIBUTE},
                        {USER_ATTRIBUTE, PASSWORD_ATTRIBUTE, HOST_ATTRIBUTE, PORT_ATTRIBUTE}
                }));

        oracleDbConfigFactoryBean.addAlias("instance", "database");
        registerBeanDefinitionParser("oracle-config", oracleDbConfigFactoryBean);

        registerBeanDefinitionParser("mysql-config", new DbConfigDefinitionParser(MySqlConfigResolverFactoryBean.class, new CheckExclusiveAttributes(new String[][] {
                new String[] {URL_ATTRIBUTE, LOGIN_TIMEOUT_ATTRIBUTE, TRANSACTION_ISOLATION_ATTRIBUTE, USE_XA_TRANSACTIONS_ATTRIBUTE},
                new String[] {HOST_ATTRIBUTE, PORT_ATTRIBUTE, DATABASE_ATTRIBUTE, LOGIN_TIMEOUT_ATTRIBUTE, TRANSACTION_ISOLATION_ATTRIBUTE, USE_XA_TRANSACTIONS_ATTRIBUTE},
                new String[] {DATA_SOURCE_REF_ATTRIBUTE}})));

        registerIgnoredElement(DbConfigDefinitionParser.CONNECTION_PROPERTIES_ELEMENT_NAME);
        registerIgnoredElement(DbConfigDefinitionParser.PROPERTY_ELEMENT_NAME);
        registerIgnoredElement(DbConfigDefinitionParser.DATA_TYPES_ELEMENT);
        registerIgnoredElement(DbConfigDefinitionParser.DATA_TYPE_ELEMENT);
    }

    private void registerIgnoredElement(String name)
    {
        registerBeanDefinitionParser(name, new IgnoredDefinitionParser());
    }
}
