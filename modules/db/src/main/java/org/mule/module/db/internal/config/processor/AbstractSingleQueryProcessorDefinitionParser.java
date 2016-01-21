/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.processor;

import org.mule.module.db.internal.config.domain.param.DefaultSqlParamResolverFactoryBean;
import org.mule.module.db.internal.config.resolver.query.QueryResolverFactoryBean;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public abstract class AbstractSingleQueryProcessorDefinitionParser extends AbstractAdvancedDbProcessorDefinitionParser
{

    protected BeanDefinition queryResolverBean;
    protected BeanDefinition queryBean;

    @Override
    protected void doParseElement(Element element, ParserContext context, BeanDefinitionBuilder builder)
    {
        // We want any parsing to occur as a child of this tag so we need to make
        // a new one that has this as it's owner/parent
        ParserContext nestedCtx = new ParserContext(context.getReaderContext(), context.getDelegate(), builder.getBeanDefinition());

        parseConfig(element, builder);

        String streamingValue = element.getAttribute(STREAMING_ATTRIBUTE);
        processStreamingAttribute(streamingValue);
        BeanDefinition sqlParamResolver = getParamResolverBeanDefinition();

        queryBean = queryDefinitionParser.parseQuery(element, nestedCtx);

        createQueryResolverBeanDefinition(sqlParamResolver);

        builder.addConstructorArgValue(queryResolverBean);

        parseSourceExpression(element, builder);
        parseTargetExpression(element, builder);
        parseExecutorFactory(element, builder);
        parseTransactionalAction(element, builder);
        parseMetadataProvider(element, builder);
        configureStatementResultSetCloser(builder);
    }

    protected void createQueryResolverBeanDefinition(BeanDefinition sqlParamResolver)
    {
        BeanDefinitionBuilder queryResolverFactoryBean = BeanDefinitionBuilder.genericBeanDefinition(QueryResolverFactoryBean.class);
        queryResolverFactoryBean.addConstructorArgValue(queryBean);
        queryResolverFactoryBean.addConstructorArgValue(sqlParamResolver);
        queryResolverFactoryBean.addConstructorArgValue(dbConfigResolverFactoryBeanDefinition);
        queryResolverBean = queryResolverFactoryBean.getBeanDefinition();
    }

    protected BeanDefinition getParamResolverBeanDefinition()
    {
        BeanDefinitionBuilder sqlParamResolverFactory = BeanDefinitionBuilder.genericBeanDefinition(DefaultSqlParamResolverFactoryBean.class);
        return sqlParamResolverFactory.getBeanDefinition();
    }

}
