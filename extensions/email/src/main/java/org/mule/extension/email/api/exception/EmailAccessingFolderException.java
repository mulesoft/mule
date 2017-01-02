/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.exception;

import static org.mule.extension.email.api.exception.EmailErrors.ACCESSING_FOLDER;
import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * {@link ModuleException} for the cases in which there was a problem accessing an email folder
 * 
 * @since 4.0
 */
public class EmailAccessingFolderException extends ModuleException {

  public EmailAccessingFolderException(String message, Exception exception) {
    super(exception, ACCESSING_FOLDER, message);
  }

}
