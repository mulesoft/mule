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

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Contructs a single, standalone bean from an element - it is not injected into any other object.
 * This parser can be configured to automatically set the class of the object, the init and destroy methods
 * and whether this object is a singleton.
 *
 * <p>Typically, you should use {@link org.mule.config.spring.parsers.generic.MuleChildDefinitionParser}
 * instead of this class, since these elements occur in the <mule> top level element.</p>
 */
public class OrphanDefinitionParser extends AbstractMuleBeanDefinitionParser implements DelegateDefinitionParser
{

    private Class beanClass = null;
    private boolean dynamicClass = false;

    /**
     * This constructor assumes that the class name will be explicitly specified as an attribute on the element.
     */
    public OrphanDefinitionParser(boolean singleton)
    {
        this.singleton = singleton;
        dynamicClass = true;
    }

    public OrphanDefinitionParser(Class beanClass, boolean singleton)
    {
        this.beanClass = beanClass;
        this.singleton = singleton;
    }

    // @Override
    protected void preProcess()
    {
        super.preProcess();
        if (dynamicClass)
        {
            beanClass = null;
        }
    }

    // @Override
    protected Class getBeanClass(Element element)
    {
        return beanClass;
    }

    public AbstractBeanDefinition parseDelegate(Element element, ParserContext parserContext)
    {
        return parseInternal(element, parserContext);
    }

}