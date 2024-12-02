/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.message.api.el.TypeBindings;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.sdk.api.metadata.MetadataContext;

import java.util.Optional;


/**
 * A {@link MetadataContext} with additional capabilities, for internal use only.
 *
 * @since 4.8.0
 */
public interface InternalMetadataContext extends MetadataContext {

  /**
   * @return the {@link ExpressionLanguageMetadataService}, if available.
   */
  Optional<ExpressionLanguageMetadataService> getExpressionLanguageMetadataService();

  /**
   * Allows for specifying the {@link MetadataType}s of the different bindings provided to the expression language.
   * <p>
   * Examples could be payload, attributes, variables, errors and even module definitions.
   * <p>
   * This, in combination with the {@link ExpressionLanguageMetadataService}, can be used by a type resolver to provide more
   * precise information about the types involved in an expression.
   *
   * @return the {@link TypeBindings} in context, if available.
   */
  Optional<TypeBindings> getTypeBindings();
}
