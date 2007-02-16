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
 * the beanDefinition on the parent object
 */
public class SimpleChildDefinitionParser extends AbstractChildBeanDefinitionParser
{

    private Class clazz;
    private String setterMethod;

    public SimpleChildDefinitionParser(String setterMethod, Class clazz)
    {
        this.clazz = clazz;
        this.setterMethod = setterMethod;
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
        return clazz;
    }

    public String getPropertyName(Element e)
    {
        return setterMethod;
    }
}
