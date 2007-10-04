/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.collection;

import org.mule.config.spring.parsers.AbstractChildDefinitionParser;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;


/**
 * Process an element as a value that is appended to a list in the parent object (the
 * enclosing XML element).
 */
public class ChildListEntryDefinitionParser extends AbstractChildDefinitionParser
{

    private String propertyName;

    public ChildListEntryDefinitionParser(String propertyName)
    {
        super();
        this.propertyName = propertyName;
    }

    public String getPropertyName(Element element)
    {
        return propertyName;
    }

    protected Class getBeanClass(Element element)
    {
        return ListEntry.class;
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {   
        String beanType = element.getChildNodes().item(0).getNodeValue();
        builder.setSource(new ListEntry(beanType));
        postProcess(getBeanAssembler(element, builder), element);
    }
    
    public static class ListEntry extends Object
    {
        private Object proxiedObject;

        public ListEntry()
        {
            super();
        }
        
        public ListEntry(Object proxied)
        {
            this();
            proxiedObject = proxied;
        }

        public Object getProxiedObject()
        {
            return proxiedObject;
        }

        public void setProxiedObject(Object proxiedObject)
        {
            this.proxiedObject = proxiedObject;
        }   
    }

}
