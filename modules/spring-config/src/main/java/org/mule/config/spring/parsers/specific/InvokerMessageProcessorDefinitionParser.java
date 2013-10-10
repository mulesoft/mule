/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.processor.InvokerMessageProcessor;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class InvokerMessageProcessorDefinitionParser extends ChildDefinitionParser
{
    private final Class<?> objectType;
    private final String methodName;
    private final String[] parameterNames;

    public InvokerMessageProcessorDefinitionParser(String setterMethod,
                                         Class<?> objectType,
                                         String methodName,
                                         String[] parameterNames)
    {
        super(setterMethod, InvokerMessageProcessor.class);
        this.objectType = objectType;
        this.methodName = methodName;
        this.parameterNames = parameterNames;
    }

    @Override
    protected Class<?> getBeanClass(Element element)
    {
        return InvokerMessageProcessor.class;
    }

    @Override
    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        if (!StringUtils.isEmpty(element.getAttribute(getTargetPropertyConfiguration().getAttributeAlias(
            "config-ref"))))
        {
            builder.addPropertyReference("object",
                element.getAttribute(getTargetPropertyConfiguration().getAttributeAlias("config-ref")));
        }
        else
        {
            builder.addPropertyValue("objectType", objectType);
        }

        List<String> expressions = new ArrayList<String>();
        if (parameterNames != null)
        {
            for (String parameterName : parameterNames)
            {
                if (!StringUtils.isEmpty(element.getAttribute(parameterName)))
                {
                    expressions.add(element.getAttribute(parameterName));
                }
                else
                {
                    expressions.add(null);
                }
            }
        }
        builder.addPropertyValue("arguments", expressions);
        builder.addPropertyValue("methodName", methodName);

        BeanAssembler assembler = getBeanAssembler(element, builder);
        postProcess(getParserContext(), assembler, element);
    }

}
