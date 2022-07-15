/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

import java.math.BigInteger;
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
public class ParameterBigIntegerTestCase extends AbstractExtensionFunctionalTestCase {

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
  private static String SMALL_NUMBER = "10";
  private static String SMALL_NUMBER_NEGATIVE = "-11";
  private static String BIG_NUMBER = "30414093201713378043612608166064768844377641568960512000000000000";
  private static String BIG_NUMBER_NEGATIVE = "-30414093201713378043612608166064768844377641568960512000000000099";

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

  public ParameterBigIntegerTestCase(String name, String x, String y) {
    this.name = name;
    this.x = x;
    this.y = y;
  }

  @Override
  protected String getConfigFile() {
    return "values/some-extension-big-integers-config.xml";
  }

  @Test
  public void sumBigIntegers() throws Exception {
    CoreEvent result = flowRunner("sumBigInteger").withVariable("x", x).withVariable("y", y).run();
    BigInteger number = new BigInteger(x).add(new BigInteger(y));
    assertThat(result.getMessage().getPayload().getValue(), is(number));
  }

  @Test
  public void sumBigIntegersList() throws Exception {
    CoreEvent result = flowRunner("sumBigIntegerList").withVariable("x", x).withVariable("y", y).run();
    BigInteger number = new BigInteger(x).add(new BigInteger(y));
    assertThat(result.getMessage().getPayload().getValue(), is(number));
  }

  @Test
  public void sumBigIntegersProperties() throws Exception {
    CoreEvent result = flowRunner("BI:" + name).run();
    BigInteger number = new BigInteger(x).add(new BigInteger(y));
    assertThat(result.getMessage().getPayload().getValue(), is(number));
  }

  @Test
  public void sumBigIntegersInline() throws Exception {
    CoreEvent result = flowRunner("sumBigIntegerListInline").withVariable("x", x).withVariable("y", y).run();
    BigInteger number = new BigInteger(x).add(new BigInteger(y));
    assertThat(result.getMessage().getPayload().getValue(), is(number));
  }
}
