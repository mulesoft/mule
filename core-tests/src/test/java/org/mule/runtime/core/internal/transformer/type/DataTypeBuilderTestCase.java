/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.type;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.of;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.el.BindingContext.builder;
import static org.mule.runtime.api.metadata.DataType.NUMBER;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.DataType.TEXT_STRING;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import static org.mule.runtime.core.api.util.IOUtils.toByteArray;
import static org.mule.tck.junit4.matcher.DataTypeCompatibilityMatcher.assignableTo;
import static org.mule.tck.probe.PollingProber.DEFAULT_POLLING_INTERVAL;

import static java.util.Collections.singletonList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionFunction;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.api.metadata.FunctionDataType;
import org.mule.runtime.api.metadata.FunctionParameter;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.internal.metadata.DefaultCollectionDataType;
import org.mule.runtime.core.internal.metadata.DefaultFunctionDataType;
import org.mule.runtime.core.internal.metadata.DefaultMapDataType;
import org.mule.runtime.core.internal.metadata.SimpleDataType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.report.HeapDumpOnFailure;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DataTypeBuilderTestCase extends AbstractMuleTestCase {

  private static final int GC_POLLING_TIMEOUT = 10000;

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Rule
  public HeapDumpOnFailure heapDumpOnFailure = new HeapDumpOnFailure();

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
    assertThat(((DefaultCollectionDataType) dataType).getItemDataType(), is(OBJECT));
  }

  @Test
  public void buildFunction() {
    FunctionDataType dataType = (FunctionDataType) DataType.fromFunction(new SomeFunction());

    // Return type
    assertThat(dataType.getReturnType().isPresent(), is(true));
    assertThat(dataType.getReturnType().get(), equalTo(STRING));
    // Parameters
    assertThat(dataType.getParameters(), hasSize(2));
    FunctionParameter first = dataType.getParameters().get(0);
    assertThat(first.getName(), is("fst"));
    assertThat(first.getType(), equalTo(NUMBER));
    assertThat(first.getDefaultValueResolver(), nullValue());
    FunctionParameter second = dataType.getParameters().get(1);
    assertThat(second.getName(), is("snd"));
    assertThat(second.getType(), equalTo(OBJECT));
    // Default
    assertThat(second.getDefaultValueResolver().getDefaultValue(builder().build()), is("wow"));
  }

  @Test
  public void buildMap() {
    final DataType dataType = DataType.fromType(HashMap.class);
    assertThat(dataType, instanceOf(DefaultMapDataType.class));
    assertThat(dataType.getType(), is(equalTo(HashMap.class)));
    assertThat(((DefaultMapDataType) dataType).getKeyDataType(), is(OBJECT));
    assertThat(((DefaultMapDataType) dataType).getValueDataType(), is(OBJECT));
  }

  @Test
  public void buildTypedCollection() {
    final DataType dataType = DataType.builder()
        .collectionType(List.class)
        .itemType(String.class)
        .itemMediaType(APPLICATION_JSON)
        .build();
    assertThat(dataType, instanceOf(DefaultCollectionDataType.class));
    assertThat(dataType.getType(), is(equalTo(List.class)));
    DataType itemDataType = ((DefaultCollectionDataType) dataType).getItemDataType();
    assertThat(itemDataType.getType(), equalTo(String.class));
    assertThat(itemDataType.getMediaType(), is(APPLICATION_JSON));
  }

  @Test
  public void buildTypedMap() {
    final DataType dataType = DataType.builder()
        .mapType(HashMap.class)
        .keyType(Number.class)
        .keyMediaType(APPLICATION_JAVA)
        .valueType(String.class)
        .valueMediaType(APPLICATION_JSON)
        .build();
    assertThat(dataType, instanceOf(DefaultMapDataType.class));
    assertThat(dataType.getType(), is(equalTo(HashMap.class)));
    DataType keyDataType = ((DefaultMapDataType) dataType).getKeyDataType();
    assertThat(keyDataType.getType(), equalTo(Number.class));
    assertThat(keyDataType.getMediaType(), is(APPLICATION_JAVA));
    DataType valueDataType = ((DefaultMapDataType) dataType).getValueDataType();
    assertThat(valueDataType.getType(), equalTo(String.class));
    assertThat(valueDataType.getMediaType(), is(APPLICATION_JSON));
  }

  @Test
  public void buildTypedCollectionFromImplementationClass() {
    final DataType dataType = DataType.builder().collectionType(SpecificCollection.class).build();
    assertThat(dataType, instanceOf(DefaultCollectionDataType.class));
    assertThat(dataType.getType(), is(equalTo(SpecificCollection.class)));
    assertThat(((DefaultCollectionDataType) dataType).getItemDataType(), is(STRING));
  }

  @Test
  public void buildTypedMapFromImplementationClass() {
    final DataType dataType = DataType.builder().mapType(SpecificMap.class).build();
    assertThat(dataType, instanceOf(DefaultMapDataType.class));
    assertThat(dataType.getType(), is(equalTo(SpecificMap.class)));
    assertThat(((DefaultMapDataType) dataType).getKeyDataType(), is(STRING));
    assertThat(((DefaultMapDataType) dataType).getValueDataType(), is(NUMBER));
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
    assertThat(((DefaultCollectionDataType) dataType).getItemDataType(), is(OBJECT));
    assertThat(dataType.getMediaType().getPrimaryType(), is("text"));
    assertThat(dataType.getMediaType().getSubType(), is("plain"));
    assertThat(dataType.getMediaType().getCharset().get(), is(US_ASCII));
  }

  @Test
  public void templateTypedCollection() {
    final DataType template =
        DataType.builder()
            .collectionType(List.class)
            .itemType(String.class)
            .itemMediaType("application/json;charset=UTF-8")
            .mediaType("text/plain;charset=ASCII")
            .build();
    final DataType dataType = DataType.builder(template).build();

    assertThat(dataType, instanceOf(DefaultCollectionDataType.class));
    assertThat(dataType.getType(), is(equalTo(List.class)));
    assertThat(((DefaultCollectionDataType) dataType).getItemDataType(), is(assignableTo(STRING)));
    assertThat(((DefaultCollectionDataType) dataType).getItemDataType().getMediaType().getPrimaryType(), is("application"));
    assertThat(((DefaultCollectionDataType) dataType).getItemDataType().getMediaType().getSubType(), is("json"));
    assertThat(((DefaultCollectionDataType) dataType).getItemDataType().getMediaType().getCharset().get(), is(UTF_8));
    assertThat(dataType.getMediaType().getPrimaryType(), is("text"));
    assertThat(dataType.getMediaType().getSubType(), is("plain"));
    assertThat(dataType.getMediaType().getCharset().get(), is(US_ASCII));
  }

  @Test
  public void templateMap() {
    final DataType template = DataType.builder().type(HashMap.class).mediaType("text/plain;charset=ASCII").build();
    final DataType dataType = DataType.builder(template).build();

    assertThat(dataType, instanceOf(DefaultMapDataType.class));
    assertThat(dataType.getType(), is(equalTo(HashMap.class)));
    assertThat(((DefaultMapDataType) dataType).getKeyDataType(), is(OBJECT));
    assertThat(((DefaultMapDataType) dataType).getValueDataType(), is(OBJECT));
    assertThat(dataType.getMediaType().getPrimaryType(), is("text"));
    assertThat(dataType.getMediaType().getSubType(), is("plain"));
    assertThat(dataType.getMediaType().getCharset().get(), is(US_ASCII));
  }

  @Test
  public void templateTypedMap() {
    final DataType template = DataType.builder()
        .mapType(HashMap.class)
        .keyType(String.class)
        .keyMediaType("text/plain;charset=UTF-8")
        .valueType(Number.class)
        .valueMediaType("application/json;charset=ISO-8859-1")
        .mediaType("text/plain;charset=ASCII")
        .build();
    final DataType dataType = DataType.builder(template).build();

    assertThat(dataType, instanceOf(DefaultMapDataType.class));
    assertThat(dataType.getType(), is(equalTo(HashMap.class)));
    assertThat(((DefaultMapDataType) dataType).getKeyDataType(), is(assignableTo(TEXT_STRING)));
    assertThat(((DefaultMapDataType) dataType).getKeyDataType().getMediaType().getPrimaryType(), is("text"));
    assertThat(((DefaultMapDataType) dataType).getKeyDataType().getMediaType().getSubType(), is("plain"));
    assertThat(((DefaultMapDataType) dataType).getKeyDataType().getMediaType().getCharset().get(), is(UTF_8));
    assertThat(((DefaultMapDataType) dataType).getValueDataType(), is(assignableTo((NUMBER))));
    assertThat(((DefaultMapDataType) dataType).getValueDataType().getMediaType().getPrimaryType(), is("application"));
    assertThat(((DefaultMapDataType) dataType).getValueDataType().getMediaType().getSubType(), is("json"));
    assertThat(((DefaultMapDataType) dataType).getValueDataType().getMediaType().getCharset().get(), is(ISO_8859_1));
  }

  @Test
  public void templateFunction() {
    FunctionParameter functionParameter = new FunctionParameter("fst", NUMBER);
    final DataType template = DataType.builder()
        .functionType(SomeFunction.class)
        .returnType(STRING)
        .parametersType(singletonList(functionParameter))
        .build();
    final DataType dataType = DataType.builder(template).build();

    assertThat(dataType, instanceOf(DefaultFunctionDataType.class));
    assertThat(dataType.getType(), is(equalTo(SomeFunction.class)));
    assertThat(((DefaultFunctionDataType) dataType).getReturnType().get(), is(STRING));
    assertThat(((DefaultFunctionDataType) dataType).getParameters(), hasItems(functionParameter));
  }

  @Test
  public void proxy() {
    final Class<?> muleMessageProxy = Proxy.getProxyClass(DataTypeBuilderTestCase.class.getClassLoader(), Message.class);

    final DataType dataType = DataType.fromType(muleMessageProxy);

    assertThat(dataType.getType(), is(equalTo(Message.class)));
  }

  @Test
  public void cglibInterfaceProxy() {
    final Message muleMessageProxy = mock(Message.class);

    final DataType dataType = DataType.fromObject(muleMessageProxy);

    assertThat(dataType.getType(), is(equalTo(Message.class)));
  }

  @Test
  public void cglibClassProxy() {
    final Message muleMessageProxy = mock(MessageTestImpl.class);

    final DataType dataType = DataType.fromObject(muleMessageProxy);

    assertThat(dataType.getType(), is(equalTo(MessageTestImpl.class)));
  }

  @Test
  public void cglibSpyProxy() {
    final Message muleMessageProxy = spy(new MessageTestImpl());

    final DataType dataType = DataType.fromObject(muleMessageProxy);

    assertThat(dataType.getType(), is(equalTo(MessageTestImpl.class)));
  }

  private static class MessageTestImpl implements Message {

    @Override
    public <T> TypedValue<T> getPayload() {
      return null;
    }

    @Override
    public <T> TypedValue<T> getAttributes() {
      return null;
    }

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

  @Test
  public void cacheClean() throws InterruptedException, ClassNotFoundException {
    ClassLoader custom = new ClassLoader(this.getClass().getClassLoader()) {

      @Override
      public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (Message.class.getName().equals(name)) {
          byte[] classBytes;
          try {
            classBytes = toByteArray(this.getClass().getResourceAsStream("/org/mule/runtime/api/message/Message.class"));
            return this.defineClass(null, classBytes, 0, classBytes.length);
          } catch (Exception e) {
            return super.loadClass(name);
          }
        } else {
          return super.loadClass(name);
        }
      }
    };

    PhantomReference<ClassLoader> clRef = new PhantomReference<>(custom, new ReferenceQueue<>());
    DataType.builder().type(custom.loadClass(Message.class.getName())).build();
    custom = null;

    new PollingProber(GC_POLLING_TIMEOUT, DEFAULT_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      System.gc();
      assertThat(clRef.isEnqueued(), is(true));
      return true;
    }, "A hard reference is being mantained to the type of the DataType."));
  }

  private class SpecificMap extends HashMap<String, Number> {

  }

  private class SpecificCollection extends LinkedList<String> {

  }

  private class SomeFunction implements ExpressionFunction {

    @Override
    public Object call(Object[] objects, BindingContext bindingContext) {
      return null;
    }

    @Override
    public Optional<DataType> returnType() {
      return of(STRING);
    }

    @Override
    public List<FunctionParameter> parameters() {
      List<FunctionParameter> parameters = new ArrayList<>();
      parameters.add(new FunctionParameter("fst", NUMBER));
      parameters.add(new FunctionParameter("snd", OBJECT, ctx -> "wow"));
      return parameters;
    }

  }
}
