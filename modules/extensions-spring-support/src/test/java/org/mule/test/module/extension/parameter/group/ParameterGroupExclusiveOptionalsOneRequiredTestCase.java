/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.parameter.group;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Test;

public class ParameterGroupExclusiveOptionalsOneRequiredTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "values/some-parameter-group-config.xml";
  }

  @Test
  public void whitespacesAreTrimmedForParameterValue() throws Exception {
    Object value = flowRunner("whitespaceValueForParameter").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello Max Mule!"));
  }

  @Test
  public void whitespacesAreTrimmedForParameterValueExpression() throws Exception {
    Object value = flowRunner("whitespaceValueForParameterExpression").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }

  @Test
  public void whitespacesAreTrimmedForContentParameterValue() throws Exception {
    Object value = flowRunner("whitespaceValueForContentParameter").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello Max Mule!"));
  }

  @Test
  public void whitespacesAreTrimmedForContentParameterValueExpression() throws Exception {
    Object value = flowRunner("whitespaceValueForContentParameterExpression").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }

  @Test
  public void whitespacesAreTrimmedForContentParameterValueCDATA() throws Exception {
    Object value = flowRunner("whitespaceValueForContentParameterCDATA").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }
}
