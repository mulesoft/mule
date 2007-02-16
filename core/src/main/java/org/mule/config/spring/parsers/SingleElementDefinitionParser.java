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
 * TODO
 */
public class SingleElementDefinitionParser extends AbstractMuleSingleBeanDefinitionParser
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private Class beanClass;


    public SingleElementDefinitionParser(Class beanClass)
    {
        this.beanClass = beanClass;
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
