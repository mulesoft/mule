/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.dataweave;

import static java.util.Collections.singletonMap;
import static java.util.Optional.of;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.internal.exception.ErrorTypeRepositoryFactory.createDefaultErrorTypeRepository;
import static org.mule.tck.MuleTestUtils.OBJECT_ERROR_TYPE_REPO_REGISTRY_KEY;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.weave.v2.el.WeaveDefaultExpressionLanguageFactoryService;

import java.util.Map;

import org.junit.Before;

public abstract class AbstractWeaveExpressionLanguageTestCase extends AbstractMuleContextTestCase {

  protected DataWeaveExpressionLanguageAdaptor expressionLanguage;

  private DefaultExpressionLanguageFactoryService weaveExpressionExecutor;
  protected Registry registry = mock(Registry.class);

  @Before
  public void setUp() {
    weaveExpressionExecutor = new WeaveDefaultExpressionLanguageFactoryService(null);
    when(registry.lookupByType(DefaultExpressionLanguageFactoryService.class)).thenReturn(of(weaveExpressionExecutor));
    expressionLanguage = new DataWeaveExpressionLanguageAdaptor(muleContext, registry, weaveExpressionExecutor);
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    return singletonMap(OBJECT_ERROR_TYPE_REPO_REGISTRY_KEY, createDefaultErrorTypeRepository());
  }

}
