/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.metadata;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.metadata.extension.MetadataConnection.CAR;
import static org.mule.test.metadata.extension.MetadataConnection.PERSON;
import static org.mule.test.metadata.extension.query.NativeQueryOutputResolver.NATIVE_QUERY;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.AMERICA;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.SAN_FRANCISCO;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.USA;

import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.test.metadata.extension.LocationKey;
import org.mule.test.metadata.extension.model.animals.AnimalClade;

import org.junit.Test;

public class RuntimeMetadataTestCase extends AbstractMetadataOperationTestCase {

  public RuntimeMetadataTestCase(ResolutionType resolutionType) {
    super(resolutionType);
  }

  @Override
  protected String getConfigFile() {
    return METADATA_TEST;
  }

  @Test
  public void injectComposedMetadataKeyIdInstanceInOperation() throws Exception {
    LocationKey payload = (LocationKey) flowRunner(SIMPLE_MULTILEVEL_KEY_RESOLVER).run().getMessage().getPayload().getValue();
    LocationKey expected = new LocationKey();
    expected.setContinent(AMERICA);
    expected.setCountry(USA);
    expected.setCity(SAN_FRANCISCO);
    assertThat(payload, is(expected));
  }

  @Test
  public void injectSimpleMetadataKeyIdInOperation() throws Exception {
    final String metadataKey = (String) flowRunner(OUTPUT_ONLY_WITHOUT_CONTENT_PARAM).run().getMessage().getPayload().getValue();
    assertThat(metadataKey, is(PERSON));
  }

  @Test
  public void injectTranslatedNativeQuery() throws Exception {
    BaseEvent event = flowRunner(QUERY_FLOW).run();
    String nativeQuery = (String) event.getMessage().getPayload().getValue();
    assertThat(nativeQuery, is(NATIVE_QUERY));
  }

  @Test
  public void injectNonTranslatedNativeQuery() throws Exception {
    BaseEvent event = flowRunner(NATIVE_QUERY_FLOW).run();
    String nativeQuery = (String) event.getMessage().getPayload().getValue();
    assertThat(nativeQuery.trim(), is(NATIVE_QUERY));
  }

  @Test
  public void enumMetadataKey() throws Exception {
    BaseEvent event = flowRunner(ENUM_METADATA_KEY).run();
    AnimalClade key = (AnimalClade) event.getMessage().getPayload().getValue();
    assertThat(key, is(AnimalClade.MAMMAL));
  }

  @Test
  public void booleanMetadataKey() throws Exception {
    BaseEvent event = flowRunner(BOOLEAN_METADATA_KEY).run();
    boolean key = (boolean) event.getMessage().getPayload().getValue();
    assertThat(key, is(true));
  }

  @Test
  public void metadataKeyDefaultValue() throws Exception {
    BaseEvent event = flowRunner(METADATA_KEY_DEFAULT_VALUE).run();
    assertThat(event.getMessage().getPayload().getValue(), is(CAR));
  }
}
