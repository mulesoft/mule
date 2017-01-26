/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.exception.connection;

import org.mule.extension.db.internal.DbConnector;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;

import java.util.Optional;

/**
 * Errors definitions for {@link DbConnector}
 *
 * @since 4.0
 */
public enum DbError implements ErrorTypeDefinition<DbError> {
  CONNECTIVITY(MuleErrors.CONNECTIVITY), INVALID_CREDENTIALS(CONNECTIVITY), INVALID_DATABASE(CONNECTIVITY), CANNOT_REACH(
      CONNECTIVITY);

  private ErrorTypeDefinition<? extends Enum<?>> parent;

  DbError(ErrorTypeDefinition<? extends Enum<?>> parent) {
    this.parent = parent;
  }

  DbError() {}


  @Override
  public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
    return Optional.ofNullable(parent);
  }
}
