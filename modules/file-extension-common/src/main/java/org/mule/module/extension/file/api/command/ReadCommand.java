/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.file.api.command;

import org.mule.extension.api.runtime.ContentMetadata;
import org.mule.module.extension.file.api.FilePayload;
import org.mule.module.extension.file.api.FileSystem;

/**
 * Command design pattern for reading files
 *
 * @since 4.0
 */
public interface ReadCommand
{

    /**
     * Reads files under the considerations of {@link FileSystem#read(String, boolean, ContentMetadata)}
     *
     * @param filePath        the path of the file you want to read
     * @param lock            whether or not to lock the file
     * @param contentMetadata a {@link ContentMetadata} to pass mimeType information of the file
     * @return the file's content and metadata on a {@link FilePayload} instance
     * @throws IllegalArgumentException if the file at the given path doesn't exists
     */
    FilePayload read(String filePath, boolean lock, ContentMetadata contentMetadata);
}
