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

import org.mule.util.StringUtils;

import java.util.List;
import java.util.Map;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This definition parser introduces the notion of Heirarchical processing to nested Xml elements. Definition
 * parsers that exnd this are always child beans that get set on the parent Definition Parser.
 *
 * A single method needs to be overriden called {@link #getPropertyName} that determines the name of the property to
 * set on the parent bean with this bean. Note that the property name can be dynamically resolved depending on the parent
 * element.
 *
 * This implementation also supports collections and Maps. For collections is a child element is repeated it will be assumed
 * that it is a collection.
 *
 * If the Bean Class for this element is set to {@link MapEntryDefinitionParser.KeyValuePair} it is assumed that a Map
 * is being processed and any child elements will be added to the parent Map.
 *
 *
 * @see SimpleChildDefinitionParser
 * @see MapEntryDefinitionParser.KeyValuePair
 * @see AbstractMuleSingleBeanDefinitionParser
 */
public abstract class AbstractChildBeanDefinitionParser extends AbstractMuleSingleBeanDefinitionParser
{
    //Make the registry available to the parsers in the post process method
    protected BeanDefinitionRegistry registry;

    protected final void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        //Child parces
        registry = parserContext.getRegistry();
        parseChild(element, parserContext, builder);
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        builder.setSingleton(isSingleton());
        super.doParse(element, parserContext, builder);
    }

    protected void postProcess(BeanDefinitionBuilder builder, Element element)
    {
        String parentBean = getParentBeanName(element);
        if (StringUtils.isBlank(parentBean))
        {
            //TODO RM*: This should probably be an exception
            logger.info("Bean: " + element.getNodeName() + " has no parent");
            return;
        }

        String name = generateChildBeanName(element);
        element.setAttribute(ATTRIBUTE_NAME, name);
        BeanDefinition parent = registry.getBeanDefinition(parentBean);

        String propertyName = getPropertyName(element);

        PropertyValue pv;
        pv = parent.getPropertyValues().getPropertyValue(propertyName);

        if (isMap(element))
        {
            if(pv==null)
            {
                 ManagedMap m = new ManagedMap();
                pv = new PropertyValue(propertyName, m);
                parent.getPropertyValues().addPropertyValue(pv);
            }
            MapEntryDefinitionParser.KeyValuePair pair = (MapEntryDefinitionParser.KeyValuePair)
                    builder.getBeanDefinition().getSource();
            ((Map) pv.getValue()).put(pair.getKey(), pair.getValue());

        }
        else if (isCollection(element))
        {
            if(pv==null)
            {
                pv = parent.getPropertyValues().getPropertyValue(propertyName + "s");
            }
            if(pv==null)
            {
                ManagedList l = new ManagedList();
                pv = new PropertyValue(propertyName + "s", l);
                parent.getPropertyValues().addPropertyValue(pv);
            }
            ((List) pv.getValue()).add(builder.getBeanDefinition());
        }
        else
        {
            pv = new PropertyValue(propertyName, builder.getBeanDefinition());
        }
        parent.getPropertyValues().addPropertyValue(pv);
    }

    protected String getParentBeanName(Element element)
    {
        return ((Element) element.getParentNode()).getAttribute(ATTRIBUTE_NAME);
    }

    protected String generateChildBeanName(Element e)
    {
        String parentId = ((Element) e.getParentNode()).getAttribute(ATTRIBUTE_NAME);
        //String parentBean = e.getLocalName() + ":" + ((Element) e.getParentNode()).getAttribute("id");
        String id = e.getAttribute(ATTRIBUTE_NAME);
        if (StringUtils.isBlank(id))
        {
            String idref = e.getAttribute(ATTRIBUTE_IDREF);
            if(StringUtils.isBlank(idref))
            {
                id = e.getLocalName();
            }
            else
            {
                id = "ref:" + idref;
            }

            if(!parentId.startsWith("."))
            {
                parentId = "." + parentId;
            }
            return parentId + ":" + id;
        }
        else
        {
            return id;
        }

    }

    public boolean isCollection(Element element)
    {
        return false;
    }

    protected final boolean isMap(Element element)
    {
        return getBeanClass(element).equals(MapEntryDefinitionParser.KeyValuePair.class);
    }

    public abstract String getPropertyName(Element e);
}
