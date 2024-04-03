/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.metadata;

import org.mule.sdk.api.metadata.resolving.ChainInputTypeResolver;

import java.util.Map;


/**
 * Parses the syntactic definition of a router to obtain its routes {@link ChainInputTypeResolver}
 *
 * @since 4.7.0
 */
public interface RoutesChainInputTypesResolverModelParser {

  /**
   * Returns the {@link ChainInputTypeResolver} through a {@link Map} which keys match the route names.
   * Routes that don't specify a resolver will be assigned a default {@code Null-Object} resolver.
   *
   * @return a non {@code null} {@link Map}
   */
  Map<String, ChainInputTypeResolver> getRoutesChainInputResolvers();
}
