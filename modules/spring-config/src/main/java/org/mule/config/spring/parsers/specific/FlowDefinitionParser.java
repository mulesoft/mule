/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.api.config.MuleProperties;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.util.ProcessingStrategyUtils;
import org.mule.construct.Flow;

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

    @java.lang.Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        builder.addConstructorArgValue(element.getAttribute(ATTRIBUTE_NAME));
        builder.addConstructorArgReference(MuleProperties.OBJECT_MULE_CONTEXT);
        ProcessingStrategyUtils.configureProcessingStrategy(element, builder,
            ProcessingStrategyUtils.QUEUED_ASYNC_PROCESSING_STRATEGY);
        super.doParse(element, parserContext, builder);
    }
}
