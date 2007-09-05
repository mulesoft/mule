/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.generic;

import org.mule.config.spring.parsers.AbstractChildDefinitionParser;
import org.mule.config.spring.parsers.delegate.DelegateDefinitionParser;

import org.w3c.dom.Element;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * Creates a definition parser that will construct a single child element and inject it into
 * the parent object (the enclosing XML element).
 *
 * The parser will set all attributes defined in the Xml as bean properties and will
 * process any nested elements as bean properties too, except the correct Definition parser
 * for the element will be looked up automatically.
 */
public class ChildDefinitionParser extends AbstractChildDefinitionParser implements DelegateDefinitionParser
{

    protected Class clazz;
    protected String setterMethod;

    /**
     * The class will be inferred from the class attribute
     * @param setterMethod The target method (where the child will be injected)
     */
    public ChildDefinitionParser(String setterMethod)
    {
        this(setterMethod, null, null, true);
    }

    /**
     * @param setterMethod The target method (where the child will be injected)
     * @param clazz The class created by this element/parser
     */
    public ChildDefinitionParser(String setterMethod, Class clazz)
    {
        this(setterMethod, clazz, null, null == clazz);
    }

    /**
     * The class (which is inferred from the class attribute if null here) is checked to be
     * a subclass of the constraint
     * @param setterMethod The target method (where the child will be injected)
     * @param clazz The class created by this element/parser (may be null)
     * @param constraint Superclass of clazz (may be null) 
     */
    public ChildDefinitionParser(String setterMethod, Class clazz, Class constraint)
    {
        this(setterMethod, clazz, constraint, null == clazz);
    }

    /**
     * The class (which is inferred from the class attribute if null here) is checked to be
     * a subclass of the constraint.
     *
     * @param setterMethod The target method (where the child will be injected)
     * @param clazz The class created by this element/parser (may be null)
     * @param constraint Superclass of clazz (may be null)
     * @param allowClassAttribute Is class read from class attribute (if present, takes precedence over clazz)
     */
    public ChildDefinitionParser(String setterMethod, Class clazz, Class constraint, boolean allowClassAttribute)
    {
        this.clazz = clazz;
        this.setterMethod = setterMethod;
        setClassConstraint(constraint);
        setAllowClassAttribute(allowClassAttribute);
    }

    protected void preProcess()
    {
        super.preProcess();
        if (isAllowClassAttribute())
        {
           clazz = null; // reset for this element
        }
    }

    protected Class getBeanClass(Element element)
    {
        return clazz;
    }

    public String getPropertyName(Element e)
    {
        return setterMethod;
    }

    public AbstractBeanDefinition parseDelegate(Element element, ParserContext parserContext)
    {
        return parseInternal(element, parserContext);
    }

}
