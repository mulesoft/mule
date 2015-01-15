/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.specific;

import org.mule.module.springconfig.factories.AsyncMessageProcessorsFactoryBean;
import org.mule.module.springconfig.parsers.generic.ChildDefinitionParser;
import org.mule.module.springconfig.util.ProcessingStrategyUtils;

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
