/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.file.api.command;

import org.mule.api.temporary.MuleMessage;
import org.mule.module.extension.file.api.FileAttributes;
import org.mule.module.extension.file.api.FileSystem;

import java.io.InputStream;

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
     * @param message  the incoming MuleMessage
     * @param filePath the path of the file you want to read
     * @param lock     whether or not to lock the file
     * @return A {@link MuleMessage} with an {@link InputStream} with the file's content as payload
     * and a {@link FileAttributes} object as {@link MuleMessage#getAttributes()}
     * @throws IllegalArgumentException if the file at the given path doesn't exists
     */
    MuleMessage<InputStream, FileAttributes> read(MuleMessage<?, ?> message, String filePath, boolean lock);
}
