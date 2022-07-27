/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.component.value;

import static java.util.Collections.emptySet;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.component.value.AbstractValueDeclarerFactory;
import org.mule.runtime.extension.api.component.value.ArrayValueDeclarer;
import org.mule.runtime.extension.api.component.value.ValueDeclarer;
import org.mule.runtime.module.extension.internal.runtime.resolver.CollectionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.resolver.ValueResolverFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class DefaultArrayValueDeclarer implements ArrayValueDeclarer, HasValue {

  private final ArrayType type;
  private final String parameterName;
  private final Set<ModelProperty> modelProperties;
  private final List<ValueResolver> valueResolvers;

  public DefaultArrayValueDeclarer(ArrayType type, String parameterName) {
    this(type, parameterName, emptySet());
  }

  public DefaultArrayValueDeclarer(ArrayType type, String parameterName, Set<ModelProperty> modelProperties) {
    this.type = type;
    this.parameterName = parameterName;
    this.modelProperties = modelProperties;
    this.valueResolvers = new ArrayList<>();
  }

  @Override
  public ArrayValueDeclarer withItem(Object value) {
    valueResolvers.add(new StaticValueResolver(value));
    return this;
  }

  @Override
  public ArrayValueDeclarer withItem(Consumer<ValueDeclarer> valueDeclarerConsumer) {
    MetadataType itemType = type.getType();
    ValueDeclarer valueDeclarer = new DefaultValueDeclarer(itemType, parameterName, emptySet());
    valueDeclarerConsumer.accept(valueDeclarer);
    Object value = valueDeclarer.getValue();
    valueResolvers.add(value instanceof ValueResolver ? (ValueResolver) value : new StaticValueResolver(value));
    return this;
  }

  @Override
  public Object getValue() {
    CollectionValueResolver collectionValueResolver = new CollectionValueResolver()
  }
}
