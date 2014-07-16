/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.PostProcessor;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.routing.MessageFilter;

import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class MessageFilterDefinitionParser extends MessageProcessorDefinitionParser implements PostProcessor
{
    private static final String ATTRIBUTE_UNACCEPTED = "onUnaccepted";

    public MessageFilterDefinitionParser()
    {
        super(MessageFilter.class);
        addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
        addIgnored(ATTRIBUTE_UNACCEPTED);
        registerPostProcessor(this);
    }

    public MessageFilterDefinitionParser(Class<?> filterClass)
    {
        super(filterClass);
        addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
        addIgnored(ATTRIBUTE_UNACCEPTED);
        registerPostProcessor(this);
    }

    
    public void postProcess(ParserContext context, BeanAssembler assembler, Element element)
    {
        String onUnaccepted = element.getAttribute(ATTRIBUTE_UNACCEPTED);
        if (onUnaccepted != null)
        {
            assembler.extendBean("unacceptedMessageProcessor", onUnaccepted, true);
        }
    }
}
