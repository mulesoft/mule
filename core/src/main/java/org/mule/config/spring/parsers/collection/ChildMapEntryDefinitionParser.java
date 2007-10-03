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
 * Allows a series of key value pair elements to be set on the parent object (the enclosing XML element).
 * There is no need to define a surrounding 'map' element to contain the map entries.
 * This is useful for key value pair mappings for example -
 *
 * <code>
 *   <mule:endpoint name="fruitBowlEndpoint" address="test://fruitBowlPublishQ">
 *       <xm:jxpath-filter expectedValue="bar" expression="name">
 *           <xm:namespace prefix="foo" uri="http://foo.com"/>
 *           <xm:namespace prefix="bar" uri="http://bar.com"/>
 *       </xm:jxpath-filter>
 *   </mule:endpoint>
 * </code>
 *
 * Here <xm:namespace> refers to a Map of prefix/uri values.
 */
public class ChildMapEntryDefinitionParser extends AbstractChildDefinitionParser
{
    private String propertyName;
    private String keyName;
    private String valueName;

    public ChildMapEntryDefinitionParser(String mapName, String keyName, String valueName)
    {
        this.propertyName = mapName;
        this.keyName = keyName;
        this.valueName = valueName;
    }

    public String getPropertyName(Element e)
    {
        return propertyName;
    }

    protected Class getBeanClass(Element element)
    {
        return ChildMapEntryDefinitionParser.KeyValuePair.class;
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        String key = element.getAttribute(keyName);
        String value = element.getAttribute(valueName);
        builder.setSource(new ChildMapEntryDefinitionParser.KeyValuePair(key, value));
        postProcess(getBeanAssembler(element, builder), element);
    }

    public static class KeyValuePair
    {
        private String key;
        private Object value;

        public KeyValuePair()
        {
        }

        public KeyValuePair(String key, Object value)
        {
            this.key = key;
            this.value = value;
        }

        public String getKey()
        {
            return key;
        }

        public Object getValue()
        {
            return value;
        }


        public void setKey(String key)
        {
            this.key = key;
        }

        public void setValue(Object value)
        {
            this.value = value;
        }

    }

}
