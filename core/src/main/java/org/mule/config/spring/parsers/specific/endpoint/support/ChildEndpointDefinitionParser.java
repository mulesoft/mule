/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific.endpoint.support;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

/**
 * A parser for "embedded" endpoints - ie inbound, outbound and response endpoints.
 * Because we have automatic String -> MuleEnpointURI conversin via property editors
 * this can be used in a variety of ways.  It should work directly with a simple String
 * address attribute or, in combination with a child element (handled by
 * {@link ChildAddressDefinitionParser},
 * or embedded in
 * {@link AddressedEndpointDefinitionParser}
 * for a more compact single-eleent approach.
 *
 * <p>This class does support references to other endpoints.</p>
 * TODO - check that references are global!
 */
public class ChildEndpointDefinitionParser extends ChildDefinitionParser
{

    public static final String ENDPOINT_REF_ATTRIBUTE = "ref";

    public ChildEndpointDefinitionParser(Class endpoint)
    {
        super("endpoint", endpoint);
        addIgnored(ENDPOINT_REF_ATTRIBUTE);
        EndpointUtils.addProperties(this);
        EndpointUtils.addPostProcess(this);
    }

    // @Override
    public BeanDefinitionBuilder createBeanDefinitionBuilder(Element element, Class beanClass)
    {
        if (null == element.getAttributeNode(ENDPOINT_REF_ATTRIBUTE))
        {
            return super.createBeanDefinitionBuilder(element, beanClass);
        }
        else
        {
            String parent = element.getAttribute(ENDPOINT_REF_ATTRIBUTE);
            BeanDefinitionBuilder bdb = BeanDefinitionBuilder.childBeanDefinition(parent);
            bdb.getBeanDefinition().setBeanClassName(beanClass.getName());
            return bdb;
        }
    }

    // @Override
    protected String generateChildBeanName(Element element)
    {
        if (null != element.getAttributeNode(ENDPOINT_REF_ATTRIBUTE))
        {
            // why do we do this?  it doesn't seem to be used anwhere else
            // is it to avoid having to specify a name?  if so, we should
            // perhaps check to see if a name is given?  TODO
            return "ref:" + element.getAttribute(ENDPOINT_REF_ATTRIBUTE);
        }
        else
        {
            return super.generateChildBeanName(element);
        }
    }

}
