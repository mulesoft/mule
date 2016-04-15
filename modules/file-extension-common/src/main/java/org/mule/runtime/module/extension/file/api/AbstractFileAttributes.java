/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.file.api;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Base class for implementations of {@link FileAttributes}
 *
 * @since 4.0
 */
public abstract class AbstractFileAttributes implements FileAttributes
{

    protected final Path path;

    /**
     * Creates a new instance
     *
     * @param path a {@link Path} pointing to the represented file
     */
    protected AbstractFileAttributes(Path path)
    {
        this.path = path;
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

    protected LocalDateTime asDateTime(Instant instant)
    {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
