/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.file.api.command;

import org.mule.api.temporary.MuleMessage;
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
     * Reads files under the considerations of {@link FileSystem#read(MuleMessage, String, boolean)}
     *
     * @param message         the incoming MuleMessage
     * @param filePath        the path of the file you want to read
     * @param lock            whether or not to lock the file
     * @return a MuleMessage with the file's content and metadata on a {@link FilePayload} instance
     * @throws IllegalArgumentException if the file at the given path doesn't exists
     */
    MuleMessage read(MuleMessage message, String filePath, boolean lock);
}
