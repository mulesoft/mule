/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific.endpoint;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

/**
 * A parser for "embedded" endpoints - ie inbound, outbound and response endpoints.
 * Because we have automatic String -> MuleEnpointURI conversin via property editors
 * this can be used in a variety of ways.  It should work directly with a simple String
 * address attribute or, in combination with a child element (handled by
 * {@link org.mule.config.spring.parsers.specific.endpoint.ChildAddressDefinitionParser},
 * or embedded in
 * {@link org.mule.config.spring.parsers.specific.endpoint.AddressedEndpointDefinitionParser}
 * for a more compact single-eleent approach.
 */
public class ChildEndpointDefinitionParser extends ChildDefinitionParser
{

    public ChildEndpointDefinitionParser(Class endpoint)
    {
        super("endpoint", endpoint);
        EndpointUtils.addConditions(this);
        EndpointUtils.addPostProcess(this);
    }

    // @Override
    protected BeanDefinitionBuilder createBeanDefinitionBuilder(Element element, Class beanClass)
    {
        BeanDefinitionBuilder builder = EndpointUtils.createBeanDefinitionBuilder(element, beanClass);
        if (null == builder)
        {
            return super.createBeanDefinitionBuilder(element, beanClass);
        }
        else
        {
            return builder;
        }
    }

    // @Override
    protected String generateChildBeanName(Element element)
    {
        if (null != element.getAttributeNode(EndpointUtils.ENDPOINT_REF_ATTRIBUTE))
        {
            return "ref:" + element.getAttribute(EndpointUtils.ENDPOINT_REF_ATTRIBUTE);
        }
        else
        {
            return super.generateChildBeanName(element);
        }
    }

}
