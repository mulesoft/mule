/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.el;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.el.ExtendedExpressionLanguageAdaptor;
import org.mule.runtime.core.internal.el.dataweave.DataWeaveExpressionLanguageAdaptor;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

public class DataWeaveExtendedExpressionLanguageAdaptorFactoryBean implements FactoryBean<ExtendedExpressionLanguageAdaptor>,
    DisposableBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataWeaveExtendedExpressionLanguageAdaptorFactoryBean.class);

  @Inject
  private DefaultExpressionLanguageFactoryService dwExpressionLanguageFactory;

  @Inject
  private FeatureFlaggingService featureFlaggingService;

  @Inject
  private MuleContext muleContext;

  @Inject
  private Registry registry;

  private ExtendedExpressionLanguageAdaptor instance;

  @Override
  public ExtendedExpressionLanguageAdaptor getObject() throws Exception {
    // This is a singleton scoped bean. So it doesn't need to be synchronized.
    if (instance == null) {
      instance = getExtendedExpressionLanguageAdaptor();
    }
    return instance;
  }

  @Override
  public Class<?> getObjectType() {
    return ExtendedExpressionLanguageAdaptor.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  @Override
  public void destroy() throws Exception {
    disposeIfNeeded(instance, LOGGER);
  }

  /**
   * @return the {@link org.mule.runtime.core.internal.el.ExpressionLanguageAdaptor} that will be returned as a singleton bean.
   *
   * @since 4.5.0
   */
  protected ExtendedExpressionLanguageAdaptor getExtendedExpressionLanguageAdaptor() {
    return new DataWeaveExpressionLanguageAdaptor(muleContext, registry, dwExpressionLanguageFactory, featureFlaggingService);
  }
}
