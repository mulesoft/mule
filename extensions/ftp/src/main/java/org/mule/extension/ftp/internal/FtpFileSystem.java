/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.extension.ftp.internal.command.FtpCopyCommand;
import org.mule.extension.ftp.internal.command.FtpCreateDirectoryCommand;
import org.mule.extension.ftp.internal.command.FtpDeleteCommand;
import org.mule.extension.ftp.internal.command.FtpListCommand;
import org.mule.extension.ftp.internal.command.FtpMoveCommand;
import org.mule.extension.ftp.internal.command.FtpReadCommand;
import org.mule.extension.ftp.internal.command.FtpRenameCommand;
import org.mule.extension.ftp.internal.command.FtpWriteCommand;
import org.mule.module.extension.file.api.AbstractFileSystem;
import org.mule.module.extension.file.api.PathLock;
import org.mule.module.extension.file.api.URLPathLock;
import org.mule.module.extension.file.api.command.CopyCommand;
import org.mule.module.extension.file.api.command.CreateDirectoryCommand;
import org.mule.module.extension.file.api.command.DeleteCommand;
import org.mule.module.extension.file.api.command.ListCommand;
import org.mule.module.extension.file.api.command.MoveCommand;
import org.mule.module.extension.file.api.command.ReadCommand;
import org.mule.module.extension.file.api.command.RenameCommand;
import org.mule.module.extension.file.api.command.WriteCommand;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link AbstractFileSystem} for files residing on a
 * FTP server
 *
 * @since 4.0
 */
public final class FtpFileSystem extends AbstractFileSystem
{

    private static final Logger LOGGER = LoggerFactory.getLogger(FtpFileSystem.class);

    @Inject
    private MuleContext muleContext;

    private final FtpConnector config;
    private final FTPClient client;
    private final CopyCommand copyCommand;
    private final CreateDirectoryCommand createDirectoryCommand;
    private final DeleteCommand deleteCommand;
    private final ListCommand listCommand;
    private final MoveCommand moveCommand;
    private final ReadCommand readCommand;
    private final RenameCommand renameCommand;
    private final WriteCommand writeCommand;


    /**
     * Creates a new instance
     *
     * @param config the {@link FtpConnector} through which {@code this} instance is used
     * @param client a ready to use {@link FTPClient}
     */
    FtpFileSystem(FtpConnector config, FTPClient client)
    {
        this.config = config;
        this.client = client;

        copyCommand = new FtpCopyCommand(this, config, client);
        createDirectoryCommand = new FtpCreateDirectoryCommand(this, config, client);
        deleteCommand = new FtpDeleteCommand(this, config, client);
        listCommand = new FtpListCommand(this, config, client);
        moveCommand = new FtpMoveCommand(this, config, client);
        readCommand = new FtpReadCommand(this, config, client);
        renameCommand = new FtpRenameCommand(this, config, client);
        writeCommand = new FtpWriteCommand(this, config, client);
    }

    /**
     * Severs the connection by invoking {@link FTPClient#logout()} and
     * {@link FTPClient#disconnect()} on the provided {@link #client}.
     * <p>
     * Notice that {@link FTPClient#disconnect()} will be invoked even if
     * {@link FTPClient#logout()} fails. This method will never throw
     * exception. Any errors will be logged.
     */
    void disconnect()
    {
        try
        {
            client.logout();
        }
        catch (FTPConnectionClosedException e)
        {
            // this is valid and expected if the server closes the connection prematurely as a result of the logout... ignore
        }
        catch (Exception e)
        {
            LOGGER.warn("Exception found trying to logout from ftp at " + toURL(null), e);
        }
        finally
        {
            try
            {
                client.disconnect();
            }
            catch (Exception e)
            {
                LOGGER.warn("Exception found trying to disconnect from ftp at " + toURL(null), e);
            }
        }
    }

    /**
     * Sets the transfer mode on the {@link #client}
     *
     * @param mode a {@link FtpTransferMode}
     */
    void setTransferMode(FtpTransferMode mode)
    {
        try
        {
            if (!client.setFileType(FTP.BINARY_FILE_TYPE))
            {
                throw new IOException(String.format("Failed to set %s transfer type. FTP reply code is: ", mode.getDescription(), client.getReplyCode()));
            }
            client.setFileType(mode.getCode());
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(createStaticMessage(String.format("Found exception trying to change transfer mode to %s. FTP reply code is: ",
                                                                             mode.getClass(), client.getReplyCode())));
        }
    }

