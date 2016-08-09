/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tls.internal.config;

import org.mule.runtime.config.spring.handlers.AbstractMuleNamespaceHandler;

/**
 * Reigsters a Bean Definition Parser for handling <code><tls:connector></code> elements.
 */
public class TlsNamespaceHandler extends AbstractMuleNamespaceHandler {

  public void init() {
    registerBeanDefinitionParser("key-store", new KeyStoreParentContextDefinitionParser());
    registerBeanDefinitionParser("context", new TlsContextDefinitionParser());
    registerBeanDefinitionParser("trust-store", new TrustStoreTlsContextDefinitionParser());
  }

}
