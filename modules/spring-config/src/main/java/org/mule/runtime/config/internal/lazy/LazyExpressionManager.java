/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.lazy;

import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.core.internal.el.DefaultExpressionManager;
import org.mule.runtime.core.internal.el.ExtendedExpressionLanguageAdaptor;

/**
 * Expression manager that does not initialize the underlying expression language support until a first usage is done.
 *
 * @since 4.4
 */
public class LazyExpressionManager extends DefaultExpressionManager {

  public static final String NON_LAZY_EXPRESSION_MANAGER = "_muleNonLazyExpressionManager";

  @Override
  protected ExtendedExpressionLanguageAdaptor createExpressionLanguageAdaptor(DefaultExpressionLanguageFactoryService service) {
    return new LazyExpressionLanguageAdaptor(() -> createWeaveExpressionLanguageAdaptor(service));
  }

}
