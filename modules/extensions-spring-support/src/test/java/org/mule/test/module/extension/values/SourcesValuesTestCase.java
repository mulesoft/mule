/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.values;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mule.tck.junit4.matcher.ValueMatcher.valueWithId;
import org.mule.runtime.api.value.Value;
import org.mule.tck.junit4.matcher.ValueMatcher;
import org.junit.Test;

import java.util.Set;

public class SourcesValuesTestCase extends AbstractValuesTestCase {

  @Override
  protected String getConfigFile() {
    return "values/sources-values.xml";
  }

  @Test
  public void singleValues() throws Exception {
    Set<Value> channels = getValuesFromSource("simple-source", "channel");
    assertThat(channels, hasSize(3));
    assertThat(channels, hasValues("channel1", "channel2", "channel3"));
  }

  @Test
  public void singleValuesEnabledParameterWithConnection() throws Exception {
    Set<Value> channels = getValuesFromSource("source-with-connection", "channel");
    assertThat(channels, hasSize(3));
    assertThat(channels, hasValues("connection1", "connection2", "connection3"));
  }

  @Test
  public void singleValuesEnabledParameterWithConfiguration() throws Exception {
    Set<Value> channels = getValuesFromSource("source-with-configuration", "channel");
    assertThat(channels, hasSize(3));
    assertThat(channels, hasValues("config1", "config2", "config3"));
  }

  @Test
  public void singleValuesEnabledParameterWithRequiredParameters() throws Exception {
    Set<Value> channels = getValuesFromSource("source-with-values-with-required-parameters", "channels");
    assertThat(channels, hasSize(4));
    assertThat(channels, hasValues("requiredInteger:2", "requiredBoolean:false", "strings:[1, 2]", "requiredString:aString"));
  }

  @Test
  public void singleValuesEnabledParameterInsideParameterGroup() throws Exception {
    Set<Value> channels = getValuesFromSource("source-with-values-with-required-parameter-inside-param-group", "channels");
    assertThat(channels, hasSize(3));
    assertThat(channels, hasValues("channel1", "channel2", "channel3"));
  }

  @Test
  public void multiLevelValue() throws Exception {
    Set<Value> values = getValuesFromSource("source-with-multi-level-value", "values");
    ValueMatcher americaValue = valueWithId("America")
        .withDisplayName("America")
        .withPartName("continent")
        .withChilds(valueWithId("Argentina")
            .withDisplayName("Argentina")
            .withPartName("country")
            .withChilds(valueWithId("Buenos Aires")
                .withDisplayName("Buenos Aires")
                .withPartName("city")));

    assertThat(values, hasValues(americaValue));
  }

  @Test
  public void singleValuesWithRequiredParameterWithAlias() throws Exception {
    Set<Value> channels = getValuesFromSource("source-with-required-parameter-with-alias", "channels");
    assertThat(channels, hasSize(1));
    assertThat(channels, hasValues("requiredString:dummyValue"));
  }

  @Test
  public void optionsInsideShowInDslGroup() throws Exception {
    Set<Value> values = getValuesFromSource("source-with-required-parameter-inside-show-in-dsl-group", "values");
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("anyParameter:someValue"));
  }
}
