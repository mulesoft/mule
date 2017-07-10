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
import org.junit.Test;

import java.util.Set;

public class ConnectionProviderValuesTestCase extends AbstractValuesTestCase {

  @Override
  protected String getConfigFile() {
    return "values/connection-provider-values.xml";
  }

  @Test
  public void connectionWithValues() throws Exception {
    Set<Value> channels = getValuesFromConnection("with-value-parameter-connection", "channel");
    assertThat(channels, hasSize(3));
    assertThat(channels, hasValues("channel1", "channel2", "channel3"));
  }

  @Test
  public void connectionWithValuesAndRequiredParameters() throws Exception {
    Set<Value> channels = getValuesFromConnection("with-value-with-required-param-connection", "channel");
    assertThat(channels, hasSize(2));
    assertThat(channels, hasValues("required2:value2", "required1:value1"));
  }

  @Test
  public void connectionWithValuesWithRequiredParamsFromParamGroup() throws Exception {
    Set<Value> channels = getValuesFromConnection("values-with-required-params-from-param-group-connection", "valueParam");
    assertThat(channels, hasSize(1));
    assertThat(channels, hasValues("anyParameter:someValue"));
  }

  @Test
  public void connectionWithValuesWithRequiredParamsFromShowInDslGroup() throws Exception {
    Set<Value> channels =
        getValuesFromConnection("values-with-required-params-from-show-in-dsl-group-connection", "valueParam");
    assertThat(channels, hasSize(1));
    assertThat(channels, hasValues("anyParameter:someValue"));
  }
}
