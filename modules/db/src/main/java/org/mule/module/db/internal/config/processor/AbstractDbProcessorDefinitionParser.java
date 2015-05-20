/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.processor;

import org.mule.config.spring.parsers.AbstractHierarchicalDefinitionParser;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.module.db.internal.config.resolver.database.DefaultDbConfigResolverFactoryBean;
import org.mule.module.db.internal.domain.statement.QueryStatementFactory;
import org.mule.module.db.internal.resolver.database.ConfiguredDbConfigResolver;
import org.mule.util.StringUtils;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public abstract class AbstractDbProcessorDefinitionParser extends AbstractHierarchicalDefinitionParser
{

    public static final String LIST_SEPARATOR = ",";
    public static final String CONFIG_PROPERTY = "config-ref";
    public static final String MAX_ROWS_ATTRIBUTE = "maxRows";
    public static final String FETCH_SIZE = "fetchSize";
    public static final String STREAMING_ATTRIBUTE = "streaming";
    public static final int DEFAULT_FETCH_SIZE = 10;

    protected BeanDefinition dbConfigResolverFactoryBeanDefinition;
    protected boolean streaming;

    @Override
    protected void doParse(Element element, ParserContext context, BeanDefinitionBuilder builder)
    {
        builder.setScope(isSingleton() ? BeanDefinition.SCOPE_SINGLETON : BeanDefinition.SCOPE_PROTOTYPE);
        doParseElement(element, context, builder);

        BeanAssembler assembler = getBeanAssembler(element, builder);
        assembler.insertBeanInTarget("messageProcessor");
    }

    protected abstract void doParseElement(Element element, ParserContext context, BeanDefinitionBuilder builder);

    protected void parseConfig(Element element, BeanDefinitionBuilder builder)
    {
        String config = element.getAttribute(CONFIG_PROPERTY);

        BeanDefinitionBuilder wrapper;

        if ("".equals(config))
        {
            wrapper = BeanDefinitionBuilder.genericBeanDefinition(DefaultDbConfigResolverFactoryBean.class);
        }
        else
        {
            wrapper = BeanDefinitionBuilder.genericBeanDefinition(ConfiguredDbConfigResolver.class);
            wrapper.addConstructorArgReference(config);
        }

        dbConfigResolverFactoryBeanDefinition = wrapper.getBeanDefinition();
        builder.addConstructorArgValue(dbConfigResolverFactoryBeanDefinition);
    }

    protected void parseSourceExpression(Element element, BeanDefinitionBuilder builder)
    {
        builder.addPropertyValue("source", element.getAttribute("source"));
    }

    protected void parseTargetExpression(Element element, BeanDefinitionBuilder builder)
    {
        builder.addPropertyValue("target", element.getAttribute("target"));
    }

    protected Object parseStatementFactory(Element element)
    {
        QueryStatementFactory defaultStatementFactory = new QueryStatementFactory();

        if (element.hasAttribute(MAX_ROWS_ATTRIBUTE))
        {
            defaultStatementFactory.setMaxRows(Integer.parseInt(element.getAttribute(MAX_ROWS_ATTRIBUTE)));
        }

        if (element.hasAttribute(FETCH_SIZE))
        {
            defaultStatementFactory.setFetchSize(Integer.parseInt(element.getAttribute(FETCH_SIZE)));
        }
        else if (streaming)
        {
            logger.warn("Streaming mode needs to configure fetchSize property. Using default value: " + DEFAULT_FETCH_SIZE);
            defaultStatementFactory.setFetchSize(DEFAULT_FETCH_SIZE);
        }

        return defaultStatementFactory;
    }

    protected void processStreamingAttribute(String streamingValue)
    {
        if (!StringUtils.isEmpty(streamingValue))
        {
            streaming = Boolean.parseBoolean(streamingValue);
        }
    }
}
