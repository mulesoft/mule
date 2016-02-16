/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import static java.lang.String.format;
import org.mule.extension.ftp.internal.FtpConnector;
import org.mule.extension.ftp.internal.FtpFileAttributes;
import org.mule.extension.ftp.internal.FtpFileSystem;
import org.mule.module.extension.file.api.FileSystem;
import org.mule.module.extension.file.api.command.FileCommand;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

/**
 * Base class for implementations of {@link FileCommand} which operate
 * on a FTP server
 *
 * @since 4.0
 */
abstract class FtpCommand extends FileCommand<FtpConnector, FtpFileSystem>
{

    protected final FTPClient client;

    /**
     * Creates a new instance
     *
     * @param fileSystem the {@link FileSystem} on which the operation is performed
     * @param config     the config which configures the operation
     * @param client     a ready to use {@link FTPClient} to perform the operations
     */
    FtpCommand(FtpFileSystem fileSystem, FtpConnector config, FTPClient client)
    {
        super(fileSystem, config);
        this.client = client;
    }

    /**
     * Returns a {@link Path} which composes the {@link #client}
     * current working directory with the given {@code filePath}
     *
     * @param filePath the filePath to a file or directory
     * @return an absolute {@link Path}
     */
    @Override
    protected Path resolvePath(String filePath)
    {
        String cwd;
        try
        {
            cwd = client.printWorkingDirectory();
        }
        catch (Exception e)
        {
            throw exception("Failed to determine current working directory");
        }

        return Paths.get(cwd).resolve(filePath);
    }

    /**
     * Similar to {@link #getFile(String)} but throwing
     * an {@link IllegalArgumentException} if the {@code filePath}
     * doesn't exists
     *
     * @param filePath the path to the file you want
     * @return a {@link FtpFileAttributes}
     * @throws IllegalArgumentException if the {@code filePath} doesn't exists
     */
    protected FtpFileAttributes getExistingFile(String filePath)
    {
        return getFile(filePath, true);
    }

    /**
     * Obtains a {@link FtpFileAttributes} for the given {@code filePath}
     * by using the {@link FTPClient#mlistFile(String)} FTP command
     *
     * @param filePath the path to the file you want
     * @return a {@link FtpFileAttributes} or {@code null} if it doesn't exists
     */
    protected FtpFileAttributes getFile(String filePath)
    {
        return getFile(filePath, false);
    }

    private FtpFileAttributes getFile(String filePath, boolean requireExistence)
    {
        Path path = resolvePath(filePath);
        FTPFile ftpFile;
        try
        {
            ftpFile = client.mlistFile(filePath);
        }
        catch (Exception e)
        {
            throw exception("Found exception trying to obtain path " + path, e);
        }

        if (ftpFile != null)
        {
            return new FtpFileAttributes(path, ftpFile);
        }
        else
        {
            if (requireExistence)
            {
                throw pathNotFoundException(path);
            }
            else
            {
                return null;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected boolean exists(Path path)
    {
        return getFile(path.toString()) != null;
    }

    /**
     * Creates the directory pointed by {@code directoryPath} also creating
     * any missing parent directories
     *
     * @param directoryPath the {@link Path} to the directory you want to create
     */
    protected void mkdirs(Path directoryPath)
    {
        String cwd = getCurrentWorkingDirectory();
        try
        {
            changeWorkingDirectory("/");
            for (int i = 0; i < directoryPath.getNameCount(); i++)
            {
                String fragment = directoryPath.getName(i).toString();
                if (!tryChangeWorkingDirectory(fragment))
                {
                    makeDirectory(fragment);
                    changeWorkingDirectory(fragment);
                }
            }
        }
        catch (Exception e)
        {
            throw exception("Found exception trying to recursively create directory " + directoryPath, e);
        }
        finally
        {
            changeWorkingDirectory(cwd);
        }
    }

    /**
     * Attempts to change the current working directory of the
     * FTP {@link #client}. If it was not possible
     * (for example, because it doesn't exists), it returns
     * {@code false}
     *
     * @param path the path to which you wish to move
     * @return {@code true} if the CWD was changed. {@code false} otherwise
     */
    protected boolean tryChangeWorkingDirectory(String path)
    {
        try
        {
            return client.changeWorkingDirectory(path);
        }
        catch (IOException e)
        {
            throw exception("Exception was found while trying to change working directory to " + path, e);
        }
    }

    /**
     * Changes the FTP {@link #client} current working directory
     * to the given {@code path}
     *
     * @param path the {@link Path} to which you wish to move
     * @throws IllegalArgumentException if the CWD could not be changed
     */
    protected void changeWorkingDirectory(Path path)
    {
        changeWorkingDirectory(path.toString());
    }

    /**
     * Changes the FTP {@link #client} current working directory
     * to the given {@code path}
     *
     * @param path the path to which you wish to move
     * @throws IllegalArgumentException if the CWD could not be changed
     */
    protected void changeWorkingDirectory(String path)
    {
        if (!tryChangeWorkingDirectory(path))
        {
            throw new IllegalArgumentException(format("Could not change working directory to '%s'. Path doesn't exists or is not a directory", path.toString()));
        }
    }

    /**
     * Creates the directory of the given {@code directoryName} in the
     * current working directory
     *
     * @param directoryName the name of the directory you want to create
     */
    protected void makeDirectory(String directoryName)
    {
        try
        {
            if (!client.makeDirectory(directoryName))
            {
                throw exception("Failed to create directory " + directoryName);
            }
        }
        catch (Exception e)
        {
            throw exception("Exception was found trying to create directory " + directoryName, e);
        }
    }

    /**
     * @return the {@link #client}'s working directory
     */
    protected String getCurrentWorkingDirectory()
    {
        try
        {
            return client.printWorkingDirectory();
        }
        catch (Exception e)
        {
            throw exception("Could not obtain CWD", e);
        }
    }

    /**
     * @param fileName the name of a file
     * @return {@code true} if {@code fileName} equals to &quot;.&quot; or &quot;..&quot;
     */
    protected boolean isVirtualDirectory(String fileName)
    {
        return ".".equals(fileName) || "..".equals(fileName);
    }

    /**
     * {@inheritDoc}
     * Same as the super method but adding the FTP rely code
     */
    @Override
    protected RuntimeException exception(String message)
    {
        return super.exception(enrichExceptionMessage(message));
    }

    /**
     * {@inheritDoc}
     * Same as the super method but adding the FTP rely code
     */
    @Override
    protected RuntimeException exception(String message, Exception cause)
    {
        return super.exception(enrichExceptionMessage(message), cause);
    }

    private String enrichExceptionMessage(String message)
    {
        return String.format("%s. Ftp reply code: %d", message, client.getReplyCode());
    }
}
