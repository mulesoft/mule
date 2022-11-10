/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.parameterization.value;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.mule.runtime.api.parameterization.value.ArrayValueDeclarer;
import org.mule.runtime.api.parameterization.value.ObjectValueDeclarer;
import org.mule.runtime.api.parameterization.value.ValueDeclarer;
import org.mule.runtime.api.util.Pair;

public class DefaultValueDeclarer implements ValueDeclarer {

  private HasValue value;
  private Pair<String, String> typeInformation = null;

  @Override
  public void objectValue(Consumer<ObjectValueDeclarer> objectValueDeclarerConsumer) {
    value = new DefaultObjectValueDeclarer();
    objectValueDeclarerConsumer.accept((ObjectValueDeclarer) value);
  }

  @Override
  public void objectValue(Consumer<ObjectValueDeclarer> objectValueDeclarerConsumer, String typeIdentifier) {
    value = new TypedObjectValueDeclarer(typeIdentifier);
    objectValueDeclarerConsumer.accept((ObjectValueDeclarer) value);
  }

  @Override
  public void arrayValue(Consumer<ArrayValueDeclarer> arrayValueDeclarerConsumer) {
    value = new DefaultArrayValueDeclarer();
    arrayValueDeclarerConsumer.accept((ArrayValueDeclarer) value);
  }

  @Override
  public void withValue(Object value) {
    this.value = new SimpleHasValue(value);
  }

  public Object getValue() {
    return value.getValue();
  }

  private static Object getValue(Consumer<ValueDeclarer> valueDeclarerConsumer) {
    DefaultValueDeclarer valueDeclarer = new DefaultValueDeclarer();
    valueDeclarerConsumer.accept(valueDeclarer);
    return valueDeclarer.getValue();
  }

  private interface HasValue {

    Object getValue();
  }

  private static class DefaultObjectValueDeclarer implements ObjectValueDeclarer, HasValue {

    private final Map<String, Object> mapValue;

    public DefaultObjectValueDeclarer() {
      mapValue = new LinkedHashMap();
    }

    @Override
    public ObjectValueDeclarer withField(String name, Object value) {
      mapValue.put(name, value);
      return this;
    }

    @Override
    public ObjectValueDeclarer withField(String name, Consumer<ValueDeclarer> valueDeclarerConsumer) {
      mapValue.put(name, DefaultValueDeclarer.getValue(valueDeclarerConsumer));
      return this;
    }

    @Override
    public Object getValue() {
      return mapValue;
    }
  }

  private static class TypedObjectValueDeclarer extends DefaultObjectValueDeclarer {

    private final String typeIdentifier;

    public TypedObjectValueDeclarer(String typeIdentifier) {
      super();
      this.typeIdentifier = typeIdentifier;
    }

    @Override
    public Object getValue() {
      return new EnrichedValue(super.getValue(), typeIdentifier);
    }

  }

  private static class DefaultArrayValueDeclarer implements ArrayValueDeclarer, HasValue {

    private final List<Object> listValue;

    public DefaultArrayValueDeclarer() {
      listValue = new ArrayList<>();
    }

    @Override
    public ArrayValueDeclarer withItem(Object value) {
      listValue.add(value);
      return this;
    }

    @Override
    public ArrayValueDeclarer withItem(Consumer<ValueDeclarer> valueDeclarerConsumer) {
      listValue.add(DefaultValueDeclarer.getValue(valueDeclarerConsumer));
      return this;
    }

    @Override
    public Object getValue() {
      return listValue;
    }
  }

  private static class SimpleHasValue implements HasValue {

    private final Object value;

    public SimpleHasValue(Object value) {
      this.value = value;
    }

    @Override
    public Object getValue() {
      return value;
    }
  }

}
