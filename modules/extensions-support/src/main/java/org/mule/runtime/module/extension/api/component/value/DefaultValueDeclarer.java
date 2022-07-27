/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.component.value;

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.function.UnaryOperator.identity;
import static org.mule.metadata.api.utils.MetadataTypeUtils.checkArgument;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getExpressionSupport;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.core.internal.event.NullEventFactory;
import org.mule.runtime.extension.api.component.value.ArrayValueDeclarer;
import org.mule.runtime.extension.api.component.value.MapValueDeclarer;
import org.mule.runtime.extension.api.component.value.ObjectValueDeclarer;
import org.mule.runtime.extension.api.component.value.ValueDeclarer;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.resolver.ValueResolverFactory;

import java.util.Set;

public class DefaultValueDeclarer implements ValueDeclarer, HasValue {

  private final MetadataType type;
  private final String parameterName;
  private final Set<ModelProperty> modelProperties;
  private Either<Object, HasValue> value;
  private final ValueResolverFactory valueResolverFactory;

  public DefaultValueDeclarer(MetadataType type, String parameterName) {
    this(type, parameterName, emptySet());
  }

  public DefaultValueDeclarer(MetadataType type, String parameterName, Set<ModelProperty> modelProperties) {
    this.type = type;
    this.parameterName = parameterName;
    this.modelProperties = modelProperties;
    this.valueResolverFactory = new ValueResolverFactory();
  }

  @Override
  public MapValueDeclarer asMapValue() {
    return new DefaultMapValueDeclarer();
  }

  @Override
  public ObjectValueDeclarer asObjectValue() {
    return new DefaultObjectValueDeclarer();
  }

  @Override
  public ArrayValueDeclarer asArrayValue() {
    checkArgument(type instanceof ArrayType, "Value static type must be an ArrayType to be declared as an array.");
    ArrayValueDeclarer arrayValueDeclarer = new DefaultArrayValueDeclarer((ArrayType) type, parameterName, modelProperties);
    value = left(arrayValueDeclarer);
    return arrayValueDeclarer;
  }

  @Override
  public void withValue(Object value) {
    try {
      this.value = left(getStaticValue(value));
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public Object getValue() {
    requireNonNull(value, "A value must be set first.");
    return value.reduce(identity(), HasValue::getValue);
  }

  private Object getStaticValue(Object value) throws MuleException {
    ValueResolver valueResolver = valueResolverFactory.of(parameterName, type, value, getDefaultValue(type),
                                                          getExpressionSupport(type), false, modelProperties, false);

    return valueResolver.isDynamic() ? valueResolver
        : valueResolver.resolve(ValueResolvingContext.builder(NullEventFactory.getNullEvent()).build());
  }
}
