/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.parsers;

import org.mule.util.StringUtils;

import java.util.List;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * todo document
 */
public abstract class AbstractChildBeanDefinitionParser extends AbstractMuleSingleBeanDefinitionParser
{

    protected boolean asList = false;

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
        super.doParse(element, parserContext, builder);
    }

    protected void postProcess(BeanDefinitionBuilder builder, Element element)
    {
        String parentBean = ((Element) element.getParentNode()).getAttribute(ATTRIBUTE_ID);
        if (StringUtils.isBlank(parentBean))
        {
            logger.info("Bean: " + element.getNodeName() + " has no parent");
            return;
        }

        String name = generateChildBeanName(element);
        element.setAttribute(ATTRIBUTE_ID, name);
        BeanDefinition parent = registry.getBeanDefinition(parentBean);

        String propertyName = getPropertyName(element);

        PropertyValue pv;
        pv = parent.getPropertyValues().getPropertyValue(propertyName);
//        if (pv == null)
//        {
//            pv = parent.getPropertyValues().getPropertyValue(propertyName += "s");
//        }
        //If the property has already been registered under the same name, we assume we're dealing with a list property
        if (pv == null && isCollection(element))
        {
//            if (!(pv.getValue() instanceof List))
//            {
                //Object o = pv.getValue();
                ManagedList l = new ManagedList();
               // l.add(o);
                //parent.getPropertyValues().removePropertyValue(propertyName);
                pv = new PropertyValue(propertyName + "s", l);
                parent.getPropertyValues().addPropertyValue(pv);
          //  }
            ((List) pv.getValue()).add(builder.getBeanDefinition());
        }
        else
        {
            pv = new PropertyValue(propertyName, builder.getBeanDefinition());
        }
        parent.getPropertyValues().addPropertyValue(pv);
    }

    protected String generateChildBeanName(Element e)
    {
        String parentId = ((Element) e.getParentNode()).getAttribute("id");
        //String parentBean = e.getLocalName() + ":" + ((Element) e.getParentNode()).getAttribute("id");
        String id = e.getAttribute("id");
        if (StringUtils.isBlank(id))
        {
            id = e.getLocalName();
            return "." + parentId + ":" + id;
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

    public abstract String getPropertyName(Element e);
}
