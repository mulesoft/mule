/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.generic;

import org.mule.config.spring.parsers.AbstractChildBeanDefinitionParser;

import org.w3c.dom.Element;

/**
 * Creates a definition parser that will construct a single child element and set
 * the beanDefinition on the parent object.
 *
 * The parser will set all attributes defined in the Xml as bean properties and will
 * process any nested elements as bean properties too, except the correct Definition parser
 * for the element will be looked up automatically.
 *
 * If the class is read from an attribute (when class is null) then it is checked against
 * the constraint - it must be a subclass of the constraint.
 */
public class SimpleChildDefinitionParser extends AbstractChildBeanDefinitionParser
{
    protected Class constraint;
    protected Class clazz;
    protected String setterMethod;
    protected boolean resetEachUseMule1735;

    public SimpleChildDefinitionParser(String setterMethod, Class clazz)
    {
        this(setterMethod, clazz, null);
    }

    public SimpleChildDefinitionParser(String setterMethod, Class clazz, Class constraint)
    {
        this.constraint = constraint;
        this.clazz = clazz;
        this.setterMethod = setterMethod;
        resetEachUseMule1735 = null == clazz;
    }

    protected void preProcess()
    {
        if (resetEachUseMule1735)
        {
           clazz = null; // reset for this element
        }
    }

    protected Class getBeanClass(Element element)
    {
        if (null != clazz && null != constraint && !constraint.isAssignableFrom(clazz))
        {
            logger.error(clazz + " not a subclass of " + constraint);
            clazz = null;
        }
        return clazz;
    }

    public String getPropertyName(Element e)
    {
        return setterMethod;
    }

}
