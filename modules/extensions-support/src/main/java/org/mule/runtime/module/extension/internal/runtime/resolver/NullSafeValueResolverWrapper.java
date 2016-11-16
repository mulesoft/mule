/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.mule.metadata.internal.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.util.ValueHolder;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultResolverSetBasedObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ObjectBuilder;

/**
 * A {@link ValueResolver} wrapper which generates and returns default instances
 * if the {@link #delegate} returns {@code null}.
 * <p>
 * The values are generated according to the rules described in {@link NullSafe}.
 * <p>
 * Instances are to be obtained through the {@link #of(ValueResolver, ParameterizedModel, ParameterModel, MuleContext)}
 * factory method
 *
 * @param <T> the generic type of the produced values.
 * @since 4.0
 */
public class NullSafeValueResolverWrapper<T> implements ValueResolver<T> {

  private final ValueResolver<T> delegate;
  private final ValueResolver<T> fallback;

  /**
   * Creates a new instance
   *
   * @param delegate           the {@link ValueResolver} to wrap
   * @param parameterizedModel the model of the group which owns the {@code parameter}
   * @param parameterModel     the model of the null safe parameter
   * @param muleContext        the current {@link MuleContext}
   * @param <T>                the generic type of the produced values
   * @return a new null safe {@link ValueResolver}
   * @throws IllegalParameterModelDefinitionException if used on parameters of not supported types
   */
  public static <T> ValueResolver<T> of(ValueResolver<T> delegate,
                                        ParameterizedModel parameterizedModel,
                                        ParameterModel parameterModel,
                                        MuleContext muleContext) {
    checkArgument(delegate != null, "delegate cannot be null");

    ValueHolder<ValueResolver> value = new ValueHolder<>();
    parameterModel.getType().accept(new MetadataTypeVisitor() {

      @Override
      public void visitObject(ObjectType objectType) {
        Class<?> clazz = getType(objectType);
        ResolverSet resolverSet = new ResolverSet();

        objectType.getFields().forEach(field -> {
          final Class<?> fieldClass = getType(field.getValue());

          if (field.isRequired()) {
            throw new IllegalParameterModelDefinitionException(
                                                               format("Class '%s' cannot be used with '@%s' parameter since it contains non optional fields",
                                                                      clazz.getName(), NullSafe.class.getSimpleName()));
          }

          getDefaultValue(field).ifPresent(defaultValue -> resolverSet.add(field.getKey().getName().getLocalPart(),
                                                                           new TypeSafeExpressionValueResolver(defaultValue,
                                                                                                               fieldClass,
                                                                                                               muleContext)));

        });

        ObjectBuilder<T> objectBuilder =
            new DefaultResolverSetBasedObjectBuilder(clazz, parameterizedModel, resolverSet);

        value.set(new ObjectBuilderValueResolver(objectBuilder));
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        Class collectionClass = getType(arrayType);
        value.set(CollectionValueResolver.of(collectionClass, emptyList()));
      }

      @Override
      public void visitDictionary(DictionaryType dictionaryType) {
        Class mapClass = getType(dictionaryType);
        value.set(MapValueResolver.of(mapClass, emptyList(), emptyList()));
      }

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        throw new IllegalParameterModelDefinitionException(
                                                           format("Cannot use @%s on type '%s'", NullSafe.class.getSimpleName(),
                                                                  getType(metadataType)));
      }
    });

    return new NullSafeValueResolverWrapper<>(delegate, value.get());
  }

  private NullSafeValueResolverWrapper(ValueResolver<T> delegate, ValueResolver<T> fallback) {
    this.delegate = delegate;
    this.fallback = fallback;
  }

  @Override
  public T resolve(Event event) throws MuleException {
    T value = delegate.resolve(event);

    if (value == null) {
      value = fallback.resolve(event);
    }

    return value;
  }

  @Override
  public boolean isDynamic() {
    return delegate.isDynamic();
  }
}
