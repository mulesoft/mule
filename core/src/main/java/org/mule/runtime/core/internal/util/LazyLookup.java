/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;

/**
 * {@link LazyValue} specialization which lookups and object of a certain type in the mule registry
 *
 * @param <T> the generic type of the object to look for
 * @since 4.0
 */
public class LazyLookup<T> extends LazyValue<T> {

  public LazyLookup(Class<T> type, MuleContext muleContext) {
    super(() -> {
      try {
        return ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(type);
      } catch (Exception e) {
        throw new MuleRuntimeException(createStaticMessage("Could not fetch dependency of type " + type.getName()), e);
      }
    });
  }
}
