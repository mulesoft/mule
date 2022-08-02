/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.sdk.api.metadata.resolving.InputTypeResolver;

/**
 * Parses the syntactic definition of input resolvers so that the semantics reflected in it can be extracted in a uniform way,
 * regardless of the actual syntax used by the extension developer.
 *
 * @see ExtensionModelParser
 * @since 4.5.0
 */
public interface InputResolverModelParser {

  /**
   * @return the name of the parameter associated to the resolver parser by this parser
   */
  String getParameterName();

  /**
   * @return the instance of {@link InputTypeResolver}
   */
  InputTypeResolver<?> getInputResolver();

}
