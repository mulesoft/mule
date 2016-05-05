/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import static java.lang.String.format;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.extension.ftp.api.FtpConnector;
import org.mule.extension.ftp.api.FtpFileAttributes;
import org.mule.extension.ftp.api.FtpFileSystem;
import org.mule.runtime.module.extension.file.api.FileAttributes;
import org.mule.runtime.module.extension.file.api.TreeNode;
import org.mule.runtime.module.extension.file.api.command.ListCommand;
import org.mule.runtime.core.util.ArrayUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPListParseEngine;
import org.apache.commons.net.ftp.FTPReply;

/**
 * A {@link FtpCommand} which implements the {@link ListCommand} contract
 *
 * @since 4.0
 */
public final class FtpListCommand extends FtpCommand implements ListCommand
{

    private static final int FTP_LIST_PAGE_SIZE = 25;

    /**
     * {@inheritDoc}
     */
    public FtpListCommand(FtpFileSystem fileSystem, FtpConnector config, FTPClient client)
    {
        super(fileSystem, config, client);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeNode list(String directoryPath, boolean recursive, MuleMessage<?, ?> message, Predicate<FileAttributes> matcher)
    {
        FileAttributes fileAttributes = getExistingFile(directoryPath);
        Path path = Paths.get(fileAttributes.getPath());

        if (!fileAttributes.isDirectory())
        {
            throw cannotListFileException(path);
        }

        if (!tryChangeWorkingDirectory(path.toString()))
        {
            throw exception(format("Could not change working directory to '%s' while trying to list that directory", path));
        }

        TreeNode.Builder treeNodeBuilder = TreeNode.Builder.forDirectory(fileAttributes);
        try
        {
            doList(path, treeNodeBuilder, recursive, message, matcher);

            if (!FTPReply.isPositiveCompletion(client.getReplyCode()))
            {
                throw exception(format("Failed to list files on directory '%s'", path));
            }

            changeWorkingDirectory(path);
        }
        catch (Exception e)
        {
            throw exception(format("Failed to list files on directory '%s'", path), e);
        }

        return treeNodeBuilder.build();
    }

    private void doList(Path path, TreeNode.Builder treeNodeBuilder, boolean recursive, MuleMessage message, Predicate<FileAttributes> matcher) throws IOException
    {
        FTPListParseEngine engine = client.initiateListParsing();
        while (engine.hasNext())
        {
            FTPFile[] files = engine.getNext(FTP_LIST_PAGE_SIZE);
            if (ArrayUtils.isEmpty(files))
            {
                return;
            }

            for (FTPFile file : files)
            {
                final Path filePath = path.resolve(file.getName());
                FileAttributes attributes = new FtpFileAttributes(filePath, file);

                if (isVirtualDirectory(attributes.getName()) || !matcher.test(attributes))
                {
                    continue;
                }

                if (attributes.isDirectory())
                {
                    TreeNode.Builder childNodeBuilder = TreeNode.Builder.forDirectory(attributes);
                    treeNodeBuilder.addChild(childNodeBuilder);

                    if (recursive)
                    {
                        Path recursionPath = path.resolve(attributes.getName());
                        if (!client.changeWorkingDirectory(attributes.getName()))
                        {
                            throw exception(format("Could not change working directory to '%s' while performing recursion on list operation", recursionPath));
                        }
                        doList(recursionPath, childNodeBuilder, recursive, message, matcher);
                        if (!client.changeToParentDirectory())
                        {
                            throw exception(format("Could not return to parent working directory '%s' while performing recursion on list operation", recursionPath.getParent()));
                        }
                    }
                }
                else
                {
                    treeNodeBuilder.addChild(TreeNode.Builder.forFile(fileSystem.read(message, filePath.toString(), false)));
                }
            }
        }
    }
}
