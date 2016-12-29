/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.common.api.exceptions;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

/**
 * Errors for the file family extensions
 * 
 * @since 4.0
 */
public enum FileErrors implements ErrorTypeDefinition<FileErrors> {

  FILE_NOT_FOUND, ILLEGAL_PATH, ILLEGAL_CONTENT, CONCURRENCY, FILE_ALREADY_EXISTS, ACCESS_DENIED
}
