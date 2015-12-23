/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.file.api.command;

import org.mule.api.MuleEvent;
import org.mule.module.extension.file.api.FileSystem;
import org.mule.module.extension.file.api.FileWriteMode;

/**
 * Command design pattern for writing files
 *
 * @since 4.0
 */
public interface WriteCommand
{

    /**
     * Writes a file under the considerations of {@link FileSystem#write(String, Object, FileWriteMode, MuleEvent, boolean, boolean)}
     *
     * @param filePath              the path of the file to be written
     * @param content               the content to be written into the file
     * @param mode                  a {@link FileWriteMode}
     * @param event                 the {@link MuleEvent} which processing triggers this operation
     * @param lock                  whether or not to lock the file
     * @param createParentDirectory whether or not to attempt creating the parent directory if it doesn't exists.
     * @throws IllegalArgumentException if an illegal combination of arguments is supplied
     */
    void write(String filePath, Object content, FileWriteMode mode, MuleEvent event, boolean lock, boolean createParentDirectory);
}
