/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.util.StringUtils;
import org.mule.util.object.SimpleObjectFactory;
import org.mule.util.object.SingletonObjectFactory;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

public class ObjectFactoryDefinitionParser extends ChildDefinitionParser
{
    public static final String FACTORY_REF = "factory-ref";

    public ObjectFactoryDefinitionParser(String setterMethod)
    {
        super(setterMethod, null);
        setAllowClassAttribute(false);
        addAlias("class", "objectClassName");
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
        Attr factory = element.getAttributeNode(FACTORY_REF);
        if (null != factory)
        {
            getBeanAssembler(element, builder).extendTarget(factory);
            element.removeAttributeNode(factory);
        }
        else
        {
            super.parseChild(element, parserContext, builder);
        }
    }

}
