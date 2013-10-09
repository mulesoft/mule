/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.pgp.config;

import org.mule.api.config.MuleProperties;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.NamedDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.specific.SecurityFilterDefinitionParser;
import org.mule.module.pgp.KeyBasedEncryptionStrategy;
import org.mule.module.pgp.PGPSecurityProvider;
import org.mule.module.pgp.filters.PGPSecurityFilter;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class PgpNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("security-manager", new NamedDefinitionParser(MuleProperties.OBJECT_SECURITY_MANAGER));
        registerBeanDefinitionParser("security-provider", new ChildDefinitionParser("provider", PGPSecurityProvider.class));
        registerBeanDefinitionParser("security-filters", new ParentDefinitionParser());
        registerBeanDefinitionParser("security-filter", new SecurityFilterDefinitionParser(PGPSecurityFilter.class));
        registerBeanDefinitionParser("keybased-encryption-strategy", new ChildDefinitionParser("encryptionStrategy", KeyBasedEncryptionStrategy.class));
    }

}
