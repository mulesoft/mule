/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.parameter.value;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;
import static org.mule.test.allure.AllureConstants.JavaSdk.Parameters.PARAMETERS;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.math.BigDecimal;
import java.util.Collection;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;
import org.junit.runners.Parameterized;

@Feature(JAVA_SDK)
@Story(PARAMETERS)
@Issue("W-11348869")
@RunnerDelegateTo(Parameterized.class)
public class ParameterBigDecimalTestCase extends AbstractExtensionFunctionalTestCase {

  private String x;
  private String y;

  private static String ZERO = "0";
  private static String SMALL_NUMBER = "10.1";
  private static String SMALL_NUMBER_NEGATIVE = "-11.2";
  private static String BIG_NUMBER = "3041.4093201713378043612608166064768844377641568960512000000000000";
  private static String BIG_NUMBER_NEGATIVE = "-30414.093201713378043612608166064768844377641568960512000000000099";

  @Parameterized.Parameters()
  public static Collection<Object[]> data() {
    return asList(new Object[][] {{ZERO, ZERO}, {ZERO, SMALL_NUMBER}, {ZERO, SMALL_NUMBER_NEGATIVE},
        {SMALL_NUMBER, SMALL_NUMBER_NEGATIVE}, {SMALL_NUMBER, SMALL_NUMBER}, {SMALL_NUMBER_NEGATIVE, SMALL_NUMBER_NEGATIVE},
        {BIG_NUMBER, BIG_NUMBER_NEGATIVE}, {BIG_NUMBER, BIG_NUMBER}, {BIG_NUMBER_NEGATIVE, BIG_NUMBER_NEGATIVE}});
  }

  public ParameterBigDecimalTestCase(String x, String y) {
    this.x = x;
    this.y = y;
  }

  @Override
  protected String getConfigFile() {
    return "values/some-extension-big-numbers-config.xml";
  }

  @Test
  public void sumBigDecimals() throws Exception {
    CoreEvent result = flowRunner("sumBigDecimal").withVariable("x", x).withVariable("y", y).run();
    BigDecimal number = new BigDecimal(x).add(new BigDecimal(y));
    assertThat(result.getMessage().getPayload().getValue(), is(number));
  }

  @Test
  public void sumBigDecimalsList() throws Exception {
    CoreEvent result = flowRunner("sumBigDecimalList").withVariable("x", x).withVariable("y", y).run();
    BigDecimal number = new BigDecimal(x).add(new BigDecimal(y));
    assertThat(result.getMessage().getPayload().getValue(), is(number));
  }
}
