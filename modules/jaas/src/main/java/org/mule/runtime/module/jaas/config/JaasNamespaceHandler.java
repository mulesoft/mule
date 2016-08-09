/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.jaas.config;

import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.NamedDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.SecurityFilterDefinitionParser;
import org.mule.runtime.module.jaas.JaasSimpleAuthenticationProvider;
import org.mule.runtime.module.jaas.filters.JaasSecurityFilter;
import org.mule.runtime.core.security.PasswordBasedEncryptionStrategy;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class JaasNamespaceHandler extends NamespaceHandlerSupport {

  public void init() {
    registerBeanDefinitionParser("security-manager", new NamedDefinitionParser(MuleProperties.OBJECT_SECURITY_MANAGER));
    registerBeanDefinitionParser("security-provider",
                                 new ChildDefinitionParser("provider", JaasSimpleAuthenticationProvider.class));
    registerBeanDefinitionParser("password-encryption-strategy",
                                 new ChildDefinitionParser("encryptionStrategy", PasswordBasedEncryptionStrategy.class));
    registerBeanDefinitionParser("jaas-security-filter", new SecurityFilterDefinitionParser(JaasSecurityFilter.class));
  }

}


