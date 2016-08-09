/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.spring.security.config;

import org.mule.runtime.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.SecurityFilterDefinitionParser;
import org.mule.runtime.module.spring.security.AuthorizationFilter;
import org.mule.runtime.module.spring.security.SpringProviderAdapter;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Registers a Bean Definition Parser for handling Spring Security related elements.
 */
public class SpringSecurityNamespaceHandler extends NamespaceHandlerSupport {

  public void init() {
    registerBeanDefinitionParser("security-manager", new SecurityManagerDefinitionParser());
    registerBeanDefinitionParser("delegate-security-provider",
                                 new ChildDefinitionParser("provider", SpringProviderAdapter.class));
    registerBeanDefinitionParser("authorization-filter", new SecurityFilterDefinitionParser(AuthorizationFilter.class));
    registerBeanDefinitionParser("security-property", new ChildMapEntryDefinitionParser("securityProperty", "name", "value"));
  }

}
