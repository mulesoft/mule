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

import java.util.Set;

import org.junit.Test;

public class SourcesValuesTestCase extends AbstractValuesTestCase {

  private static final String AMERICA = "America";
  private static final String CONTINENT = "continent";
  private static final String ARGENTINA = "Argentina";
  private static final String COUNTRY = "country";
  private static final String BUENOS_AIRES = "Buenos Aires";
  private static final String CITY = "city";
  private static final String LA_PLATA = "La Plata";
  private static final String USA = "USA";
  private static final String UNITED_STATES_OF_AMERICA = "United States Of America";
  private static final String SAN_FRANCISCO = "San Francisco";

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
    ValueMatcher americaValue = valueWithId(AMERICA)
        .withDisplayName(AMERICA)
        .withPartName(CONTINENT)
        .withChilds(valueWithId(ARGENTINA)
            .withDisplayName(ARGENTINA)
            .withPartName(COUNTRY)
            .withChilds(valueWithId(BUENOS_AIRES)
                .withDisplayName(BUENOS_AIRES)
                .withPartName(CITY)));

    assertThat(values, hasValues(americaValue));
  }

  @Test
  public void childsOrder() throws Exception {
    Set<Value> values = getValuesFromSource("source-with-multi-level-value", "values");
    ValueMatcher americaValue = valueWithId("America").strict()
        .withDisplayName(AMERICA)
        .withPartName(CONTINENT)
        .withChilds(valueWithId(ARGENTINA)
            .withDisplayName(ARGENTINA)
            .withPartName(COUNTRY)
            .withChilds(valueWithId(LA_PLATA)
                .withDisplayName(LA_PLATA)
                .withPartName(CITY),
                        valueWithId(BUENOS_AIRES)
                            .withDisplayName(BUENOS_AIRES)
                            .withPartName(CITY)),
                    valueWithId(USA)
                        .withDisplayName(UNITED_STATES_OF_AMERICA)
                        .withPartName(COUNTRY)
                        .withChilds(valueWithId(SAN_FRANCISCO)
                            .withDisplayName(SAN_FRANCISCO)
                            .withPartName(CITY)));

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

  @Test
  public void sourcesMustNotStartWhenResolvingValue() throws Exception {
    Set<Value> hasBeenStarted = getValuesFromSource("source-must-not-start", "hasBeenStarted");
    assertThat(hasBeenStarted, hasValues("FALSE"));
  }
}
