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

import org.w3c.dom.Element;

/**
 * Creates a definitionparser that will construct a single child element and set
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

    private Class constraint;
    private Class clazz;
    private String setterMethod;
    private boolean isDynamic;

    public SimpleChildDefinitionParser(String setterMethod, Class clazz)
    {
        this(setterMethod, clazz, null);
    }

    public SimpleChildDefinitionParser(String setterMethod, Class clazz, Class constraint)
    {
        this.constraint = constraint;
        this.clazz = clazz;
        this.setterMethod = setterMethod;
        isDynamic = null == clazz;
    }

    protected void preProcess()
    {
        if (isDynamic)
        {
           clazz = null; // reset for this element
        }
    }

    protected Class getBeanClass(Element element)
    {
        if (clazz == null)
        {
            String cls = element.getAttribute(ATTRIBUTE_CLASS);
            try
            {
                //TODO TC: probably need to use OSGi Loader here
                clazz = ClassUtils.loadClass(cls, getClass());
            }
            catch (ClassNotFoundException e)
            {
                logger.error("could not load class: " + cls, e);
            }
        }
        element.removeAttribute(ATTRIBUTE_CLASS);
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
