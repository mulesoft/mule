/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.spring.security.config;

import org.mule.api.config.MuleProperties;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.NamedDefinitionParser;
import org.mule.config.spring.parsers.specific.SecurityFilterDefinitionParser;
import org.mule.module.spring.security.AuthorizationFilter;
import org.mule.module.spring.security.SpringProviderAdapter;
import org.mule.module.spring.security.filters.http.HttpBasicAuthenticationFilter;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Registers a Bean Definition Parser for handling Spring Security related elements.
 */
public class SpringSecurityNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("security-manager", new NamedDefinitionParser(MuleProperties.OBJECT_SECURITY_MANAGER));
        registerBeanDefinitionParser("delegate-security-provider", new ChildDefinitionParser("provider", SpringProviderAdapter.class));
        registerBeanDefinitionParser("http-security-filter", new SecurityFilterDefinitionParser(HttpBasicAuthenticationFilter.class));
        registerBeanDefinitionParser("authorization-filter", new SecurityFilterDefinitionParser(AuthorizationFilter.class));
        registerBeanDefinitionParser("security-property", new ChildMapEntryDefinitionParser("securityProperty", "name", "value"));
    }

}
