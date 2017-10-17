/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.values;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import org.mule.runtime.api.value.Value;

import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

public class ConfigurationValuesTestCase extends AbstractValuesTestCase {

  @Override
  protected String getConfigFile() {
    return "values/configuration-values.xml";
  }

  @Test
  public void configWithValues() throws Exception {
    Set<Value> channels = getValuesFromConfig("config-with-value", "channel");
    assertThat(channels, hasSize(3));
    assertThat(channels, hasValues("channel1", "channel2", "channel3"));
  }

  @Test
  public void configWithValuesWithRequiredParameters() throws Exception {
    Set<Value> channels = getValuesFromConfig("value-with-required-param", "channel");
    assertThat(channels, hasSize(2));
    assertThat(channels, hasValues("required2:value2", "required1:value1"));
  }

  @Test
  public void configWithValuesWithRequiredParamsFromParamGroup() throws Exception {
    Set<Value> channels = getValuesFromConfig("values-with-required-params-from-param-group-config", "valueParam");
    assertThat(channels, hasSize(1));
    assertThat(channels, hasValues("anyParameter:someValue"));
  }

  @Test
  @Ignore("MULE-13786")
  public void configWithValuesWithRequiredParamsFromShowInDslGroup() throws Exception {
    Set<Value> channels = getValuesFromConfig("values-with-required-params-from-show-in-dsl-group", "valueParam");
    assertThat(channels, hasSize(1));
    assertThat(channels, hasValues("anyParameter:someValue"));
  }
}
