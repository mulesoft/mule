/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
