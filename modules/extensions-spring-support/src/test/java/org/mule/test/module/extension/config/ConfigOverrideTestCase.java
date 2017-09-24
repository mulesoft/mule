/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.vegan.extension.AppleConfig;
import org.mule.test.vegan.extension.HealthyFood;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ConfigOverrideTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "vegan-config-overrides.xml";
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @Test
  public void simpleParameterOverrideWithConfigDefaults() throws Exception {
    TypedValue<Integer> result = flowRunner("timeToPeelOverrideOperationWithDefaults")
        .run().getMessage().getPayload();
    assertThat(result.getValue(), is(10));
  }

  @Test
  public void simpleParameterOverrideWithConfigCustomValues() throws Exception {
    TypedValue<Integer> result = flowRunner("timeToPeelOverrideOperationWithCustom")
        .run().getMessage().getPayload();
    assertThat(result.getValue(), is(60));
  }

  @Test
  public void simpleParameterIsNotOverriddenIfDeclared() throws Exception {
    TypedValue<Integer> result = flowRunner("timeToPeelDeclareValueInOperationWithCustom")
        .run().getMessage().getPayload();
    assertThat(result.getValue(), is(30));
  }

  @Test
  public void instanceUsedForOverridesIsTheReferencedOne() throws Exception {
    TypedValue<Integer> result = flowRunner("timeToPeelOverrideOperationWithBananaCustom")
        .run().getMessage().getPayload();
    assertThat(result.getValue(), is(20));
  }

  @Test
  public void dynamicValuesGetResolved() throws Exception {
    final String flowName = "timeToPeelOverrideOperationWithBananaDynamic";
    TypedValue<Integer> result = flowRunner(flowName)
        .withPayload("45")
        .withVariable("declaredVar", null)
        .run().getMessage().getPayload();
    assertThat(result.getValue(), is(45));

    result = flowRunner(flowName)
        .withPayload("45")
        .withVariable("declaredVar", "25")
        .run().getMessage().getPayload();
    assertThat(result.getValue(), is(25));

    result = flowRunner(flowName)
        .withPayload(null)
        .withVariable("declaredVar", "25")
        .run().getMessage().getPayload();
    assertThat(result.getValue(), is(25));

    result = flowRunner(flowName)
        .withPayload(null)
        .withVariable("declaredVar", null)
        .run().getMessage().getPayload();
    assertThat(result.getValue(), is(nullValue()));
  }

  @Test
  public void listParameterOverrideWithNullsafeFromConfigDefaults() throws Exception {
    TypedValue<List<String>> result = flowRunner("mainProducersOverrideOperationWithDefaults")
        .run().getMessage().getPayload();
    assertThat(result.getValue(), hasSize(0));
  }

  @Test
  public void listParameterOverrideWithConfigCustomValues() throws Exception {
    TypedValue<List<String>> result = flowRunner("mainProducersOverrideOperationWithCustom")
        .run().getMessage().getPayload();
    assertThat(result.getValue(), hasSize(2));
    assertThat(result.getValue().get(0), equalTo("appleProducerOne"));
  }

  @Test
  public void listParameterIsNotOverriddenIfDeclared() throws Exception {
    TypedValue<List<String>> result = flowRunner("mainProducersDeclareValueInOperationWithCustom")
        .run().getMessage().getPayload();
    assertThat(result.getValue(), hasSize(1));
    assertThat(result.getValue().get(0), equalTo("getProducersOne"));
  }

  @Test
  public void pojoParameterOverrideFromConfigDefaults() throws Exception {
    TypedValue<HealthyFood> result = flowRunner("sampleOverrideOperationWithDefaults")
        .run().getMessage().getPayload();
    assertThat(result.getValue(), is(nullValue()));
  }

  public void pojoParameterOverrideFromNullsafeConfigDefaults() throws Exception {
    TypedValue<HealthyFood> result = flowRunner("sampleOverrideOperationWithBananaDefaults")
        .run().getMessage().getPayload();
    assertThat(result.getValue(), is(notNullValue()));
    assertThat(result.getValue().getTasteProfile(), is(notNullValue()));
  }

  @Test
  public void pojoParameterOverrideWithConfigCustomValues() throws Exception {
    TypedValue<HealthyFood> result = flowRunner("sampleOverrideOperationWithCustom")
        .run().getMessage().getPayload();
    assertThat(result.getValue(), is(notNullValue()));
    assertThat(result.getValue().getTasteProfile(), is(notNullValue()));
    assertThat(result.getValue().getTasteProfile().isTasty(), is(true));
  }

  @Test
  public void pojoParameterIsNotOverriddenIfDeclared() throws Exception {
    TypedValue<HealthyFood> result = flowRunner("sampleDeclareValueInOperationWithCustom")
        .run().getMessage().getPayload();
    assertThat(result.getValue(), is(notNullValue()));
    assertThat(result.getValue().getTasteProfile(), is(notNullValue()));
    assertThat(result.getValue().getTasteProfile().isTasty(), is(false));
  }

  @Test
  public void parameterIsNotOverriddenWithDefaults() throws Exception {
    TypedValue<String> result = flowRunner("shouldNotOverrideOverrideOperationWithDefaults")
        .run().getMessage().getPayload();
    assertThat(result.getValue(), is(nullValue()));
  }

  @Test
  public void parameterIsNotOverriddenWithCustomValue() throws Exception {
    TypedValue<String> result = flowRunner("shouldNotOverrideOverrideOperationWithCustom")
        .run().getMessage().getPayload();
    assertThat(result.getValue(), is(nullValue()));
  }

  @Test
  public void parameterIsNotOverriddenWhenDeclaredInline() throws Exception {
    TypedValue<String> result = flowRunner("shouldNotOverrideDeclareValueInOperationWithCustom")
        .run().getMessage().getPayload();
    assertThat(result.getValue(), is("declared"));
  }

  @Test
  public void sourceMainAndCallbacksUseOverrideFromConfigDefaults() throws Exception {
    AppleConfig apple = locateConfig("apple");
    pollingProbe(apple, "sourceOverrideWithDefaults", 0, null, null, 10);
  }

  @Test
  public void sourceMainAndCallbacksUseOverrideFromConfigCustom() throws Exception {
    AppleConfig apple = locateConfig("appleCustom");
    pollingProbe(apple, "sourceOverrideWithCustom", 2, true, null, 60);
  }

  @Test
  public void sourceMainAndCallbacksUseDeclaredParameters() throws Exception {
    AppleConfig apple = locateConfig("appleCustom");
    pollingProbe(apple, "sourceDeclareValueInlineWithCustom", 1, false, "declared", 30);
  }

  private boolean assertSourceResults(Map<String, List<Object>> results, String flowName,
                                      int mainProducersSize, Boolean tasty, String shouldNotOverride, int timeToPeel) {

    List<Object> objects = results.get(flowName);
    if (objects == null) {
      return false;
    }

    assertThat(objects.size(), is(4));
    assertThat(((List) objects.get(0)).size(), is(mainProducersSize));

    if (tasty == null) {
      assertThat(objects.get(1), is(nullValue()));
    } else {
      assertThat(((HealthyFood) objects.get(1)).getTasteProfile().isTasty(), is(tasty));
    }
    assertThat(objects.get(2), is(shouldNotOverride));
    assertThat(objects.get(3), is(timeToPeel));

    return true;
  }

  private <T> T locateConfig(String name) throws MuleException {
    return (T) muleContext.getExtensionManager().getConfiguration(name, testEvent()).getValue();
  }

  private void pollingProbe(AppleConfig config, String name,
                            int mainProducersSize, Boolean tasty, String shouldNotOverride, int timeToPeel) {

    new PollingProber(30000, 300)
        .check(new JUnitLambdaProbe(() -> assertSourceResults(config.getResults(), name,
                                                              mainProducersSize, tasty, shouldNotOverride, timeToPeel)));
  }

}
