/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.types;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.el.BindingContext.builder;
import static org.mule.runtime.api.metadata.DataType.NUMBER;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.core.util.IOUtils.toByteArray;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionFunction;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.api.metadata.FunctionDataType;
import org.mule.runtime.api.metadata.FunctionParameter;
import org.mule.runtime.core.metadata.DefaultCollectionDataType;
import org.mule.runtime.core.metadata.SimpleDataType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

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
  public void buildFunction() {
    FunctionDataType dataType = (FunctionDataType) DataType.fromFunction(new ExpressionFunction() {

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
    });

    //Return type
    assertThat(dataType.getReturnType().isPresent(), is(true));
    assertThat(dataType.getReturnType().get(), equalTo(STRING));
    //Parameters
    assertThat(dataType.getParameters(), hasSize(2));
    FunctionParameter first = dataType.getParameters().get(0);
    assertThat(first.getName(), is("fst"));
    assertThat(first.getType(), equalTo(NUMBER));
    assertThat(first.getDefaultValueResolver(), nullValue());
    FunctionParameter second = dataType.getParameters().get(1);
    assertThat(second.getName(), is("snd"));
    assertThat(second.getType(), equalTo(OBJECT));
    //Default
    assertThat(second.getDefaultValueResolver().getDefaultValue(builder().build()), is("wow"));
  }

  @Test
  public void buildTypedCollection() {
    final DataType dataType = DataType.builder().collectionType(List.class).itemType(String.class).build();
    assertThat(dataType, instanceOf(DefaultCollectionDataType.class));
    assertThat(dataType.getType(), is(equalTo(List.class)));
    assertThat(((DefaultCollectionDataType) dataType).getItemDataType(), is(STRING));
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
    assertThat(((DefaultCollectionDataType) dataType).getItemDataType(), is(STRING));
  }

  @Test
  public void proxy() {
    final Class<?> muleMessageProxy = Proxy.getProxyClass(DataTypeBuilderTestCase.class.getClassLoader(), Message.class);

    final DataType dataType = DataType.fromType(muleMessageProxy);

    assertThat(dataType.getType(), is(equalTo(Message.class)));
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

    new PollingProber().check(new JUnitLambdaProbe(() -> {
      System.gc();
      assertThat(clRef.isEnqueued(), is(true));
      return true;
    }, "A hard reference is being mantained to the type of the DataType."));
  }
}
