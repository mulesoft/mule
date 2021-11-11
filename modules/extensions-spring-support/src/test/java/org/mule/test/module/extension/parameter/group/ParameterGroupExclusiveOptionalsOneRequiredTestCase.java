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
  public void whitespacesAreNotTrimmedForParameterValue() throws Exception {
    Object value = flowRunner("value").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }

  @Test
  public void whitespacesAreNotTrimmedForParameterValueExpression() throws Exception {
    Object value = flowRunner("expression").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }

  @Test
  public void whitespacesAreNotTrimmedForContentParameterValue() throws Exception {
    Object value = flowRunner("content").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello \n                Max Mule\n            !"));
  }

  @Test
  public void whitespacesAreNotTrimmedForContentParameterValueExpression() throws Exception {
    Object value = flowRunner("contentExpression").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }

  @Test
  public void whitespacesAreNotTrimmedForContentParameterValueCDATA() throws Exception {
    Object value = flowRunner("contentCDATA").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }

  @Test
  public void whitespacesAreNotTrimmedFortextParameterValue() throws Exception {
    Object value = flowRunner("text").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello \n                Max Mule\n            !"));
  }

  @Test
  public void whitespacesAreNotTrimmedForTextParameterValueExpression() throws Exception {
    Object value = flowRunner("textExpression").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }

  @Test
  public void whitespacesAreNotTrimmedForTextParameterValueCDATA() throws Exception {
    Object value = flowRunner("textCDATA").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }
}
