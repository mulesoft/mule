/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.parameter.value;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;
import static org.mule.test.allure.AllureConstants.JavaSdk.Parameters.PARAMETERS;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

@Feature(JAVA_SDK)
@Story(PARAMETERS)
@Issue("W-11348869")
@RunnerDelegateTo(Parameterized.class)
public class ParameterBigDecimalTestCase extends AbstractExtensionFunctionalTestCase {

  private String name;
  private String x;
  private String y;

  @Rule
  public SystemProperty zero = new SystemProperty("ZERO", ZERO);

  @Rule
  public SystemProperty small_number = new SystemProperty("SMALL_NUMBER", SMALL_NUMBER);

  @Rule
  public SystemProperty small_number_negative = new SystemProperty("SMALL_NUMBER_NEGATIVE", SMALL_NUMBER_NEGATIVE);

  @Rule
  public SystemProperty big_number = new SystemProperty("BIG_NUMBER", BIG_NUMBER);

  @Rule
  public SystemProperty big_number_negative = new SystemProperty("BIG_NUMBER_NEGATIVE", BIG_NUMBER_NEGATIVE);

  private static String ZERO = "0";
  private static String SMALL_NUMBER = "10.1";
  private static String SMALL_NUMBER_NEGATIVE = "-11.2";
  private static String BIG_NUMBER = "3041.4093201713378043612608166064768844377641568960512000000000000";
  private static String BIG_NUMBER_NEGATIVE = "-30414.093201713378043612608166064768844377641568960512000000000099";

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    List<Object[]> combinations = new ArrayList<>();
    String[] names = {"ZERO", "SMALL_NUMBER", "SMALL_NUMBER_NEGATIVE", "BIG_NUMBER", "BIG_NUMBER_NEGATIVE"};
    String[] numbers = {ZERO, SMALL_NUMBER, SMALL_NUMBER_NEGATIVE, BIG_NUMBER, BIG_NUMBER_NEGATIVE};
    for (int i = 0; i < numbers.length; i++) {
      for (int j = i; j < numbers.length; j++) {
        combinations.add(new String[] {names[i] + "+" + names[j], numbers[i], numbers[j]});
      }
    }
    return combinations;
  }

  public ParameterBigDecimalTestCase(String name, String x, String y) {
    this.name = name;
    this.x = x;
    this.y = y;
  }

  @Override
  protected String getConfigFile() {
    return "values/some-extension-big-decimals-config.xml";
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

  @Test
  public void sumBigDecimalsProperties() throws Exception {
    CoreEvent result = flowRunner("BD:" + name).run();
    BigDecimal number = new BigDecimal(x).add(new BigDecimal(y));
    assertThat(result.getMessage().getPayload().getValue(), is(number));
  }

  @Test
  public void sumBigDecimalsInline() throws Exception {
    CoreEvent result = flowRunner("sumBigDecimalListInline").withVariable("x", x).withVariable("y", y).run();
    BigDecimal number = new BigDecimal(x).add(new BigDecimal(y));
    assertThat(result.getMessage().getPayload().getValue(), is(number));
  }
}
