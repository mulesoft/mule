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

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;

import org.w3c.dom.Element;

/**
 * Contructs a single, standalone bean from an element - it is not injected into any other object.
 * This parser can be configured to automatically set the class of the object, the init and destroy methods
 * and whether this object is a singleton.
 */
public class OrphanDefinitionParser extends AbstractMuleBeanDefinitionParser
{
    private Class beanClass = null;

    /**
     * This constructor assumes that the class name will be explicitly specified as an attribute on the element.
     */
    public OrphanDefinitionParser(boolean singleton)
    {
        this.singleton = singleton;
    }

    public OrphanDefinitionParser(Class beanClass, boolean singleton)
    {
        this.beanClass = beanClass;
        this.singleton = singleton;
    }

    protected Class getBeanClass(Element element)
    {
        return beanClass;
    }

}
