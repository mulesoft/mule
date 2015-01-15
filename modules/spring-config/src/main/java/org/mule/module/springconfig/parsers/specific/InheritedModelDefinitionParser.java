/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.specific;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Allows the <i>Inherited</i> model type to be used. This parser simply looks up the real Model bean and
 * passes that back.
 */
public class InheritedModelDefinitionParser extends AbstractBeanDefinitionParser
{

    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
    {
        String parent = element.getAttribute("name");
        element.setAttribute("id", parent);
        return (AbstractBeanDefinition)parserContext.getRegistry().getBeanDefinition(parent);
    }

}
