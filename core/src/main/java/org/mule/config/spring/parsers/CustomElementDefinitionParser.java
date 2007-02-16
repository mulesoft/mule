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
public class CustomElementDefinitionParser extends AbstractMuleSingleBeanDefinitionParser
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected Class getBeanClass(Element element)
    {
        String cls = element.getAttribute(ATTRIBUTE_CLASS);
        element.removeAttribute(ATTRIBUTE_CLASS);
        try
        {
            //RM* Todo probably need to use OSGi Loader here
            return ClassUtils.loadClass(cls, getClass());
        }
        catch (ClassNotFoundException e)
        {
            logger.error("could not load class: " + cls, e);
            return null;
        }
    }
}
