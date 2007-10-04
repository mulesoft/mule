/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.delegate;

import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.MuleChildDefinitionParser;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.assembly.MapBeanAssemblerFactory;
import org.mule.config.spring.parsers.assembly.PropertyConfiguration;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.w3c.dom.Element;

/**
 * This changes a {@link org.mule.config.spring.parsers.generic.ChildDefinitionParser}
 * so that it generates a map instead of a bean definition.  This is useful for converting
 * parsers to work with the object factory (which requires a map).
 */
public class MapDefinitionParserMutator
        extends AbstractDelegatingDefinitionParser
        implements MapBeanAssemblerFactory.BeanAssemblerStore, MuleChildDefinitionParser
{

    private String setter;
    private Element currentElement;
    private Map pendingAssemblers = new HashMap();

    public MapDefinitionParserMutator(String setter, ChildDefinitionParser delegate)
    {
        super(new MuleDefinitionParser[]{delegate});
        this.setter = setter;
        // this is where we set the callback
        delegate.setBeanAssemblerFactory(new MapBeanAssemblerFactory(this));
    }

    public AbstractBeanDefinition parseDelegate(Element element, ParserContext parserContext)
    {
        if (pendingAssemblers.containsKey(element))
        {
            // this is the second call, after the children have been processed
            BeanAssembler beanAssembler = (BeanAssembler) pendingAssemblers.get(element);
            pendingAssemblers.remove(element);
            beanAssembler.insertBeanInTarget(setter);
            return null;
        }
        else
        {
            // first call, so process in normal manner, but set current element so that
            // when the store callback is called, we can associate the assembler with this
            // element
            currentElement = element;
            return getChildDelegate().parseDelegate(element, parserContext);
        }
    }

    public void saveBeanAssembler(BeanAssembler beanAssembler)
    {
        // this is called by the bean assembler from inside parseDelegate above.
        pendingAssemblers.put(currentElement, beanAssembler);
    }

    protected ChildDefinitionParser getChildDelegate()
    {
        return (ChildDefinitionParser) getDelegate(0);
    }

    public void forceParent(BeanDefinition parent)
    {
        getChildDelegate().forceParent(parent);
    }

    public PropertyConfiguration getTargetPropertyConfiguration()
    {
        return getChildDelegate().getTargetPropertyConfiguration();
    }

}
