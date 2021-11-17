/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.parameter.value;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_PARAMETER_WHITESPACE_TRIMMING_PROPERTY;

import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class ParameterWhitespaceTrimmingTestCase extends AbstractExtensionFunctionalTestCase {

  private final boolean trimWhitespaces;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"whitespace trimming disabled", false},
        {"whitespace trimming enabled", true}
    });
  }

  public ParameterWhitespaceTrimmingTestCase(String name, boolean trimWhitespaces) {
    this.trimWhitespaces = trimWhitespaces;
    if (trimWhitespaces) {
      System.setProperty(ENABLE_PARAMETER_WHITESPACE_TRIMMING_PROPERTY, "true");
    }
  }

  @Override
  protected String getConfigFile() {
    return "values/some-parameter-group-config.xml";
  }

  @Test
  public void whitespacesForParameterValue() throws Exception {
    Object value = flowRunner("value").run().getMessage().getPayload().getValue();
    if (this.trimWhitespaces) {
      assertThat(value, is("Hello Max Mule!"));
    } else {
      assertThat(value, is("Hello    Max Mule   !"));
    }
  }

  @Test
  public void whitespacesAreNotTrimmedForParameterValueExpression() throws Exception {
    Object value = flowRunner("expression").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }

  @Test
  public void whitespacesAreTrimmedForContentParameterValue() throws Exception {
    Object value = flowRunner("content").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello Max Mule!"));
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
  public void whitespacesAreTrimmedForTextParameterValue() throws Exception {
    Object value = flowRunner("text").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello Max Mule!"));
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
