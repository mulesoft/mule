/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.message;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.message.Error;

import java.util.List;

/**
 * Extends the {@link Error} interface, adding privileged methods.
 *
 * @since 4.4
 */
@NoImplement
public interface PrivilegedError extends Error {

  /**
   * List of errors that where identified as underlying causes of this error. For instance, the until-successful scope may throw a
   * retry exhausted error including the error that caused the exhaustion as a suppressed error.
   *
   * Can be an empty list if no underlying causes exist.
   *
   * @return {@link List <Error>} with the underlying causes.
   * @since 4.4
   */
  List<Error> getSuppressedErrors();

}
