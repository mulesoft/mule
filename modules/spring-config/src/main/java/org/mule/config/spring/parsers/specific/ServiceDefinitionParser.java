/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.processors.ProvideDefaultName;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class ServiceDefinitionParser extends OrphanDefinitionParser
{

    // for custom services
    public ServiceDefinitionParser()
    {
        super(true);
        registerPreProcessor(new ProvideDefaultName("service"));
    }

    public ServiceDefinitionParser(Class clazz)
    {
        super(clazz, true);
        registerPreProcessor(new ProvideDefaultName("service"));
    }

    @java.lang.Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        Element parent = (Element) element.getParentNode();
        String modelName = parent.getAttribute(ATTRIBUTE_NAME);
        builder.addPropertyReference("model", modelName);
        super.doParse(element, parserContext, builder);
    }

}
