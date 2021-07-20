/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.api.extension;

import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.PRIMITIVE_TYPES;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import java.util.Set;

/**
 * Open {@link ValueProvider} which returns the keys for the primitive types.
 * <p>
 * This is needed because XML SDK can also point to types defined in the application's catalog.
 *
 * @since 4.4.0
 */
public class XmlSdkTypesValueProvider implements ValueProvider {

  static final String ID = "XmlSdkTypesValueProvider";

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return ValueBuilder.getValuesFor(PRIMITIVE_TYPES.keySet().stream());
  }

  @Override
  public String getId() {
    return ID;
  }
}
