/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.dataweave;

import static java.util.Optional.of;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.weave.v2.el.WeaveDefaultExpressionLanguageFactoryService;

import org.junit.Before;

public abstract class AbstractWeaveExpressionLanguageTestCase extends AbstractMuleContextTestCase {

  protected DataWeaveExpressionLanguageAdaptor expressionLanguage;

  private DefaultExpressionLanguageFactoryService weaveExpressionExecutor;
  protected Registry registry = mock(Registry.class);

  @Before
  public void setUp() {
    weaveExpressionExecutor = new WeaveDefaultExpressionLanguageFactoryService(null);
    when(registry.lookupByType(DefaultExpressionLanguageFactoryService.class)).thenReturn(of(weaveExpressionExecutor));
    expressionLanguage =
        new DataWeaveExpressionLanguageAdaptor(muleContext, registry, weaveExpressionExecutor, getFeatureFlaggingService());
  }

}
