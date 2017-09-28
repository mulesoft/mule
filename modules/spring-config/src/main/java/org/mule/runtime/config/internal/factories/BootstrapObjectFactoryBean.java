/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import org.mule.runtime.core.internal.config.bootstrap.BootstrapObjectFactory;

import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean which returns an object created through a {@link BootstrapObjectFactory}}
 *
 * @since 3.7.0
 */
public class BootstrapObjectFactoryBean implements FactoryBean<Object> {

  private final Object object;

  public BootstrapObjectFactoryBean(BootstrapObjectFactory factory) {
    object = factory.create();
  }

  @Override
  public Object getObject() throws Exception {
    return object;
  }

  @Override
  public Class<?> getObjectType() {
    return object.getClass();
  }

  @Override
  public boolean isSingleton() {
    return true;
  }
}
