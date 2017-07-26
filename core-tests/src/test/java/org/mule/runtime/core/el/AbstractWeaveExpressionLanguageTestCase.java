/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_REGISTRY;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.builders.DefaultsConfigurationBuilder;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.weave.v2.el.WeaveDefaultExpressionLanguageFactoryService;

import org.junit.Before;


public abstract class AbstractWeaveExpressionLanguageTestCase extends AbstractMuleContextTestCase {

  protected DataWeaveExpressionLanguageAdaptor expressionLanguage;

  @Before
  public void setUp() {
    expressionLanguage = DataWeaveExpressionLanguageAdaptor.create(muleContext);
  }

  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    return new DefaultsConfigurationBuilder() {

      @Override
      public void configure(MuleContext muleContext) throws ConfigurationException {
        try {
          DefaultExpressionLanguageFactoryService weaveExpressionExecutor = new WeaveDefaultExpressionLanguageFactoryService();
          muleContext.getRegistry().registerObject(OBJECT_REGISTRY, new DefaultRegistry(muleContext));
          muleContext.getRegistry().registerObject(weaveExpressionExecutor.getName(), weaveExpressionExecutor);
        } catch (RegistrationException e) {
          throw new ConfigurationException(e);
        }
        super.configure(muleContext);
      }
    };
  }
}
