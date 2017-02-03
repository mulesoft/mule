/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.core.util.ClassUtils.isInstance;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;

/**
 * An extended {@link StaticValueResolver} which always returns the same value but if it is required
 * it will be converted to the required type using the mule transformation service.
 *
 * @param <T> Type of the value to resolver
 * @since 4.0
 */
public class TypeSafeStaticValueResolver<T> extends StaticValueResolver<T> {

  private final Class<T> expectedtype;
  private final DataType expectedDataType;
  private final TypeSafeTransformer typeSafeTransformer;
  private Object value;

  public TypeSafeStaticValueResolver(T originalValue, Class<T> expectedType, MuleContext muleContext) {
    super(originalValue);
    try {
      this.value = super.resolve(null);
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }

    this.typeSafeTransformer = new TypeSafeTransformer(muleContext);
    this.expectedtype = expectedType;
    this.expectedDataType = DataType.fromType(expectedType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T resolve(Event event) throws MuleException {
    if (isInstance(ValueResolver.class, value)) {
      value = ((ValueResolver) value).resolve(event);
    }

    if (isInstance(expectedtype, value)) {
      return (T) value;
    }

    return (T) typeSafeTransformer.transform(value, DataType.fromObject(value), expectedDataType, event);
  }
}
