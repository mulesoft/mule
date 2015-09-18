/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.file.api;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for implementations of {@link FilePayload}
 *
 * @since 4.0
 */
public abstract class AbstractFilePayload implements FilePayload
{

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFilePayload.class);

    protected final Path path;
    protected final PathLock lock;
    protected InputStream content;

    /**
     * Creates a new instance
     *
     * @param path a {@link Path} pointing to the represented file
     */
    protected AbstractFilePayload(Path path)
    {
        this(path, new NullPathLock());
    }

    /**
     * Creates a new instance.
     * <p>
     * This constructor allows providing a {@link PathLock}
     * to be released when the {@link #close()} method is invoked
     * or when the {@link InputStream} returned by the
     * {@link #getContent()} method is closed or fully consumed.
     * <p>
     * It is the responsibility of whomever invokes this constructor
     * to invoke (if necessary) the {@link PathLock#tryLock()} method
     * on the supplied {@code lock}.
     *
     * @param path a {@link Path} pointing to the represented file
     * @param lock a {@link PathLock}
     */
    protected AbstractFilePayload(Path path, PathLock lock)
    {
        this.path = path;
        this.lock = lock;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath()
    {
        return path.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return path.getFileName().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final synchronized InputStream getContent()
    {
        if (content == null)
        {
            try
            {
                content = doGetContent();
            }
            catch (Exception e)
            {
                throw new MuleRuntimeException(createStaticMessage("Could not open file " + path), e);
            }
        }

        return content;
    }

    protected abstract InputStream doGetContent() throws Exception;


    /**
     * Closes the {@link InputStream} returned by {@link #getContent()}
     * and releases the {@link #lock} if supplied
     *
     * @throws IOException
     */
    @Override
    public final synchronized void close() throws MuleException
    {
        try
        {
            if (content != null)
            {
                closeContentStream(content);
            }
        }
        catch (Exception e)
        {
            if (LOGGER.isInfoEnabled())
            {
                LOGGER.info("Could not close stream for file " + path.toString(), e);
            }
        }
        finally
        {
            lock.release();
        }
    }

    protected void closeContentStream(InputStream contentInputStream) throws Exception
    {
        contentInputStream.close();
    }

    @Override
    public boolean isLocked()
    {
        return lock.isLocked();
    }

    protected LocalDateTime asDateTime(Instant instant)
    {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
