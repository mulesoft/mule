/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.factories.AsyncMessageProcessorsFactoryBean;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.util.ProcessingStrategyUtils;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class AsyncMessageProcessorsDefinitionParser extends ChildDefinitionParser
{
    public AsyncMessageProcessorsDefinitionParser()
    {
        super("messageProcessor", AsyncMessageProcessorsFactoryBean.class);
        addIgnored("processingStrategy");
    }

    @Override
    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        ProcessingStrategyUtils.configureProcessingStrategy(element, builder,
            ProcessingStrategyUtils.QUEUED_ASYNC_PROCESSING_STRATEGY);
        super.parseChild(element, parserContext, builder);
    }

}
