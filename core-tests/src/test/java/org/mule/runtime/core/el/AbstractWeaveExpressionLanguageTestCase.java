/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import org.mule.weave.v2.el.WeaveDefaultExpressionLanguageFactoryService;
import org.junit.Before;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.config.builders.DefaultsConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleContextTestCase;


public abstract class AbstractWeaveExpressionLanguageTestCase extends AbstractMuleContextTestCase {

  protected DataWeaveExpressionLanguageAdaptor expressionLanguage;

  @Before
  public void setUp() {
    expressionLanguage = new DataWeaveExpressionLanguageAdaptor(muleContext);
  }

  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    return new DefaultsConfigurationBuilder() {

      @Override
      public void configure(MuleContext muleContext) throws ConfigurationException {
        try {
          DefaultExpressionLanguageFactoryService weaveExpressionExecutor = new WeaveDefaultExpressionLanguageFactoryService();
          muleContext.getRegistry().registerObject(weaveExpressionExecutor.getName(), weaveExpressionExecutor);
        } catch (RegistrationException e) {
          throw new ConfigurationException(e);
        }
        super.configure(muleContext);
      }
    };
  }
}
