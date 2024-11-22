/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.transformer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Every.everyItem;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.internal.transformer.CompositeConverter;
import org.mule.runtime.core.internal.transformer.simple.ByteArrayToObject;
import org.mule.runtime.core.internal.transformer.simple.InputStreamToByteArray;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class CompositeConverterTestCase extends AbstractMuleTestCase {

  private Converter mockConverterA = mock(Converter.class);
  private Converter mockConverterB = mock(Converter.class);

  @Test(expected = IllegalArgumentException.class)
  public void rejectsEmptyCompositeTransformer() throws Exception {
    new CompositeConverter();
  }

  @Test
  public void isSourceDataTypeSupported() {
    Converter converter = mock(Converter.class);
    when(converter.isSourceDataTypeSupported(DataType.STRING)).thenReturn(true);
    CompositeConverter chain = new CompositeConverter(converter);
    assertThat(chain.isSourceDataTypeSupported(DataType.STRING), equalTo(true));
  }

  @Test
  public void getSourceDataTypes() {
    DataType[] dataTypes = new DataType[] {DataType.STRING};
    Converter converter = mock(Converter.class);
    when(converter.getSourceDataTypes()).thenReturn(asList(dataTypes));
    CompositeConverter chain = new CompositeConverter(converter);

    assertThat(DataType.STRING, equalTo(chain.getSourceDataTypes().get(0)));
  }

  @Test
  public void isAcceptNull() {
    Converter converter = mock(Converter.class);
    when(converter.isAcceptNull()).thenReturn(true);
    CompositeConverter chain = new CompositeConverter(converter);

    assertThat(chain.isAcceptNull(), equalTo(true));
  }

  @Test
  public void isIgnoreBadInput() {
    Converter converter = mock(Converter.class);
    when(converter.isIgnoreBadInput()).thenReturn(true);
    CompositeConverter chain = new CompositeConverter(converter);

    assertThat(chain.isIgnoreBadInput(), equalTo(true));
  }

  @Test
  public void setReturnDataType() {
    Converter converter = mock(Converter.class);
    CompositeConverter chain = new CompositeConverter(converter);
    chain.setReturnDataType(DataType.STRING);

    verify(converter, atLeastOnce()).setReturnDataType(DataType.STRING);
  }

  @Test
  public void getReturnDataType() {
    doReturn(DataType.STRING).when(mockConverterB).getReturnDataType();
    CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);

    assertThat(DataType.STRING, equalTo(compositeConverter.getReturnDataType()));
  }

  @Test
  public void priorityWeighting() throws Exception {
    CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);
    when(mockConverterA.getPriorityWeighting()).thenReturn(1);
    when(mockConverterB.getPriorityWeighting()).thenReturn(2);

    int priorityWeighting = compositeConverter.getPriorityWeighting();

    assertThat(3, equalTo(priorityWeighting));
  }

  @Test
  public void initialise() throws Exception {
    CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);

    compositeConverter.initialise();

    verify(mockConverterA, atLeastOnce()).initialise();
    verify(mockConverterB, atLeastOnce()).initialise();
  }

  @Test
  public void dispose() throws Exception {
    CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);

    compositeConverter.dispose();

    verify(mockConverterA, atLeastOnce()).dispose();
    verify(mockConverterB, atLeastOnce()).dispose();
  }

  @Test
  public void transform() throws Exception {
    doReturn("MyOutput1").when(mockConverterA).transform(any());
    doReturn(DataType.builder().charset(UTF_8).build()).when(mockConverterA).getReturnDataType();
    doReturn("MyOutput2").when(mockConverterB).transform(eq("MyOutput1"), eq(UTF_8));
    doReturn(DataType.builder().charset(UTF_8).build()).when(mockConverterB).getReturnDataType();
    CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);

    Object output = compositeConverter.transform("MyInput");

    verify(mockConverterA, times(1)).transform("MyInput");
    verify(mockConverterB, times(1)).transform("MyOutput1", UTF_8);
    assertThat("MyOutput2", equalTo(output));
  }

  @Test
  public void equalsReturnsTrueOnCompositeConvertersWithSameNameAndSameTransformationChain() {
    Converter byteArrayToObjectConverter = configureTransformer(new ByteArrayToObject());
    Converter inputStreamToByteArrayConverter = configureTransformer(new InputStreamToByteArray());
    CompositeConverter compositeConverterA = new CompositeConverter(byteArrayToObjectConverter, inputStreamToByteArrayConverter);
    CompositeConverter compositeConverterB = new CompositeConverter(byteArrayToObjectConverter, inputStreamToByteArrayConverter);

    assertThat(compositeConverterA, equalTo(compositeConverterB));
  }


  @Test
  public void equalsReturnsFalseOnCompositeConvertersWithDifferentTransformationChain() {
    Converter byteArrayToObjectConverter = configureTransformer(new ByteArrayToObject());
    Converter inputStreamToByteArrayConverter = configureTransformer(new InputStreamToByteArray());
    CompositeConverter compositeConverterA = new CompositeConverter(byteArrayToObjectConverter, inputStreamToByteArrayConverter);
    CompositeConverter compositeConverterB = new CompositeConverter(inputStreamToByteArrayConverter, byteArrayToObjectConverter);

    assertThat(compositeConverterA, not(equalTo(compositeConverterB)));
  }

  @Test
  public void hashCodeForCompositeConvertersChangesWithDifferentTransformationChain() {
    Converter byteArrayToObjectConverter = configureTransformer(new ByteArrayToObject());
    Converter inputStreamToByteArrayConverter = configureTransformer(new InputStreamToByteArray());

    Converter byteArrayToObjectConverter2 = configureTransformer(new ByteArrayToObject());
    Converter inputStreamToByteArrayConverter2 = configureTransformer(new InputStreamToByteArray());

    int hashCodeConverterA = new CompositeConverter(byteArrayToObjectConverter, inputStreamToByteArrayConverter).hashCode();

    int hashCodeConverterAClone = new CompositeConverter(byteArrayToObjectConverter, inputStreamToByteArrayConverter).hashCode();
    int hashCodeConverterAnotherClone =
        new CompositeConverter(byteArrayToObjectConverter2, inputStreamToByteArrayConverter2).hashCode();

    assertThat(asList(hashCodeConverterAClone, hashCodeConverterAnotherClone), everyItem(equalTo(hashCodeConverterA)));
  }

  protected final <T extends Converter> T configureTransformer(T transformer) {
    if (transformer instanceof AbstractTransformer t) {
      t.setArtifactEncoding(() -> UTF_8);
    }
    return transformer;
  }
}
