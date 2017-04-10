/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.el;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;

import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.el.ExpressionLanguage;

public class MockExpressionLanguageFactoryService implements DefaultExpressionLanguageFactoryService {

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public ExpressionLanguage create() {
    return new MockExpressionLanguage();
  }
}
