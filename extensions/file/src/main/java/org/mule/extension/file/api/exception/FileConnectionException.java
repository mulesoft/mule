/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.api.exception;

import org.mule.extension.file.common.api.exceptions.FileError;
import org.mule.extension.file.internal.FileConnector;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * {@link ConnectionException} implementation to communicate errors occurred creating a connection for
 * {@link FileConnector}
 *
 * @since 4.0
 */
public class FileConnectionException extends ConnectionException {

  public FileConnectionException(String s, FileError fileError) {
    super(s, new ModuleException(null, fileError));
  }

  public FileConnectionException(String s, Throwable e) {
    super(s, e);
  }

}
