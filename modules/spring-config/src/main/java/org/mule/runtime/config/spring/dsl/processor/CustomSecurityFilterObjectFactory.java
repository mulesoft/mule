/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.processor;

import org.mule.runtime.core.api.security.SecurityFilter;
import org.mule.runtime.core.util.BeanUtils;
import org.mule.runtime.core.util.ClassUtils;

import java.util.Map;

/**
 * Object factory for custom {@link SecurityFilter}.
 *
 * @since 4.0
 */
public class CustomSecurityFilterObjectFactory extends AbstractSecurityFilterObjectFactory<SecurityFilter> {

  private final Class<? extends SecurityFilter> clazz;
  private final Map properties;

  public CustomSecurityFilterObjectFactory(Class<? extends SecurityFilter> clazz, Map properties) {
    this.clazz = clazz;
    this.properties = properties;
  }

  @Override
  public SecurityFilter getFilter() {
    try {
      SecurityFilter securityFilter = ClassUtils.instanciateClass(clazz);
      if (properties != null) {
        BeanUtils.populateWithoutFail(securityFilter, properties, false);
      }
      return securityFilter;
    } catch (Exception e) {
      throw new RuntimeException();
    }
  }

}
