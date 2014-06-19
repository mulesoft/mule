/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
import org.mule.util.SecurityUtils;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

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
