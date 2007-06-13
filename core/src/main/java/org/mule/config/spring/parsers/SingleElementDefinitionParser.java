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

import org.w3c.dom.Element;

/**
 * Contructs a single bean object from a element. This parser can be configured to automatically set the
 * class of the object, the init and destroy methods and whether this object is a singleton.
 */
public class SingleElementDefinitionParser extends AbstractMuleSingleBeanDefinitionParser
{
    private Class beanClass = null;

    /**
     * This constructor assumes that the class name will be explicitly specified as an attribute on the element.
     */
    public SingleElementDefinitionParser(boolean singleton)
    {
        this.singleton = singleton;
    }

    public SingleElementDefinitionParser(Class beanClass, boolean singleton)
    {
        this.beanClass = beanClass;
        this.singleton = singleton;
    }

    protected Class getBeanClass(Element element)
    {
        return beanClass;
    }
}
