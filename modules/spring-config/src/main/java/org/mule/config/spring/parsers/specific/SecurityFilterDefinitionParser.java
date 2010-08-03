/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.api.security.EndpointSecurityFilter;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.assembly.BeanAssemblerFactory;
import org.mule.config.spring.parsers.assembly.DefaultBeanAssembler;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.WrappingChildDefinitionParser;
import org.mule.endpoint.SecurityFilterMessageProcessorBuilder;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.w3c.dom.Element;

import java.util.List;

/**
 * This allows a security filter to be defined globally, or embedded within an endpoint. The filter is
 * always wrapped in a SecurityFilterMessageProcessorBuilder instance before being injected into the parent.
 */
public class SecurityFilterDefinitionParser extends ParentContextDefinitionParser  implements WrappingChildDefinitionParser.WrappingController
{

    public static final String SECURITY_FILTER = "securityFilter";
    public static final String ATTRIBUTE_NAME = AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME;

    public SecurityFilterDefinitionParser(Class filter)
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENT, new OrphanDefinitionParser(filter, false));
        otherwise(
            new WrappingChildDefinitionParser(
                "messageProcessor", filter, EndpointSecurityFilter.class, false, SecurityFilterMessageProcessorBuilder.class,
                SECURITY_FILTER, SECURITY_FILTER, this));
        addIgnored(ATTRIBUTE_NAME);
    }

    public SecurityFilterDefinitionParser()
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENT, new OrphanDefinitionParser(false));
        otherwise(
            new WrappingChildDefinitionParser(
                "messageProcessor", null, EndpointSecurityFilter.class, true, SecurityFilterMessageProcessorBuilder.class,
                SECURITY_FILTER, SECURITY_FILTER, this));
        addIgnored(ATTRIBUTE_NAME);
    }

    public boolean shouldWrap(Element elm)
    {
        return true;
    }
}