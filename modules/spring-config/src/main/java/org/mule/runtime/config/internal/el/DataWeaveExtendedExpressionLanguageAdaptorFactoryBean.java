/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.el;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.el.ExtendedExpressionLanguageAdaptor;
import org.mule.runtime.core.internal.el.dataweave.DataWeaveExpressionLanguageAdaptor;

import jakarta.inject.Inject;

import org.springframework.beans.factory.FactoryBean;

public class DataWeaveExtendedExpressionLanguageAdaptorFactoryBean implements FactoryBean<ExtendedExpressionLanguageAdaptor> {

  @Inject
  private DefaultExpressionLanguageFactoryService dwExpressionLanguageFactory;

  @Inject
  private FeatureFlaggingService featureFlaggingService;

  @Inject
  private MuleContext muleContext;

  @Inject
  private ArtifactEncoding artifactEncoding;

  @Inject
  private Registry registry;

  @Override
  public ExtendedExpressionLanguageAdaptor getObject() throws Exception {
    return new DataWeaveExpressionLanguageAdaptor(muleContext, registry,
                                                  muleContext.getConfiguration(), artifactEncoding,
                                                  dwExpressionLanguageFactory, featureFlaggingService);
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
