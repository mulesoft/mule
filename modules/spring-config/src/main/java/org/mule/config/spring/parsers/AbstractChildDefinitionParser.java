/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.generic.AutoIdUtils;
import org.mule.config.spring.util.SpringXMLUtils;
import org.mule.util.StringUtils;

import org.springframework.beans.factory.config.BeanDefinition;
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
public abstract class AbstractChildDefinitionParser
        extends AbstractHierarchicalDefinitionParser
        implements MuleChildDefinitionParser
{

    protected final void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        setRegistry(parserContext.getRegistry());
        parseChild(element, parserContext, builder);
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        builder.setScope(isSingleton() ? BeanDefinition.SCOPE_SINGLETON : BeanDefinition.SCOPE_PROTOTYPE);
        super.doParse(element, parserContext, builder);
    }

    protected void postProcess(ParserContext context, BeanAssembler assembler, Element element)
    {
        super.postProcess(context, assembler, element);

        // legacy handling of orphan beans - avoid setting parent
        String propertyName = getPropertyName(element);
        if (null != propertyName)
        {
            // If this is a singleton we need to inject it into parent using a
            // RuntimeBeanReference so that the bean does not get created twice, once
            // with a name and once as an (inner bean).
            if (!assembler.getBean().getBeanDefinition().isSingleton())
            {
                assembler.insertBeanInTarget(propertyName);
            }
            else
            {
                assembler.insertSingletonBeanInTarget(propertyName,
                    element.getAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME));
            }
        }
    }

    public String getBeanName(Element e)
    {
        String name = SpringXMLUtils.getNameOrId(e);
        if (StringUtils.isBlank(name))
        {
            String parentId = getParentBeanName(e);
            if (!parentId.startsWith("."))
            {
                parentId = "." + parentId;
            }
            return AutoIdUtils.uniqueValue(parentId + ":" + e.getLocalName());
        }
        else
        {
            return name;
        }
    }

    public abstract String getPropertyName(Element element);

}
