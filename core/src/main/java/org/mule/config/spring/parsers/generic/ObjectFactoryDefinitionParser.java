/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.generic;

import org.mule.util.StringUtils;
import org.mule.util.object.SimpleObjectFactory;
import org.mule.util.object.SingletonObjectFactory;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class ObjectFactoryDefinitionParser extends SimpleChildDefinitionParser
{
    public ObjectFactoryDefinitionParser(String setterMethod)
    {
        super(setterMethod, null);
        allowClassAttribute = false;
        withAlias("class", "objectClassName");
    }
    
    protected Class getBeanClass(Element element)
    {
        String scope = element.getAttribute("scope");
        // Default scope is "prototype"
        if (StringUtils.isBlank(scope))
        {
            scope = "prototype";
        }
        element.removeAttribute("scope");
        
        if (scope.equals("prototype"))
        {
            return SimpleObjectFactory.class;
        }
        else if (scope.equals("singleton"))
        {
            return SingletonObjectFactory.class;
        }
        else
        {
            logger.error("Scope " + scope + " not recognized.");
            return null;
        }
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        String factory = element.getAttribute("factory-ref");
        element.removeAttribute("factory-ref");
        if (StringUtils.isNotBlank(factory))
        {
            addParentPropertyValue(element, 
                new PropertyValue(getPropertyName(element), new RuntimeBeanReference(factory)));
        }
        else
        {
            super.parseChild(element, parserContext, builder);
        }
    }
}
