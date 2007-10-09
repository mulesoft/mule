/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.util.StringUtils;
import org.mule.util.object.PooledObjectFactory;
import org.mule.util.object.SimpleObjectFactory;
import org.mule.util.object.SingletonObjectFactory;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

public class ObjectFactoryDefinitionParser extends ChildDefinitionParser
{
    public static final String FACTORY_REF = "factory-ref";
    
    protected Class beanClass = null;

    public ObjectFactoryDefinitionParser(Class beanClass, String setterMethod)
    {
        this(setterMethod);
        this.beanClass = beanClass;
    }                                                             
    
    public ObjectFactoryDefinitionParser(String setterMethod)
    {
        super(setterMethod, null);
        setAllowClassAttribute(false);
        addAlias("class", "objectClassName");
    }                                                             
    
    protected Class getBeanClass(Element element)
    {
        if (beanClass != null)
        {
            return beanClass;
        }
        
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
        else if (scope.equals("pooled"))
        {
            return PooledObjectFactory.class;
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
