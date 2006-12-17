/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extras.spring.config.parsers;

import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.mule.util.ClassUtils;

/**
 * TODO
 */
public class CustomElementDefinitionParser extends AbstractSingleBeanDefinitionParser
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected Class getBeanClass(Element element)
    {
        String cls = element.getAttribute("class");
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
