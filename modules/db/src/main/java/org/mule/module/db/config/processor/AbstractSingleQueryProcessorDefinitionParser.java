/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.config.processor;

import org.mule.module.db.config.domain.param.DefaultSqlParamResolverFactoryBean;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public abstract class AbstractSingleQueryProcessorDefinitionParser extends AbstractAdvancedDbProcessorDefinitionParser
{

    protected BeanDefinition queryResolverBean;

    @Override
    protected void doParseElement(Element element, ParserContext context, BeanDefinitionBuilder builder)
    {
        // We want any parsing to occur as a child of this tag so we need to make
        // a new one that has this as it's owner/parent
        ParserContext nestedCtx = new ParserContext(context.getReaderContext(), context.getDelegate(), builder.getBeanDefinition());

        parseConfig(element, builder);

        String streamingValue = element.getAttribute(STREAMING_ATTRIBUTE);
        processStreamingAttribute(builder, streamingValue);
        BeanDefinitionBuilder sqlParamResolverFactory = BeanDefinitionBuilder.genericBeanDefinition(DefaultSqlParamResolverFactoryBean.class);
        AbstractBeanDefinition sqlParamResolver = sqlParamResolverFactory.getBeanDefinition();
        queryResolverBean = parameterizedQueryDefinitionParser.parseQuery(element, nestedCtx, sqlParamResolver, dbConfigResolverFactoryBeanDefinition);
        builder.addConstructorArgValue(queryResolverBean);

        parseSourceExpression(element, builder);
        parseTargetExpression(element, builder);
        parseExecutorFactory(element, builder);
        parseTransactionalAction(element, builder);
        parseMetadataProvider(element, builder);
    }

}
