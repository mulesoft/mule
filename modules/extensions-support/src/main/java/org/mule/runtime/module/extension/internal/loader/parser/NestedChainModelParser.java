/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.nested.NestedChainModel;

/**
 * Parses the syntactic definition of a {@link NestedChainModel} so that the semantics reflected in it can be extracted in a
 * uniform way, regardless of the actual syntax used by the extension developer.
 *
 * @see ExtensionModelParser
 * @since 4.5.0
 */
public interface NestedChainModelParser extends SemanticTermsParser, AllowedStereotypesModelParser {

  /**
   * @return the chain's name
   */
  String getName();

  /**
   * @return the chain's description
   */
  String getDescription();

  /**
   * @return whether the chain is required or not
   */
  boolean isRequired();
}
