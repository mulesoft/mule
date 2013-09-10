/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.delegate;

import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.util.SpringXMLUtils;
import org.mule.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * Select sub parser depending on presence of a particular attribute
 */
public class AttributeSelectionDefinitionParser extends AbstractParallelDelegatingDefinitionParser
{

    private Map attributeToParserIndex = new HashMap();

    public AttributeSelectionDefinitionParser(String attribute, MuleDefinitionParser delegate)
    {
        super();
        addDelegate(attribute, delegate);
    }

    public void addDelegate(String attribute, MuleDefinitionParser delegate)
    {
        addDelegate(delegate);
        attributeToParserIndex.put(attribute, new Integer(size() - 1));
        delegate.setIgnoredDefault(true);
        delegate.removeIgnored(attribute);
    }

    protected MuleDefinitionParser getDelegate(Element element, ParserContext parserContext)
    {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); ++i)
        {
            String attribute = SpringXMLUtils.attributeName((Attr) attributes.item(i));
            if (attributeToParserIndex.containsKey(attribute))
            {
                return getDelegate(((Integer) attributeToParserIndex.get(attribute)).intValue());
            }
        }
        throw new IllegalArgumentException("Element " + SpringXMLUtils.elementToString(element) +
                " does not contain any attribute from " +
                CollectionUtils.toString(attributeToParserIndex.keySet(), 10, false));
    }

}
