/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific.endpoint.support;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.generic.AutoIdUtils;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.util.StringUtils;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

/**
 * A parser for "embedded" endpoints - ie inbound, outbound and response endpoints.
 * Because we have automatic String -> MuleEnpointURI conversion via property editors
 * this can be used in a variety of ways. It should work directly with a simple
 * String address attribute or, in combination with a child element (handled by
 * {@link ChildAddressDefinitionParser}, or embedded in
 * {@link AddressedEndpointDefinitionParser} for a more compact single-element
 * approach.
 * <p>
 * This class does support references to other endpoints.
 * </p>
 * TODO - check that references are global!
 */
public class ChildEndpointDefinitionParser extends ChildDefinitionParser
{

    public ChildEndpointDefinitionParser(Class endpoint)
    {
        super("messageProcessor", endpoint);
        addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF);
        EndpointUtils.addProperties(this);
        EndpointUtils.addPostProcess(this);
    }

    @Override
    public BeanDefinitionBuilder createBeanDefinitionBuilder(Element element, Class beanClass)
    {
        BeanDefinitionBuilder builder = super.createBeanDefinitionBuilder(element, beanClass);
        String global = element.getAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF);
        if (StringUtils.isNotBlank(global))
        {
            builder.addConstructorArgReference(global);
            builder.addDependsOn(global);
        }
        return builder;
    }

    @Override
    public String getPropertyName(Element e)
    {
        String parent = e.getParentNode().getLocalName().toLowerCase();
        if (e.getLocalName() != null
            && (e.getLocalName().toLowerCase().endsWith("inbound-endpoint") || e.getLocalName()
                .toLowerCase()
                .equals("poll")))
        {
            return "messageSource";
        }
        else if ("wire-tap".equals(parent) || "wire-tap-router".equals(parent))
        {
            return "tap";
        }
        else if ("binding".equals(parent) || "java-interface-binding".equals(parent)
                 || "publish-notifications".equals(parent) || "remote-dispatcher-agent".equals(parent))
        {
            return "endpoint";
        }
        else
        {
            return super.getPropertyName(e);
        }
    }

    @Override
    public String getBeanName(Element element)
    {
        if (null != element.getAttributeNode(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF))
        {
            return AutoIdUtils.uniqueValue("ref:"
                                           + element.getAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF));
        }
        else
        {
            return super.getBeanName(element);
        }
    }
}
