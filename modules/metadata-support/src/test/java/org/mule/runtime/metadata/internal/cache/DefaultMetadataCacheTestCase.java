/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metadata.internal.cache;

import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SDK_TOOLING_SUPPORT;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.MetadataTypeResolutionStory.METADATA_SERVICE;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.Serializable;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SDK_TOOLING_SUPPORT)
@Story(METADATA_SERVICE)
public class DefaultMetadataCacheTestCase extends AbstractMuleTestCase {

  private DefaultMetadataCache cache;

  @Before
  public void setUp() throws Exception {
    cache = new DefaultMetadataCache();
  }

  @Test
  public void getMissingKey() {
    Optional<Serializable> result = cache.get("missingKey");
    assertThat(result.isPresent(), is(false));
  }

  @Test
  public void putElementFirstTime() {
    final int someValue = 1123123;
    final int someKey = 1;

    Optional<Serializable> result = cache.get(someKey);
    assertThat(result.isPresent(), is(false));
    cache.put(someKey, someValue);

    result = cache.get(someKey);
    assertThat(result.isPresent(), is(true));
    assertThat(result.get(), is(someValue));
  }

  @Test
  public void updateValue() {
    final String someValue = "My Value";
    final String otherValue = "Updated Value";
    final String someKey = "someKey";

    cache.put(someKey, someValue);
    assertThat(cache.get(someKey).get(), is(someValue));

    cache.put(someKey, otherValue);
    assertThat(cache.get(someKey).get(), is(otherValue));
  }

  @Test
  public void putAllCreatesAndUpdates() {
    final String someValue = "My Value";
    final String otherValue = "Updated Value";
    final String someKey = "someKey";
    final int intValue = 1123123;
    final int otherKey = 10;

    cache.put(someKey, someValue);
    assertThat(cache.get(someKey).get(), is(someValue));
    assertThat(cache.get(otherKey).isPresent(), is(false));


    ImmutableMap<Serializable, Serializable> params = ImmutableMap.<Serializable, Serializable>builder()
        .put(someKey, otherValue)
        .put(otherKey, intValue)
        .build();

    cache.putAll(params);
    assertThat(cache.get(someKey).get(), is(otherValue));
    assertThat(cache.get(otherKey).get(), is(intValue));
  }

  @Test
  public void computeIfAbsentCreatesValue() throws MetadataResolvingException, ConnectionException {
    final String someKey = "someKey";
    final String someValue = "My Value";

    assertThat(cache.get(someKey).isPresent(), is(false));

    Serializable result = cache.computeIfAbsent(someKey, k -> someValue);
    assertThat(result, is(someValue));
    assertThat(cache.get(someKey).get(), is(someValue));
  }

  @Test
  public void computeIfAbsentDoesNotModifyExistingValue() throws MetadataResolvingException, ConnectionException {
    final String someKey = "someKey";
    final String someValue = "My Value";
    final String otherValue = "Other Value";

    cache.put(someKey, someValue);

    Serializable result = cache.computeIfAbsent(someKey, k -> otherValue);
    assertThat(result, is(someValue));
    assertThat(cache.get(someKey).get(), is(someValue));
  }
}
