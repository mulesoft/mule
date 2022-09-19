/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.error.ErrorModel;

import java.util.Optional;

/**
 * Parses the syntactic definition of a {@link ErrorModel} so that the semantics reflected in it can be extracted in a uniform
 * way, regardless of the actual syntax used by the extension developer.
 *
 * @see ExtensionModelParser
 * @since 4.5.0
 */
public interface ErrorModelParser {

  /**
   * @return the error's type
   */
  String getType();

  /**
   * @return the error's namespace
   */
  String getNamespace();

  /**
   * @return {@code true} if it represents to a core Mule language error.
   */
  boolean isMuleError();

  /**
   * @return the error's parent definition
   */
  Optional<ErrorModelParser> getParent();

  /**
   * @return {@code true} if the parsed error should be suppressed from the operation model.
   */
  default boolean isSuppressed() {
    return false;
  }
}
