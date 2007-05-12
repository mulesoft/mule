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

import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This definition parser supports the definition of beans that are then set on the parent bean.
 * It also extends {@link org.mule.config.spring.parsers.AbstractChildDefinitionParser} with
 * methods that assume the data are associated with a single property.
 *
 * This supports collections and Maps. For collections if a child element is repeated it will be assumed
 * that it is a collection.
 *
 * If the Bean Class for this element is set to {@link MapEntryDefinitionParser.KeyValuePair} it is assumed that a Map
 * is being processed and any child elements will be added to the parent Map.
 *
 * A single method needs to be overriden called {@link #getPropertyName} that determines the name of the property to
 * set on the parent bean with this bean. Note that the property name can be dynamically resolved depending on the parent
 * element.
 *
 * @see SimpleChildDefinitionParser
 * @see MapEntryDefinitionParser.KeyValuePair
 * @see AbstractMuleSingleBeanDefinitionParser
 */
public abstract class AbstractChildBeanDefinitionParser extends AbstractChildDefinitionParser
{

    protected final void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        setRegistry(parserContext.getRegistry());
        parseChild(element, parserContext, builder);
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        builder.setSingleton(isSingleton());
        super.doParse(element, parserContext, builder);
    }

    protected void postProcess(BeanDefinitionBuilder builder, Element element)
    {
        String name = generateChildBeanName(element);
        element.setAttribute(ATTRIBUTE_NAME, name);

        //Some objects may or may not have a parent.  We need to check if
        //If this bean has a property name (that will be set on the parent)
        //If not, we can skip the post processing here
        if(getPropertyName(element)==null)
        {
            return;
        }
        PropertyValue pv;
        try
        {
            pv = getParentPropertyValue(element);
        }
        catch (Exception e)
        {
            // MULE-1737 - remove this once fixed.
            //RM*: I think we should still leave this in here for the time being, JIC
            logger.warn("Skipping process for " + element, e);
            return;
        }

        if (isMap(element))
        {
            if (pv == null)
            {
                pv = newParentPropertyValue(element, new ManagedMap());
            }
            MapEntryDefinitionParser.KeyValuePair pair =
                    (MapEntryDefinitionParser.KeyValuePair) builder.getBeanDefinition().getSource();
            ((Map) pv.getValue()).put(pair.getKey(), pair.getValue());

        }
        else if (isCollection(element))
        {
            if (pv == null)
            {
                pv = newParentPropertyValue(element, new ManagedList());
            }
            ((List) pv.getValue()).add(builder.getBeanDefinition());
        }
        else
        {
            pv = newParentPropertyValue(element, builder.getBeanDefinition());
        }
        addParentPropertyValue(element, pv);
    }

    protected String generateChildBeanName(Element e)
    {
        String parentId = getParentBeanId(e);
        String id = e.getAttribute(ATTRIBUTE_NAME);
        if (StringUtils.isBlank(id))
        {
            String idref = e.getAttribute(ATTRIBUTE_IDREF);
            if (StringUtils.isBlank(idref))
            {
                id = e.getLocalName();
            }
            else
            {
                id = "ref:" + idref;
            }

            if (!parentId.startsWith("."))
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

    protected PropertyValue getParentPropertyValue(Element element)
    {
        return getParentBeanDefinition(element)
                .getPropertyValues().getPropertyValue(getBestGuessName(element));
    }

    protected PropertyValue newParentPropertyValue(Element element, Object value)
    {
        return new PropertyValue(getBestGuessName(element), value);
    }

    protected String getBestGuessName(Element element)
    {
        String name = getPropertyName(element);
        if (! isCollection(element))
        {
            return name;
        }
        else
        {
            BeanDefinition parent = getParentBeanDefinition(element);
            try
            {
                // is there a better way than this?!
                // BeanWrapperImpl instantiates an instance, which we don't want.
                // if there really is no better way, i guess it should go in
                // class or bean utils.
                Class clazz = ClassUtils.getClass(parent.getBeanClassName());
                Method[] methods = clazz.getMethods();
                String setter = "set" + name;
                for (int i = 0; i < methods.length; ++i)
                {
                    if (methods[i].getName().equalsIgnoreCase(setter))
                    {
                        return name;
                    }
                }
                // otherwise, guess this
                return name + "s";
            }
            catch (Exception e)
            {
                logger.debug("Could not access bean class " + parent.getBeanClassName(), e);
                return name;
            }
        }
    }

    public abstract String getPropertyName(Element element);

    public boolean isCollection(Element element)
    {
        return false;
    }

    protected final boolean isMap(Element element)
    {
        return getBeanClass(element).equals(MapEntryDefinitionParser.KeyValuePair.class);
    }

}
