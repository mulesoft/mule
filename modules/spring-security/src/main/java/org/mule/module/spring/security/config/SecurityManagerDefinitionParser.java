/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.security.config;

import org.mule.api.config.MuleProperties;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.generic.NamedDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.security.MuleSecurityManager;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This security manager delegates its parsing into two different definition parsers
 * based on the name of the element.
 *
 * If the name of the element matches "_muleSecurityManager", then the parser will
 * invoke ParentDefinitionParser which in turn will modify the existing _muleSecurityManager
 * bean instead of creating a new one.
 *
 * Now if the name is something different, then this parser will invoke the OrphanDefinitionParser
 * which instead of modifying an existing bean it will create a new one altogether.
 */
public class SecurityManagerDefinitionParser implements BeanDefinitionParser
{
    NamedDefinitionParser namedDefinitionParser;
    OrphanDefinitionParser orphanDefinitionParser;

    public SecurityManagerDefinitionParser()
    {
        this.namedDefinitionParser = new NamedDefinitionParser(MuleProperties.OBJECT_SECURITY_MANAGER);
        this.orphanDefinitionParser = new OrphanDefinitionParser(MuleSecurityManager.class, true);
        this.orphanDefinitionParser.addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
    }

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext)
    {
        if(element.hasAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME))
        {
            if(MuleProperties.OBJECT_SECURITY_MANAGER.equals(element.getAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME)))
            {
                element.removeAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
                return namedDefinitionParser.parse(element, parserContext);
            }
            else
            {
                return orphanDefinitionParser.parse(element, parserContext);
            }
        }
        else
        {
            return namedDefinitionParser.parse(element, parserContext);
        }
    }
}
