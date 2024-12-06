/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.metadata;

import static java.util.Optional.empty;

import org.mule.api.annotation.NoImplement;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.MetadataResolvingException;

import java.util.Optional;

/**
 * Resolves the propagated parameter type by name.
 *
 * @since 4.8.2
 */
@NoImplement
public interface PropagatedParameterTypeResolver {

  /**
   * An implementation that never returns any propagation information.
   */
  PropagatedParameterTypeResolver NO_OP = parameterName -> empty();

  /**
   * @param parameterName The parameter name.
   * @return The propagated parameter {@link MetadataType} if corresponds.
   * @throws MetadataResolvingException If there is an issue when computing the type from the current bindings in context.
   */
  Optional<MetadataType> getResolvedType(String parameterName) throws MetadataResolvingException;
}
