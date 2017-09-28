/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToCreate;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.transaction.TransactionManagerFactory;

import javax.transaction.TransactionManager;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@link FactoryBean} adapter for the configured {@link TransactionManagerFactory}.
 * <p/>
 * Creates a reference to the TransactionManager configured on the MuleContext. This is useful when you need to inject the
 * TransactionManager into other objects such as XA data Sources.
 * <p/>
 * This will first look for a single {@link TransactionManagerFactory} and use it to build the {@link TransactionManager}.
 * <p/>
 * If no {@link TransactionManagerFactory} is found, then it will look for {@link TransactionManager} instances.
 *
 * @since 3.7.4
 */
public class TransactionManagerFactoryBean implements FactoryBean<TransactionManager>, MuleContextAware {

  @Autowired(required = false)
  private TransactionManagerFactory txManagerFactory;

  private MuleContext muleContext;

  @Override
  public TransactionManager getObject() throws Exception {
    if (muleContext.isDisposing()) {
      // The txManager might be declared, but if it isn't used by the application it won't be created until the
      // muleContext is disposed.
      // At that point, there is no need to create the txManager.
      return null;
    } else if (txManagerFactory != null) {
      try {
        return txManagerFactory.create(muleContext.getConfiguration());
      } catch (Exception e) {
        throw new MuleRuntimeException(failedToCreate("transaction manager"), e);
      }
    }
    return null;
  }

  @Override
  public Class<?> getObjectType() {
    return TransactionManager.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  public void setTxManagerFactory(TransactionManagerFactory txManagerFactory) {
    this.txManagerFactory = txManagerFactory;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}
