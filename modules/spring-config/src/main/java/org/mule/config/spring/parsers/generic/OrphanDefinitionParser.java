/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.generic;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;

import org.w3c.dom.Element;

/**
 * <p>
 * Contructs a single, standalone bean from an element - it is not injected into any
 * other object. This parser can be configured to automatically set the class of the
 * object, the init and destroy methods and whether this object is a singleton.
 * </p>
 * <p>
 * Typically, you should use {@link MuleOrphanDefinitionParser} instead of this
 * class, since these elements occur in the <mule> top level element.
 * </p>
 */
public class OrphanDefinitionParser extends AbstractMuleBeanDefinitionParser
{
    private Class<?> beanClass = null;
    private boolean dynamicClass = false;

    /**
     * This constructor assumes that the class name will be explicitly specified as
     * an attribute on the element.
     */
    public OrphanDefinitionParser(boolean singleton)
    {
        this.singleton = singleton;
        dynamicClass = true;
    }

    public OrphanDefinitionParser(Class<?> beanClass, boolean singleton)
    {
        this.beanClass = beanClass;
        this.singleton = singleton;
    }

    @Override
    protected void preProcess(Element element)
    {
        super.preProcess(element);
        // top level beans need an ID element
        AutoIdUtils.ensureUniqueId(element, "bean");
        if (dynamicClass)
        {
            beanClass = null;
        }
    }

    @Override
    protected Class<?> getBeanClass(Element element)
    {
        return beanClass;
    }
}
