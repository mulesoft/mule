/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.el;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.el.ExtendedExpressionLanguageAdaptor;
import org.mule.runtime.core.internal.el.dataweave.DataWeaveExpressionLanguageAdaptor;

import javax.inject.Inject;

import org.springframework.beans.factory.FactoryBean;

public class DataWeaveExtendedExpressionLanguageAdaptorFactoryBean implements FactoryBean<ExtendedExpressionLanguageAdaptor> {

  @Inject
  private DefaultExpressionLanguageFactoryService dwExpressionLanguageFactory;

  @Inject
  private FeatureFlaggingService featureFlaggingService;

  @Inject
  private MuleContext muleContext;

  @Inject
  private Registry registry;

  @Override
  public ExtendedExpressionLanguageAdaptor getObject() throws Exception {
    return new DataWeaveExpressionLanguageAdaptor(muleContext, registry, dwExpressionLanguageFactory, featureFlaggingService);
  }

  @Override
  public Class<?> getObjectType() {
    return ExtendedExpressionLanguageAdaptor.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
