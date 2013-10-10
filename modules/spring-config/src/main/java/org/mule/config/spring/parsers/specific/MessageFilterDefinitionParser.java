/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
