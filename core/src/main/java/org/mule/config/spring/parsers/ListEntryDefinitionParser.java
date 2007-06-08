/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;


/**
 * 
 */
public class ListEntryDefinitionParser extends AbstractChildBeanDefinitionParser
{
    private String propertyName;

    public ListEntryDefinitionParser(String propertyName)
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

    public boolean isCollection(Element element)
    {
        return true;
    }

    protected boolean isMap(Element element)
    {
        return false;
    }
    
    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {   
        // axis:beanType has exacly one child node: the text
        String beanType = element.getChildNodes().item(0).getNodeValue();
        builder.setSource(new ListEntry(beanType));
        this.postProcess(builder, element);
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
