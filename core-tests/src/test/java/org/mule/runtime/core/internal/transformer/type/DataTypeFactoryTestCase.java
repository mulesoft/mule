/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.transformer.type;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_16;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.internal.metadata.DefaultCollectionDataType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.nio.charset.Charset;
import java.util.List;

import org.junit.Test;

@SmallTest
public class DataTypeFactoryTestCase extends AbstractMuleTestCase {

  private final Charset encoding = UTF_16;
  private final String mimeType = "application/json";
  private final Class<String> type = String.class;

  @Test
  public void createsDataTypeForNullPayload() throws Exception {
    DataType dataType = DataType.fromObject(null);

    assertThat(dataType, like(Object.class, MediaType.ANY, null));
  }

  @Test
  public void createsDataTypeForNonNullObject() throws Exception {
    DataType dataType = DataType.fromObject("test");

    assertThat(dataType, like(String.class, MediaType.ANY, null));
  }

  @Test
  public void mimeTypeWithEncodingInformation() throws Exception {
    DataType dataType = DataType.builder().type(type).mediaType(format("%s; charset=UTF-8", mimeType)).charset(encoding).build();
    assertThat(dataType.getType(), equalTo(type));
    assertThat(dataType.getMediaType().getPrimaryType(), is(mimeType.split("/")[0]));
    assertThat(dataType.getMediaType().getSubType(), is(mimeType.split("/")[1]));
    assertThat(dataType.getMediaType().getCharset().get(), is(encoding));
  }

  @Test
  public void createsDataTypeForNonCollection() {
    final DataType dataType = DataType.builder().collectionType(List.class).itemType(type).itemMediaType(mimeType).build();

    assertThat(dataType.getType(), equalTo(List.class));
    assertThat(dataType, instanceOf(DefaultCollectionDataType.class));
    final DataType itemDataType = ((DefaultCollectionDataType) dataType).getItemDataType();
    assertThat(itemDataType.getType(), equalTo(type));
    assertThat(itemDataType.getMediaType().getPrimaryType(), is(mimeType.split("/")[0]));
    assertThat(itemDataType.getMediaType().getSubType(), is(mimeType.split("/")[1]));
  }
}
