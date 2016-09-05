/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.metadata.extension.MetadataConnection.PERSON;
import static org.mule.test.metadata.extension.query.NativeQueryOutputResolver.NATIVE_QUERY;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.AMERICA;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.SAN_FRANCISCO;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.USA;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.test.metadata.extension.LocationKey;

import org.junit.Test;

public class RuntimeMetadataTestCase extends MetadataExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return METADATA_TEST;
  }

  @Test
  public void injectComposedMetadataKeyIdInstanceInOperation() throws Exception {
    LocationKey payload = flowRunner(SIMPLE_MULTILEVEL_KEY_RESOLVER).run().getMessage().getPayload();
    LocationKey expected = new LocationKey();
    expected.setContinent(AMERICA);
    expected.setCountry(USA);
    expected.setCity(SAN_FRANCISCO);
    assertThat(payload, is(expected));
  }

  @Test
  public void injectSimpleMetadataKeyIdInOperation() throws Exception {
    final String metadataKey = flowRunner(OUTPUT_ONLY_WITHOUT_CONTENT_PARAM).run().getMessage().getPayload();
    assertThat(metadataKey, is(PERSON));
  }

  @Test
  public void injectTranslatedNativeQuery() throws Exception {
    MuleEvent event = flowRunner(QUERY_FLOW).run();
    String nativeQuery = event.getMessage().getPayload();
    assertThat(nativeQuery, is(NATIVE_QUERY));
  }

  @Test
  public void injectNonTranslatedNativeQuery() throws Exception {
    MuleEvent event = flowRunner(NATIVE_QUERY_FLOW).run();
    String nativeQuery = event.getMessage().getPayload();
    assertThat(nativeQuery.trim(), is(NATIVE_QUERY));
  }
}
