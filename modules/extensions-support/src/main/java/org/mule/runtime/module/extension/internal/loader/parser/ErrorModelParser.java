/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
