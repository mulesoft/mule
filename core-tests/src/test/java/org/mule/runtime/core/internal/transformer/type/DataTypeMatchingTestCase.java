/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.type;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mule.runtime.api.metadata.DataType.builder;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.charset.StandardCharsets.UTF_16;
import static org.mule.tck.junit4.matcher.DataTypeCompatibilityMatcher.assignableTo;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class DataTypeMatchingTestCase extends AbstractMuleTestCase {


  private static final MediaType STAR_STAR_MEDIA_TYPE = MediaType.ANY;
  private static final MediaType APPLICATION_JSON_MEDIA_TYPE = MediaType.APPLICATION_JSON;
  private static final MediaType TEXT_MEDIA_TYPE = MediaType.TEXT;
  private static final MediaType FOO_MEDIA_TYPE = MediaType.create("foo", "foo");

  private static final DataType GENERIC_DATA_TYPE = builder().type(Object.class).mediaType(STAR_STAR_MEDIA_TYPE).build();
  private static final DataType GENERIC_TYPE_GENERIC_DATA_TYPE =
      builder().type(Object.class).mediaType(FOO_MEDIA_TYPE).build();
  private static final DataType GENERIC_MEDIA_TYPE_GENERIC_DATA_TYPE =
      builder().type(FOO.class).mediaType(STAR_STAR_MEDIA_TYPE).build();

  private static final DataType JSON_PARENT_DATA_TYPE =
      builder().type(JSON_PARENT.class).mediaType(APPLICATION_JSON_MEDIA_TYPE).build();
  private static final DataType JSON_SON_DATA_TYPE =
      builder().type(JSON_SON.class).mediaType(APPLICATION_JSON_MEDIA_TYPE).build();
  private static final DataType TEXT_DATA_TYPE = builder().type(TEXT.class).mediaType(TEXT_MEDIA_TYPE).build();

  private static final DataType[] dataTypes = {GENERIC_DATA_TYPE, GENERIC_TYPE_GENERIC_DATA_TYPE,
      GENERIC_MEDIA_TYPE_GENERIC_DATA_TYPE, JSON_PARENT_DATA_TYPE, JSON_SON_DATA_TYPE, TEXT_DATA_TYPE};

  private static class JSON_PARENT {
  }
  private static class JSON_SON extends JSON_PARENT {
  }
  private static class TEXT {
  }
  private static class FOO {
  }

  @Test
  public void sameDataTypeIsEqualsToItself() throws Exception {
    for (int i = 0; i < dataTypes.length; i++) {
      assertThat(dataTypes[i], is(equalTo(dataTypes[i])));
    }
  }

  @Test
  public void sameDataTypeIsCompatibleWithItself() throws Exception {
    for (int i = 0; i < dataTypes.length; i++) {
      assertThat(dataTypes[i], is(assignableTo(dataTypes[i])));
    }
  }

  @Test
  public void allDataTypesAreDifferent() throws Exception {
    for (int i = 0; i < dataTypes.length; i++) {
      for (int j = 0; j < dataTypes.length; j++) {
        if (i == j) {
          continue;
        }
        assertThat(dataTypes[i], is(not(equalTo(dataTypes[j]))));
        assertThat(dataTypes[j], is(not(equalTo(dataTypes[i]))));
      }
    }
  }

  @Test
  public void genericDataTypeShouldBeCompatibleWithEveryDataType() throws Exception {
    assertThat(JSON_PARENT_DATA_TYPE, is(assignableTo(GENERIC_DATA_TYPE)));
    assertThat(JSON_SON_DATA_TYPE, is(assignableTo(GENERIC_DATA_TYPE)));
    assertThat(TEXT_DATA_TYPE, is(assignableTo(GENERIC_DATA_TYPE)));
    assertThat(GENERIC_TYPE_GENERIC_DATA_TYPE, is(assignableTo(GENERIC_DATA_TYPE)));
    assertThat(GENERIC_MEDIA_TYPE_GENERIC_DATA_TYPE, is(assignableTo(GENERIC_DATA_TYPE)));
  }

  @Test
  public void noDataTypeShouldBeCompatibleWithTheGenericOne() throws Exception {
    assertThat(GENERIC_DATA_TYPE, is(not(assignableTo(JSON_PARENT_DATA_TYPE))));
    assertThat(GENERIC_DATA_TYPE, is(not(assignableTo(JSON_SON_DATA_TYPE))));
    assertThat(GENERIC_DATA_TYPE, is(not(assignableTo(TEXT_DATA_TYPE))));
  }

  @Test
  public void nonGenericDataTypesShouldNotBeCompatible() throws Exception {
    assertThat(JSON_PARENT_DATA_TYPE, is(not(assignableTo(TEXT_DATA_TYPE))));
    assertThat(TEXT_DATA_TYPE, is(not(assignableTo(JSON_PARENT_DATA_TYPE))));
    assertThat(JSON_SON_DATA_TYPE, is(not(assignableTo(TEXT_DATA_TYPE))));
    assertThat(TEXT_DATA_TYPE, is(not(assignableTo(JSON_SON_DATA_TYPE))));
  }

  @Test
  public void inheritedTypeDataTypesShouldBeCompatibleOneWay() throws Exception {
    assertThat(JSON_PARENT_DATA_TYPE, is(not(assignableTo(JSON_SON_DATA_TYPE))));
    assertThat(JSON_SON_DATA_TYPE, is(assignableTo(JSON_PARENT_DATA_TYPE)));
  }

  @Test
  public void ifCharsetItsNotSpecifiedItShouldBeCompatibleWithAny() throws Exception {
    DataType jsonWithCharset = builder(JSON_PARENT_DATA_TYPE).charset(UTF_8).build();
    assertThat(JSON_PARENT_DATA_TYPE, is(not(assignableTo(jsonWithCharset))));
    assertThat(jsonWithCharset, is(assignableTo(JSON_PARENT_DATA_TYPE)));
  }

  @Test
  public void differentCharsetsShouldNotBeCompatible() throws Exception {
    DataType jsonUtf8 = builder(JSON_PARENT_DATA_TYPE).charset(UTF_8).build();
    DataType jsonUtf16 = builder(JSON_PARENT_DATA_TYPE).charset(UTF_16).build();
    assertThat(jsonUtf8, is(not(assignableTo(jsonUtf16))));
    assertThat(jsonUtf16, is(not(assignableTo(jsonUtf8))));
  }


}
