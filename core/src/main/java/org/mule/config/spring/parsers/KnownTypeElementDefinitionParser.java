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

import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

/**
 * TODO
 */
public class KnownTypeElementDefinitionParser extends AbstractSingleBeanDefinitionParser
{
    private Class beanClass;


    public KnownTypeElementDefinitionParser(Class beanClass)
    {
        this.beanClass = beanClass;
    }

    protected Class getBeanClass(Element element)
    {
        return beanClass;
    }
}
