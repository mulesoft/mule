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
 * Allows a series of key vlaue pair elements to be set on an object as a Map. There is no need to define
 * a surrounding 'map' element to contain the map entries.
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
public class MapEntryDefinitionParser extends AbstractChildBeanDefinitionParser
{
    private String propertyName;


    public MapEntryDefinitionParser(String mapName)
    {
        this.propertyName = mapName;
    }

    public String getPropertyName(Element e)
    {
        return propertyName;
    }

    protected Class getBeanClass(Element element)
    {
        return MapEntryDefinitionParser.KeyValuePair.class;
    }


    public final boolean isCollection(Element element)
    {
        return true;
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        final String prefix = element.getAttribute("prefix");
        final String uri = element.getAttribute("uri");

        builder.setSource(new MapEntryDefinitionParser.KeyValuePair(prefix, uri));
        postProcess(builder, element);
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
