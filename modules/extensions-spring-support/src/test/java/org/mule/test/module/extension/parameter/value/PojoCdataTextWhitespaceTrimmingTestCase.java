/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.parameter.value;

import static org.mule.runtime.api.util.MuleSystemProperties.DISABLE_POJO_TEXT_CDATA_WHITESPACE_TRIMMING_PROPERTY;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;
import static org.mule.test.allure.AllureConstants.JavaSdk.Parameters.PARAMETERS;

import static java.util.Arrays.asList;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(JAVA_SDK)
@Story(PARAMETERS)
@Issue("MULE-19803")
@RunnerDelegateTo(Parameterized.class)
public class PojoCdataTextWhitespaceTrimmingTestCase extends AbstractExtensionFunctionalTestCase {

  private final boolean disableWhitespaceTrimming;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"disable whitespace trimming (expected behavior)", true},
        {"enable whitespace trimming (previous behaviour)", false}
    });
  }

  @Rule
  public SystemProperty disableWhitespaceTrimmingRule;

  public PojoCdataTextWhitespaceTrimmingTestCase(String name, boolean disableWhitespaceTrimming) {
    this.disableWhitespaceTrimming = disableWhitespaceTrimming;

    this.disableWhitespaceTrimmingRule =
        new SystemProperty(DISABLE_POJO_TEXT_CDATA_WHITESPACE_TRIMMING_PROPERTY, "" + disableWhitespaceTrimming);
  }

  @Override
  protected String getConfigFile() {
    return "values/some-parameter-group-config.xml";
  }

  @Test
  @Issue("MULE-20048")
  public void whitespacesForPojoTextCDATA() throws Exception {
    Object value = flowRunner("pojoTextCDATA").run().getMessage().getPayload().getValue();
    if (disableWhitespaceTrimming) {
      assertThat(value, is("Hello    Max Mule   !"));
    } else {
      assertThat(value, is("Hello Max Mule!"));
    }
  }

}
