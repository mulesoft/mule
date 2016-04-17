/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal;

import org.mule.runtime.api.temporary.MuleMessage;
import org.mule.extension.api.annotation.param.Connection;
import org.mule.extension.api.annotation.param.Optional;
import org.mule.runtime.module.extension.file.api.FileAttributes;
import org.mule.runtime.module.extension.file.api.FilePredicateBuilder;
import org.mule.runtime.module.extension.file.api.FileSystem;
import org.mule.runtime.module.extension.file.api.TreeNode;
import org.mule.runtime.module.extension.file.api.matcher.NullFilePayloadPredicate;

import java.util.function.Predicate;

/**
 * Contains an operation which allows to list files using an optional {@link LocalFilePredicateBuilder}
 *
 * @since 4.0
 */
public class FileListOperation
{

    /**
     * Lists all the files in the {@code directoryPath} which match the given {@code matcher}.
     * <p>
     * If the listing encounters a directory, the output list will include its contents depending
     * on the value of the {@code recursive} parameter.
     * <p>
     * If {@code recursive} is set to {@code true} but a found directory is rejected by the
     * {@code matcher}, then there won't be any recursion into such directory.
     *
     * @param directoryPath the path to the directory to be listed
     * @param recursive     whether to include the contents of sub-directories. Defaults to {@code false}
     * @param message       the {@link MuleMessage} on which this operation was triggered
     * @param matcher       a matcher used to filter the output list
     * @return a {@link TreeNode} object representing the listed directory
     * @throws IllegalArgumentException if {@code directoryPath} points to a file which doesn't exists or is not a directory
     */
    public TreeNode list(@Connection FileSystem fileSystem,
                         @Optional String directoryPath,
                         @Optional(defaultValue = "false") boolean recursive,
                         MuleMessage<?, ?> message,
                         @Optional LocalFilePredicateBuilder matcher)
    {
        return fileSystem.list(directoryPath, recursive, message, getPredicate(matcher));
    }

    private Predicate<FileAttributes> getPredicate(FilePredicateBuilder builder)
    {
        return builder != null ? builder.build() : new NullFilePayloadPredicate();
    }
}
