/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.file.api.command;

import org.mule.module.extension.file.api.FilePayload;
import org.mule.module.extension.file.api.FileSystem;

import java.util.List;
import java.util.function.Predicate;

/**
 * Command design pattern for listing files
 *
 * @since 4.0
 */
public interface ListCommand
{

    /**
     * Lists files under the considerations of {@link FileSystem#list(String, boolean, Predicate)}
     *
     * @param directoryPath the path to the directory to be listed
     * @param recursive     whether to include the contents of sub-directories
     * @param matcher       a {@link Predicate} of {@link FilePayload} used to filter the output list
     * @return a {@link List} of {@link FilePayload}. Might be empty but will never be null
     * @throws IllegalArgumentException if {@code directoryPath} points to a file which doesn't exists or is not a directory
     */
    List<FilePayload> list(String directoryPath, boolean recursive, Predicate<FilePayload> matcher);
}
