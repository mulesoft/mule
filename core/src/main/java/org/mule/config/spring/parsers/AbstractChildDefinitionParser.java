/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.util.StringUtils;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This definition parser supports the definition of beans that are then set on the parent bean -
 * it extends {@link org.mule.config.spring.parsers.AbstractHierarchicalDefinitionParser} with
 * methods that assume the data are associated with a single property.
 *
 * This supports collections and Maps. For collections if a child element is repeated it will be assumed
 * that it is a collection.
 *
 * If the Bean Class for this element is set to
 * {@link org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser.KeyValuePair} it is assumed that a Map
 * is being processed and any child elements will be added to the parent Map.  Similarly for
 * {@link org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser}.
 *
 * A single method needs to be overriden called {@link #getPropertyName} that determines the name of the property to
 * set on the parent bean with this bean. Note that the property name can be dynamically resolved depending on the parent
 * element.
 *
 * @see org.mule.config.spring.parsers.generic.ChildDefinitionParser
 * @see org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser.KeyValuePair
 * @see AbstractMuleBeanDefinitionParser
 */
public abstract class AbstractChildDefinitionParser extends AbstractHierarchicalDefinitionParser
{

    private static AtomicInteger idCounter = new AtomicInteger(0);

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

    protected void postProcess(BeanAssembler assembler, Element element)
    {
        String name = generateChildBeanName(element);
        element.setAttribute(ATTRIBUTE_NAME, name);

        // legacy handling of orphan beans - avoid setting parent
        if (null != getPropertyName(element))
        {
            assembler.insertBeanInTarget(getPropertyName(element));
        }

        super.postProcess(assembler, element);
    }

    protected String generateChildBeanName(Element e)
    {
        String parentId = getParentBeanName(e);
        String id = e.getAttribute(ATTRIBUTE_NAME);
        if (StringUtils.isBlank(id))
        {
            if (!parentId.startsWith("."))
            {
                parentId = "." + parentId;
            }
            return parentId + ":" + getBeanName(e);
        }
        else
        {
            return id;
        }
    }

    // guarantee a unique value
    protected String getBeanName(Element e)
    {
        return e.getLocalName() + "/" + idCounter.incrementAndGet();
    }

    public abstract String getPropertyName(Element element);

}
