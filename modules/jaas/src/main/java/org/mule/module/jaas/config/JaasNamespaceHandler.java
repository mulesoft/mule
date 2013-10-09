/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jaas.config;

import org.mule.api.config.MuleProperties;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.NamedDefinitionParser;
import org.mule.config.spring.parsers.specific.SecurityFilterDefinitionParser;
import org.mule.module.jaas.JaasSimpleAuthenticationProvider;
import org.mule.module.jaas.filters.JaasSecurityFilter;
import org.mule.security.PasswordBasedEncryptionStrategy;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class JaasNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("security-manager", new NamedDefinitionParser(MuleProperties.OBJECT_SECURITY_MANAGER));
        registerBeanDefinitionParser("security-provider", new ChildDefinitionParser("provider", JaasSimpleAuthenticationProvider.class));
        registerBeanDefinitionParser("password-encryption-strategy", new ChildDefinitionParser("encryptionStrategy", PasswordBasedEncryptionStrategy.class));
        registerBeanDefinitionParser("jaas-security-filter", new SecurityFilterDefinitionParser(JaasSecurityFilter.class));
    }

}


