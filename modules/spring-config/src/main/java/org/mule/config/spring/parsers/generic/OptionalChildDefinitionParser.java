/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


