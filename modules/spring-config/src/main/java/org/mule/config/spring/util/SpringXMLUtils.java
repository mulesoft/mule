/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.util;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.util.StringUtils;
import org.mule.util.XMLUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.w3c.dom.Element;

/**
 * These only depend on standard (JSE) XML classes and are used by Spring config code.
 * For a more extensive (sub-)class, see the XMLUtils class in the XML module.
 */
public class SpringXMLUtils extends XMLUtils
{

    private static final Log logger = LogFactory.getLog(SpringXMLUtils.class);

    public static final String MULE_DEFAULT_NAMESPACE = "http://www.mulesoft.org/schema/mule/core";
    public static final String MULE_NAMESPACE_PREFIX = "http://www.mulesoft.org/schema/mule/";

    public static boolean isMuleNamespace(Element element)
    {
        String ns = element.getNamespaceURI();
        return ns != null && ns.startsWith(MULE_NAMESPACE_PREFIX);
    }

    public static boolean isBeansNamespace(Element element)
    {
        String ns = element.getNamespaceURI();
        return ns != null && ns.equals(BeanDefinitionParserDelegate.BEANS_NAMESPACE_URI);
    }

    public static String getNameOrId(Element element)
    {
        String id = element.getAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_ID);
        String name = element.getAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
        if (StringUtils.isBlank(id))
        {
            if (StringUtils.isBlank(name))
            {
                return "";
            }
            else
            {
                return name;
            }
        }
        else
        {
            if (!StringUtils.isBlank(name) && !name.equals(id))
            {
                logger.warn("Id (" + id + ") and name (" + name + ") differ for " + elementToString(element));
            }
            return id;
        }
    }

}
