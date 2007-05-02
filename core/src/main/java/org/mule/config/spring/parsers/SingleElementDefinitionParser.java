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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

/**
 * Contructs a single bean object from a element. This parser can be configured to automatically set the
 * class of the object, the init and destroy methods and whether this object is a singleton.
 */
public class SingleElementDefinitionParser extends AbstractMuleSingleBeanDefinitionParser
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private Class beanClass;


    public SingleElementDefinitionParser(Class beanClass, boolean singleton)
    {
        this.beanClass = beanClass;
        this.singleton = singleton;
    }

    protected Class getBeanClass(Element element)
    {
        if(beanClass==null)
        {
            String cls = element.getAttribute("class");
            try
            {
                //TODO TC: probably need to use OSGi Loader here
                beanClass = ClassUtils.loadClass(cls, getClass());
            }
            catch (ClassNotFoundException e)
            {
                logger.error("could not load class: " + cls, e);
            }
        }
        element.removeAttribute("class");
        return beanClass;
    }
}
