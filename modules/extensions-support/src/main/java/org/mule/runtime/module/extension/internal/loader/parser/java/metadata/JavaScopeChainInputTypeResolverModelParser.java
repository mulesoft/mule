/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.metadata;

import static org.mule.runtime.module.extension.internal.metadata.chain.NullChainInputTypeResolver.NULL_INSTANCE;

import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.extension.api.loader.parser.metadata.ScopeChainInputTypeResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.utils.JavaInputResolverModelParserUtils;
import org.mule.sdk.api.metadata.resolving.ChainInputTypeResolver;

/**
 * {@link ScopeChainInputTypeResolverModelParser} implementation for the Java SDK
 *
 * @since 4.7.0
 */
public class JavaScopeChainInputTypeResolverModelParser implements ScopeChainInputTypeResolverModelParser {

  private final ExtensionParameter chain;

  public JavaScopeChainInputTypeResolverModelParser(ExtensionParameter chain) {
    this.chain = chain;
  }

  @Override
  public ChainInputTypeResolver getChainInputTypeResolver() {
    return JavaInputResolverModelParserUtils.getChainInputTypeResolver(chain).orElse(NULL_INSTANCE);
  }
}
