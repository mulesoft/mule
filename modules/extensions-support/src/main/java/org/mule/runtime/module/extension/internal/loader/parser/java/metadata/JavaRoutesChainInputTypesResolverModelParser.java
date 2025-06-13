/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.metadata;

import static org.mule.runtime.module.extension.internal.loader.utils.JavaInputResolverModelParserUtils.getChainInputTypeResolver;
import static org.mule.runtime.module.extension.internal.metadata.chain.NullChainInputTypeResolver.NULL_INSTANCE;

import static java.util.stream.Collectors.toMap;

import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.extension.api.loader.parser.metadata.RoutesChainInputTypesResolverModelParser;
import org.mule.sdk.api.metadata.resolving.ChainInputTypeResolver;

import java.util.List;
import java.util.Map;

/**
 * {@link RoutesChainInputTypesResolverModelParser} implementation for the Java SDK
 *
 * @since 4.7.0
 */
public class JavaRoutesChainInputTypesResolverModelParser implements RoutesChainInputTypesResolverModelParser {

  private final List<ExtensionParameter> routes;

  public JavaRoutesChainInputTypesResolverModelParser(List<ExtensionParameter> routes) {
    this.routes = routes;
  }

  @Override
  public Map<String, ChainInputTypeResolver> getRoutesChainInputResolvers() {
    return routes.stream().collect(toMap(
                                         ExtensionParameter::getAlias,
                                         route -> getChainInputTypeResolver(route).orElse(NULL_INSTANCE)));
  }
}
