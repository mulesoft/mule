/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.el;

import static org.junit.Assert.assertSame;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguage;

import org.junit.Test;

public class ExpressionLanguageExtensionTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/el/expression-language-extension-config.xml";
  }

  @Test
  public void doesNotOverrideExpressionLanguageInExpressionManagerOnCreation() throws Exception {
    ExpressionLanguage originalExpressionLanguage = muleContext.getExpressionLanguage();

    flowRunner("createsExpressionLanguage").withPayload(TEST_MESSAGE).run();

    ExpressionLanguage newExpressionLanguage = muleContext.getExpressionLanguage();

    assertSame(originalExpressionLanguage, newExpressionLanguage);
  }

  public static class ExpressionLanguageFactory {

    public Object process(Object value) {
      new TestExpressionLanguage(muleContext);

      return value;
    }
  }

  public static class TestExpressionLanguage extends MVELExpressionLanguage {

    public TestExpressionLanguage(MuleContext muleContext) {
      super(muleContext);
    }
  }
}
