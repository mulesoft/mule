/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.generic;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.util.StringUtils;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This class should be used when the same element can be configured as a child or an orphan 
 * (i.e., top-level).  It will inject the bean into the parent if the parent exists, otherwise 
 * it will not complain (ChildDefinitionParser throws an exception if no parent exists).
 */
public class OptionalChildDefinitionParser extends ChildDefinitionParser
{
    private boolean isChild;
    
    public OptionalChildDefinitionParser(String setterMethod)
    {
        super(setterMethod);
    }
    
    public OptionalChildDefinitionParser(String setterMethod, Class clazz)
    {
        super(setterMethod, clazz);
    }
    
    public OptionalChildDefinitionParser(String setterMethod, Class clazz, Class constraint)
    {
        super(setterMethod, clazz, constraint);
    }
    
    public OptionalChildDefinitionParser(String setterMethod, Class clazz, Class constraint, boolean allowClassAttribute)
    {
        super(setterMethod, clazz, constraint, allowClassAttribute);
    }

    @Override
    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        isChild = isChild(element, parserContext, builder);
        super.parseChild(element, parserContext, builder);
    }

    protected boolean isChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        String parentBean = getParentBeanName(element);
        return !(StringUtils.isBlank(parentBean));
    }
    
    public BeanDefinition getParentBeanDefinition(Element element)
    {
        if (isChild)
        {
            return super.getParentBeanDefinition(element);
        }
        else
        {
            return null;
        }
    }

    protected void postProcess(ParserContext context, BeanAssembler assembler, Element element)
    {
        if (isChild)
        {
            super.postProcess(context, assembler, element);
        }
    }
}


