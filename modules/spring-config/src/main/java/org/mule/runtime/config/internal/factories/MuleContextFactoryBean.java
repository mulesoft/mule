/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import org.mule.runtime.core.api.MuleContext;

import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} which returns a fixed {@link MuleContext}
 *
 * @since 4.2.0
 */
public class MuleContextFactoryBean implements FactoryBean<MuleContext> {

  private final MuleContext muleContext;

  /**
   * Creates a new instance
   *
   * @param muleContext the context ot be returned
   */
  public MuleContextFactoryBean(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  @Override
  public MuleContext getObject() throws Exception {
    return muleContext;
  }

  @Override
  public Class<?> getObjectType() {
    return MuleContext.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }
}
