/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.activiti.config;

import java.util.HashMap;
import java.util.Map;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.MapFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

public class ActionChildDefinitionParser extends ChildDefinitionParser
{
    private String key;
    
    private Class valueClass;
    
    private static AtomicInteger actionsCount = new AtomicInteger(0);

    public ActionChildDefinitionParser(String key, Class clazz)
    {
        super("properties", HashMap.class);
        
        this.key = key;
        this.valueClass = clazz;
        setIgnoredDefault(true);
    }
    
    protected Class getBeanClass(Element element)
    {
        return MapFactoryBean.class;
    }
    
    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        super.parseChild(element, parserContext, builder);
        
        Map result = new ManagedMap();

        element.setAttribute("class", this.valueClass.getCanonicalName());
        element.setAttribute("name", "_activiti_action_" + actionsCount.incrementAndGet());

        
        BeanDefinition definition = new OrphanDefinitionParser(this.valueClass, false).parse(element, parserContext);
        
        result.put(this.key, definition);
        
        builder.addPropertyValue("sourceMap", result);
        builder.addPropertyValue("targetMapClass", HashMap.class);
    }
}
