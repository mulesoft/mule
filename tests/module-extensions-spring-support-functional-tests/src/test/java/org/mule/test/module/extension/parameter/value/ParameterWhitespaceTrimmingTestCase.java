/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.parameter.value;

import static org.mule.runtime.api.util.MuleSystemProperties.DISABLE_ATTRIBUTE_PARAMETER_WHITESPACE_TRIMMING_PROPERTY;
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
public class ParameterWhitespaceTrimmingTestCase extends AbstractExtensionFunctionalTestCase {

  private final boolean disableWhitespaceTrimming;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"disable whitespace trimming", true},
        {"enable whitespace trimming (default behaviour)", false}
    });
  }

  @Rule
  public SystemProperty disableWhitespaceTrimmingRule;

  public ParameterWhitespaceTrimmingTestCase(String name, boolean disableWhitespaceTrimming) {
    this.disableWhitespaceTrimming = disableWhitespaceTrimming;

    this.disableWhitespaceTrimmingRule =
        new SystemProperty(DISABLE_ATTRIBUTE_PARAMETER_WHITESPACE_TRIMMING_PROPERTY, "" + disableWhitespaceTrimming);
  }

  @Override
  protected String getConfigFile() {
    return "values/some-parameter-group-config.xml";
  }

  @Test
  public void whitespacesForSimpleParameter() throws Exception {
    Object value = flowRunner("value").run().getMessage().getPayload().getValue();
    if (this.disableWhitespaceTrimming) {
      assertThat(value, is("Hello    Max Mule   !"));
    } else {
      assertThat(value, is("Hello Max Mule!"));
    }
  }

  @Test
  public void whitespacesAreNotTrimmedForSimpleParameterExpression() throws Exception {
    Object value = flowRunner("expression").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }

  @Test
  public void whitespacesAreTrimmedForContent() throws Exception {
    Object value = flowRunner("content").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello Max Mule!"));
  }

  @Test
  public void whitespacesAreNotTrimmedForContentExpression() throws Exception {
    Object value = flowRunner("contentExpression").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }

  @Test
  public void whitespacesAreNotTrimmedForContentCDATA() throws Exception {
    Object value = flowRunner("contentCDATA").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }

  @Test
  public void whitespacesAreTrimmedForText() throws Exception {
    Object value = flowRunner("text").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello Max Mule!"));
  }

  @Test
  public void whitespacesAreNotTrimmedForTextExpression() throws Exception {
    Object value = flowRunner("textExpression").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }

  @Test
  public void whitespacesAreNotTrimmedForTextCDATA() throws Exception {
    Object value = flowRunner("textCDATA").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }

  @Test
  public void whitespacesForPojo() throws Exception {
    Object value = flowRunner("pojo").run().getMessage().getPayload().getValue();
    if (this.disableWhitespaceTrimming) {
      assertThat(value, is("Hello    Max Mule   !"));
    } else {
      assertThat(value, is("Hello Max Mule!"));
    }
  }

  @Test
  public void whitespacesAreNotTrimmedForPojoExpression() throws Exception {
    Object value = flowRunner("pojoExpression").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }

  @Test
  public void whitespacesAreTrimmedForPojoText() throws Exception {
    Object value = flowRunner("pojoText").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello Max Mule!"));
  }

  @Test
  public void whitespacesAreNotTrimmedForPojoTextExpression() throws Exception {
    Object value = flowRunner("pojoTextExpression").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }

  @Test
  public void whitespacesForPg() throws Exception {
    Object value = flowRunner("pg").run().getMessage().getPayload().getValue();
    if (this.disableWhitespaceTrimming) {
      assertThat(value, is("Hello    Max Mule   !"));
    } else {
      assertThat(value, is("Hello Max Mule!"));
    }
  }

  @Test
  public void whitespacesAreNotTrimmedForPgExpression() throws Exception {
    Object value = flowRunner("pgExpression").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }

  @Test
  public void whitespacesAreTrimmedForPgText() throws Exception {
    Object value = flowRunner("pgText").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello Max Mule!"));
  }

  @Test
  public void whitespacesAreNotTrimmedForPgTextExpression() throws Exception {
    Object value = flowRunner("pgTextExpression").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }

  @Test
  public void whitespacesAreNotTrimmedForPgTextCDATA() throws Exception {
    Object value = flowRunner("pgTextCDATA").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }

  @Test
  public void whitespacesForPgDsl() throws Exception {
    Object value = flowRunner("pgDsl").run().getMessage().getPayload().getValue();
    if (this.disableWhitespaceTrimming) {
      assertThat(value, is("Hello    Max Mule   !"));
    } else {
      assertThat(value, is("Hello Max Mule!"));
    }
  }

  @Test
  public void whitespacesAreNotTrimmedForPgDSLExpression() throws Exception {
    Object value = flowRunner("pgDslExpression").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }

  @Test
  public void whitespacesAreTrimmedForPgDslText() throws Exception {
    Object value = flowRunner("pgDslText").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello Max Mule!"));
  }

  @Test
  public void whitespacesAreNotTrimmedForPgDslTextExpression() throws Exception {
    Object value = flowRunner("pgDslTextExpression").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }

  @Test
  public void whitespacesAreNotTrimmedForPgDslTextCDATA() throws Exception {
    Object value = flowRunner("pgDslTextCDATA").run().getMessage().getPayload().getValue();
    assertThat(value, is("Hello    Max Mule   !"));
  }
}
