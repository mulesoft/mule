/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.collection;

import org.mule.config.spring.parsers.AbstractChildDefinitionParser;
import org.mule.config.spring.util.SpringXMLUtils;
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
