/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import static java.lang.String.format;
import org.mule.extension.ftp.internal.FtpConnector;
import org.mule.extension.ftp.internal.FtpFilePayload;
import org.mule.extension.ftp.internal.FtpFileSystem;
import org.mule.module.extension.file.api.FilePayload;
import org.mule.module.extension.file.api.command.ListCommand;
import org.mule.util.ArrayUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
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
    public List<FilePayload> list(String directoryPath, boolean recursive, Predicate<FilePayload> matcher)
    {
        FilePayload filePayload = getExistingFile(directoryPath);
        Path path = Paths.get(filePayload.getPath());

        if (!filePayload.isDirectory())
        {
            throw cannotListFileException(path);
        }

        if (!tryChangeWorkingDirectory(path.toString()))
        {
            throw exception(format("Could not change working directory to '%s' while trying to list that directory", path));
        }

        List<FilePayload> accumulator = new LinkedList<>();
        try
        {
            doList(path, accumulator, recursive, matcher);

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

        return accumulator;
    }

    private void doList(Path path, List<FilePayload> accumulator, boolean recursive, Predicate<FilePayload> matcher) throws IOException
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
                FilePayload payload = new FtpFilePayload(path.resolve(file.getName()), file, config);

                if (isVirtualDirectory(payload.getName()) || !matcher.test(payload))
                {
                    continue;
                }

                accumulator.add(payload);

                if (payload.isDirectory() && recursive)
                {
                    Path recursionPath = path.resolve(payload.getName());
                    if (!client.changeWorkingDirectory(payload.getName()))
                    {
                        throw exception(format("Could not change working directory to '%s' while performing recursion on list operation", recursionPath));
                    }
                    doList(recursionPath, accumulator, recursive, matcher);
                    if (!client.changeToParentDirectory())
                    {
                        throw exception(format("Could not return to parent working directory '%s' while performing recursion on list operation", recursionPath.getParent()));
                    }
                }
            }
        }
    }
}