    /**
     * Sets the response timeout on the {@link #client}
     *
     * @param timeout  a scalar timeout value
     * @param timeUnit a {@link TimeUnit} which qualifies the {@code timeout}
     */
    void setResponseTimeout(Integer timeout, TimeUnit timeUnit)
    {
        client.setDataTimeout(new Long(timeUnit.toMillis(timeout)).intValue());
    }

    /**
     * If {@code passive} is {@code true} then the {@link #client} is
     * set on passive mode. Otherwise is set on active mode.
     *
     * @param passive whether to go passive mode or not
     */
    void setPassiveMode(boolean passive)
    {
        if (passive)
        {
            LOGGER.debug("Entering FTP passive mode");
            client.enterLocalPassiveMode();
        }
        else
        {
            LOGGER.debug("Entering FTP active mode");
            client.enterLocalActiveMode();
        }
    }

    /**
     * Returns an InputStream which obtains the content for the
     * file of the given {@code filePayload}.
     * <p>
     * The invoked <b>MUST</b> make sure that the returned stream
     * is closed in order for the underlying connection
     * to be closed.
     *
     * @param filePayload a {@link FtpFilePayload} referencing to a FTP file
     * @return an {@link InputStream}
     */
    InputStream retrieveFileContent(FtpFilePayload filePayload)
    {
        try
        {
            InputStream inputStream = client.retrieveFileStream(filePayload.getPath());
            if (inputStream == null)
            {
                throw new FileNotFoundException(String.format("Could not retrieve content of file '%s' because it doesn't exists", filePayload.getPath()));
            }

            return inputStream;
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(createStaticMessage(format("Exception was found trying to retrieve the contents of file '%s'. Ftp reply code: %d ", filePayload.getPath(), client.getReplyCode())), e);
        }
    }

    /**
     * Awaits for the underlying {@link #client} to complete
     * any pending commands. This is necessary for certain
     * operations such as write. Using the {@link #client}
     * before that can result in unexpected behavior
     */
    public void awaitCommandCompletion()
    {
        try
        {
            if (!client.completePendingCommand())
            {
                throw new IllegalStateException("Pending command did not complete");
            }
        }
        catch (IOException e)
        {
            throw new MuleRuntimeException(createStaticMessage("Failed to complete pending command. Ftp reply code: " + client.getReplyCode()), e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link URLPathLock} based on the {@link #client}'s connection information
     */
    @Override
    protected PathLock createLock(Path path, Object... params)
    {
        return new URLPathLock(toURL(path), muleContext.getLockFactory());
    }

    private URL toURL(Path path)
    {
        URL url;
        try
        {
            url = new URL("ftp", client.getRemoteAddress().toString(), client.getRemotePort(), path != null ? path.toString() : EMPTY);
        }
        catch (MalformedURLException e)
        {
            throw new MuleRuntimeException(createStaticMessage("Could not get URL for FTP server"), e);
        }
        return url;
    }

    /**
     * Changes the {@link #client}'s current working directory to
     * the {@link #config}'s {@link FtpConnector#getBaseDir()}
     */
    protected void changeToBaseDir()
    {
        if (config.getBaseDir() != null)
        {
            try
            {
                client.changeWorkingDirectory(Paths.get(config.getBaseDir()).toString());
            }
            catch (IOException e)
            {
                throw new MuleRuntimeException(createStaticMessage(format("Failed to perform CWD to the base directory '%s'", config.getBaseDir())), e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ReadCommand getReadCommand()
    {
        return readCommand;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ListCommand getListCommand()
    {
        return listCommand;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected WriteCommand getWriteCommand()
    {
        return writeCommand;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CopyCommand getCopyCommand()
    {
        return copyCommand;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MoveCommand getMoveCommand()
    {
        return moveCommand;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DeleteCommand getDeleteCommand()
    {
        return deleteCommand;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RenameCommand getRenameCommand()
    {
        return renameCommand;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CreateDirectoryCommand getCreateDirectoryCommand()
    {
        return createDirectoryCommand;
    }

    public FtpConnector getConfig()
    {
        return config;
    }
}
