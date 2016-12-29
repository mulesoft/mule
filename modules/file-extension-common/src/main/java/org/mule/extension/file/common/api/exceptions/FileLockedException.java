/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.common.api.exceptions;

import static org.mule.extension.file.common.api.exceptions.FileErrors.*;
import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * @since 4.0
 */
public final class FileLockedException extends ModuleException {

  public FileLockedException(String message) {
    super(message, CONCURRENCY);
  }
}

