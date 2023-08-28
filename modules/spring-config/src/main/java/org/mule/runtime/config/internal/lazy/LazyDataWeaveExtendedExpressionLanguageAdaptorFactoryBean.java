/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.lazy;

import org.mule.runtime.config.internal.el.DataWeaveExtendedExpressionLanguageAdaptorFactoryBean;
import org.mule.runtime.core.internal.el.ExtendedExpressionLanguageAdaptor;

public class LazyDataWeaveExtendedExpressionLanguageAdaptorFactoryBean
    extends DataWeaveExtendedExpressionLanguageAdaptorFactoryBean {

  @Override
  public ExtendedExpressionLanguageAdaptor getObject() throws Exception {
    return new LazyExpressionLanguageAdaptor(() -> super.getObject());
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
