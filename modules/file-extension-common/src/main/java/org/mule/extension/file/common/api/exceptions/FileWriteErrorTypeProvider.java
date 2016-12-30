/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.common.api.exceptions;

import static org.mule.extension.file.common.api.exceptions.FileErrors.ACCESS_DENIED;
import static org.mule.extension.file.common.api.exceptions.FileErrors.FILE_ALREADY_EXISTS;
import static org.mule.extension.file.common.api.exceptions.FileErrors.ILLEGAL_CONTENT;
import static org.mule.extension.file.common.api.exceptions.FileErrors.ILLEGAL_PATH;
import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.FileSystem;
import org.mule.extension.file.common.api.FileWriteMode;
import org.mule.extension.file.common.api.StandardFileSystemOperations;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Errors that can be thrown in the
 * {@link StandardFileSystemOperations#write(FileConnectorConfig, FileSystem, String, Object, FileWriteMode, boolean, boolean, String, Event)}
 * operation.
 *
 * @since 1.0
 */
public class FileWriteErrorTypeProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return ImmutableSet.<ErrorTypeDefinition>builder()
        .add(ILLEGAL_PATH)
        .add(ILLEGAL_CONTENT)
        .add(FILE_ALREADY_EXISTS)
        .add(ACCESS_DENIED)
        .build();
  }
}

