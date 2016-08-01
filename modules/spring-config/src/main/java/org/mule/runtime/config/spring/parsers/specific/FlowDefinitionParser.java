/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers.specific;

import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.runtime.config.spring.util.ProcessingStrategyUtils;
import org.mule.runtime.core.construct.Flow;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class FlowDefinitionParser extends OrphanDefinitionParser
{
    public FlowDefinitionParser()
    {
        super(Flow.class, true);
        addIgnored("abstract");
        addIgnored("name");
        addIgnored("processingStrategy");
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        builder.addConstructorArgValue(element.getAttribute(ATTRIBUTE_NAME));
        builder.addConstructorArgReference(MuleProperties.OBJECT_MULE_CONTEXT);
        ProcessingStrategyUtils.configureProcessingStrategy(element, builder,
            ProcessingStrategyUtils.ASYNC_PROCESSING_STRATEGY);
        super.doParse(element, parserContext, builder);
    }
}
