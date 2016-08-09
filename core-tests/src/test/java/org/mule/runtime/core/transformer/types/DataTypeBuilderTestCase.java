/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.types;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.core.metadata.DefaultCollectionDataType;
import org.mule.runtime.core.metadata.SimpleDataType;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DataTypeBuilderTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Test
  public void buildSimple() {
    final DataType dataType = DataType.fromType(String.class);
    assertThat(dataType, instanceOf(SimpleDataType.class));
    assertThat(dataType.getType(), is(equalTo(String.class)));
  }

  @Test
  public void buildCollection() {
    final DataType dataType = DataType.fromType(Set.class);
    assertThat(dataType, instanceOf(DefaultCollectionDataType.class));
    assertThat(dataType.getType(), is(equalTo(Set.class)));
    assertThat(((DefaultCollectionDataType) dataType).getItemDataType(), is(DataType.OBJECT));
  }

  @Test
  public void buildTypedCollection() {
    final DataType dataType = DataType.builder().collectionType(List.class).itemType(String.class).build();
    assertThat(dataType, instanceOf(DefaultCollectionDataType.class));
    assertThat(dataType.getType(), is(equalTo(List.class)));
    assertThat(((DefaultCollectionDataType) dataType).getItemDataType(), is(DataType.STRING));
  }

  @Test
  public void templateSimple() {
    final DataType template = DataType.builder().type(String.class).mediaType("text/plain;charset=ASCII").build();
    final DataType dataType = DataType.builder(template).build();

    assertThat(dataType, instanceOf(SimpleDataType.class));
    assertThat(dataType.getType(), is(equalTo(String.class)));
    assertThat(dataType.getMediaType().getPrimaryType(), is("text"));
    assertThat(dataType.getMediaType().getSubType(), is("plain"));
    assertThat(dataType.getMediaType().getCharset().get(), is(US_ASCII));
  }

  @Test
  public void templateCollection() {
    final DataType template = DataType.builder().type(Set.class).mediaType("text/plain;charset=ASCII").build();
    final DataType dataType = DataType.builder(template).build();

    assertThat(dataType, instanceOf(DefaultCollectionDataType.class));
    assertThat(dataType.getType(), is(equalTo(Set.class)));
    assertThat(((DefaultCollectionDataType) dataType).getItemDataType(), is(DataType.OBJECT));
    assertThat(dataType.getMediaType().getPrimaryType(), is("text"));
    assertThat(dataType.getMediaType().getSubType(), is("plain"));
    assertThat(dataType.getMediaType().getCharset().get(), is(US_ASCII));
  }

  @Test
  public void templateTypedCollection() {
    final DataType template =
        DataType.builder().collectionType(List.class).itemType(String.class).mediaType("text/plain;charset=ASCII").build();
    final DataType dataType = DataType.builder(template).build();

    assertThat(dataType, instanceOf(DefaultCollectionDataType.class));
    assertThat(dataType.getType(), is(equalTo(List.class)));
    assertThat(((DefaultCollectionDataType) dataType).getItemDataType(), is(DataType.STRING));
  }

  @Test
  public void proxy() {
    final Class<?> muleMessageProxy = Proxy.getProxyClass(DataTypeBuilderTestCase.class.getClassLoader(), MuleMessage.class);

    final DataType dataType = DataType.fromType(muleMessageProxy);

    assertThat(dataType.getType(), is(equalTo(MuleMessage.class)));
  }

  @Test
  public void mimeTypeWithEncoding() {
    final DataType dataType = DataType.builder().mediaType("text/plain;charset=ASCII").build();

    assertThat(dataType.getMediaType().getPrimaryType(), is("text"));
    assertThat(dataType.getMediaType().getSubType(), is("plain"));
    assertThat(dataType.getMediaType().getCharset().get(), is(US_ASCII));
  }

  @Test
  public void invalidMimeType() {
    expected.expect(IllegalArgumentException.class);
    final DataType dataType = DataType.builder().mediaType("imInvalid").build();
  }

  @Test
  public void invalidEncoding() {
    expected.expect(IllegalArgumentException.class);
    final DataType dataType = DataType.builder().charset("imInvalid").build();
  }

  @Test
  public void recycleBuilder() {
    final DataTypeParamsBuilder builder = DataType.builder().type(String.class);
    builder.build();

    expected.expect(IllegalStateException.class);
    builder.build();
  }

  @Test
  public void cachedInstances() {
    final DataTypeParamsBuilder builder1 = DataType.builder().type(String.class);
    final DataTypeParamsBuilder builder2 = DataType.builder().type(String.class);

    assertThat(builder1, equalTo(builder2));
    assertThat(builder1.build(), sameInstance(builder2.build()));
  }
}
