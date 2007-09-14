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
import org.mule.util.CoreXMLUtils;
import org.mule.util.StringUtils;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

public class AttributeListEntryDefinitionParser
        extends AbstractChildDefinitionParser implements DynamicAttributeDefinitionParser
{

    private String setterMethod;
    private String attributeName;

    /**
     * Only for use with dynamic naming
     */
    public AttributeListEntryDefinitionParser(String setterMethod)
    {
        this(setterMethod, null);
    }

    public AttributeListEntryDefinitionParser(String setterMethod, String attributeName)
    {
        this.setterMethod = setterMethod;
        setAttributeName(attributeName);
    }

    public String getPropertyName(Element element)
    {
        return setterMethod;
    }

    protected Class getBeanClass(Element element)
    {
        return ChildListEntryDefinitionParser.ListEntry.class;
    }

    public void setAttributeName(String attributeName)
    {
        this.attributeName = attributeName;
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        Attr attribute = element.getAttributeNode(attributeName);
        if (null == attribute || StringUtils.isEmpty(attribute.getNodeValue()))
        {
            throw new IllegalStateException(
                    "No value for " + attributeName + " in " + CoreXMLUtils.elementToString(element));
        }
        String value = attribute.getNodeValue();
        builder.setSource(new ChildListEntryDefinitionParser.ListEntry(value));
        this.postProcess(getBeanAssembler(element, builder), element);
    }

}
