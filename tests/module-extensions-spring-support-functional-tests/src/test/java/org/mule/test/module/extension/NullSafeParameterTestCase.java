/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.test.vegan.extension.BananaConfig;
import org.mule.test.vegan.extension.FarmedFood;
import org.mule.test.vegan.extension.HealthyFood;
import org.mule.test.vegan.extension.RottenFood;
import org.mule.test.vegan.extension.VeganPolicy;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;

public class NullSafeParameterTestCase extends AbstractExtensionFunctionalTestCase {

  @Inject
  @Named("nullSafeOnSourceGroupAndPojo")
  private Flow nullSafeOnSourceGroupAndPojoFlow;

  @Override
  protected String getConfigFile() {
    return "vegan-null-safe-operation.xml";
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @Test
  public void getNullSafeObject() throws Exception {
    VeganPolicy policy = (VeganPolicy) flowRunner("policy").run().getMessage().getPayload().getValue();
    assertThat(policy, is(notNullValue()));
    assertThat(policy.getMeetAllowed(), is(false));
    assertThat(policy.getIngredients(), is(notNullValue()));
    assertThat(policy.getIngredients().getSaltMiligrams(), is(0));
    assertThat(policy.getIngredients().getSaltReplacementName(), is(nullValue()));
  }

  @Test
  public void getNullSafeAbstractObjectWithDefault() throws Exception {
    FarmedFood response = (FarmedFood) flowRunner("implementingType").run().getMessage().getPayload().getValue();
    assertThat(response, is(notNullValue()));
    assertThat(response, instanceOf(HealthyFood.class));
    assertThat(response.canBeEaten(), is(true));
  }

  @Test
  public void nestedNullSafe() throws Exception {
    assertNestedNullSafe("implementingType");
  }

  private void assertNestedNullSafe(String flowName) throws Exception {
    FarmedFood response = (FarmedFood) flowRunner(flowName).run().getMessage().getPayload().getValue();
    assertThat(response, is(instanceOf(HealthyFood.class)));
    HealthyFood healthyFood = (HealthyFood) response;
    assertHealthyFood(healthyFood);
  }

  @Test
  public void topLevelNestedNullSafe() throws Exception {
    assertNestedNullSafe("topLevelNullSafe");
  }

  private void assertHealthyFood(HealthyFood healthyFood) {
    assertThat(healthyFood.getTasteProfile(), is(notNullValue()));
    assertThat(healthyFood.getTasteProfile().isTasty(), is(false));
  }

  @Test
  public void nestedNullSafeInConfig() throws Exception {
    BananaConfig config = (BananaConfig) flowRunner("inConfig").run().getMessage().getPayload().getValue();
    assertHealthyFood(config.getHealthyFood());
  }

  @Test
  public void nullSafeOnMap() throws Exception {
    TypedValue<Object> payload = flowRunner("nullSafeOnMap").run().getMessage().getPayload();
    Object nullSafeMap = payload.getValue();
    assertThat(nullSafeMap, is(instanceOf(Map.class)));
    assertThat(((Map) nullSafeMap).isEmpty(), is(true));
  }

  @Test
  public void nullSafeOnList() throws Exception {
    TypedValue<Object> payload = flowRunner("nullSafeOnList").run().getMessage().getPayload();
    Object nullSafeMap = payload.getValue();
    assertThat(nullSafeMap, is(instanceOf(List.class)));
    assertThat(((List<?>) nullSafeMap), is(empty()));
  }

  @Test
  public void nullSafeOnGroupAndPojo() throws Exception {
    TypedValue<Object> payload = flowRunner("nullSafeOnGroupAndPojo").run().getMessage().getPayload();
    Object listOfNullsafes = payload.getValue();
    assertThat(listOfNullsafes, is(instanceOf(List.class)));
    assertThat(((List<?>) listOfNullsafes).get(0), is(instanceOf(RottenFood.class)));
    assertThat(((List<?>) listOfNullsafes).get(1), is(instanceOf(RottenFood.class)));
  }

  @Test
  public void nullSafeOnSourceGroupAndPojo() throws Exception {
    nullSafeOnSourceGroupAndPojoFlow.start();
    assertThat(nullSafeOnSourceGroupAndPojoFlow.getLifecycleState().isStarted(), is(true));
  }
}
