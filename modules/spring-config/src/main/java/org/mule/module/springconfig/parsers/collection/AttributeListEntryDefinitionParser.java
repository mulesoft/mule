/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.collection;

import org.mule.module.springconfig.parsers.AbstractChildDefinitionParser;
import org.mule.module.springconfig.util.SpringXMLUtils;
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
                    "No value for " + attributeName + " in " + SpringXMLUtils.elementToString(element));
        }
        String value = attribute.getNodeValue();
        builder.getRawBeanDefinition().setSource(new ChildListEntryDefinitionParser.ListEntry(value));
        this.postProcess(parserContext, getBeanAssembler(element, builder), element);
    }

}
