/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.service.el;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;

import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.ExpressionLanguageConfiguration;

public class MockExpressionLanguageFactoryService implements DefaultExpressionLanguageFactoryService {

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public ExpressionLanguage create() {
    return new MockExpressionLanguage();
  }

  @Override
  public ExpressionLanguage create(ExpressionLanguageConfiguration configuration) {
    return create();
  }
}
