/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api;

import org.mule.extension.file.common.api.exceptions.FileError;
import org.mule.extension.ftp.internal.FtpConnector;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * {@link ConnectionException} implementation to declare connectivity errors in the {@link FtpConnector}
 *
 * @since 4.0
 */
public class FTPConnectionException extends ConnectionException {

  public FTPConnectionException(String s) {
    super(s);
  }

  public FTPConnectionException(String message, FileError errors) {
    super(message, new ModuleException(null, errors));
  }

  public FTPConnectionException(Throwable throwable, FileError fileError) {
    super(new ModuleException(throwable, fileError));
  }

  public FTPConnectionException(String message, Throwable throwable, FileError fileError) {
    super(message, new ModuleException(throwable, fileError));
  }
}
