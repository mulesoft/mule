/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.collection;

import org.mule.module.springconfig.parsers.assembly.BeanAssembler;
import org.mule.module.springconfig.parsers.generic.ChildDefinitionParser;
import org.mule.module.springconfig.util.SpringXMLUtils;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;


/**
 * Process an element as a value that is appended to a list in the parent object (the
 * enclosing XML element).
 */
public class ChildListEntryDefinitionParser extends ChildDefinitionParser
{

    public static final String VALUE = "value";
    private boolean fromText = true;

    /**
     * Takes value from enclosed text
     *
     * @param propertyName
     */
    public ChildListEntryDefinitionParser(String propertyName)
    {
        super(propertyName, ListEntry.class);
        setIgnoredDefault(true);
    }

    /**
     * Takes value from attribute
     *
     * @param propertyName
     * @param attributeName
     */
    public ChildListEntryDefinitionParser(String propertyName, String attributeName)
    {
        this(propertyName);
        addAlias(attributeName, VALUE);
        removeIgnored(attributeName);
        fromText = false;
    }

    protected void postProcess(ParserContext context, BeanAssembler assembler, Element element)
    {
        if (fromText)
        {
            assembler.extendBean(VALUE, SpringXMLUtils.getTextChild(element), false);
        }
        super.postProcess(context, assembler, element);
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        super.parseChild(element, parserContext, builder);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public static class ListEntry
    {

        private Object value;

        public ListEntry()
        {
            super();
        }
        
        public ListEntry(Object proxied)
        {
            this();
            value = proxied;
        }

        public Object getValue()
        {
            return value;
        }

        public void setValue(Object value)
        {
            this.value = value;
        }   
    }

}
