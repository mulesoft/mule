/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal;

import org.mule.extension.api.annotation.Alias;
import org.mule.extension.api.annotation.Parameter;
import org.mule.extension.api.annotation.param.Optional;
import org.mule.extension.file.api.LocalFileAttributes;
import org.mule.runtime.module.extension.file.api.FilePredicateBuilder;

import java.time.LocalDateTime;
import java.util.function.Predicate;

/**
 * A specialization of {@link FilePredicateBuilder} used to do assertions on
 * files stored on a local file system. The file's properties are to be represented
 * on an instance of {@link LocalFileAttributes}
 * <p>
 * It adds capabilities to consider creation, update and access timestamps.
 *
 * @since 4.0
 */
@Alias("matcher")
public class LocalFilePredicateBuilder extends FilePredicateBuilder<LocalFilePredicateBuilder, LocalFileAttributes>
{

    /**
     * Files created before this date are rejected.
     */
    @Parameter
    @Optional
    private LocalDateTime createdSince;

    /**
     * Files created after this date are rejected
     */
    @Parameter
    @Optional
    private LocalDateTime createdUntil;

    /**
     * Files modified before this date are rejected
     */
    @Parameter
    @Optional
    private LocalDateTime updatedSince;

    /**
     * Files modified after this date are rejected
     */
    @Parameter
    @Optional
    private LocalDateTime updatedUntil;

    /**
     * Files which were last accessed before this date are rejected
     */
    @Parameter
    @Optional
    private LocalDateTime accessedSince;

    /**
     * Files which were last accessed after this date are rejected
     */
    @Parameter
    @Optional
    private LocalDateTime accessedUntil;

    @Override
    protected Predicate<LocalFileAttributes> addConditions(Predicate<LocalFileAttributes> predicate)
    {
        if (createdSince != null)
        {
            predicate = predicate.and(attributes -> FILE_TIME_SINCE.apply(createdSince, attributes.getCreationTime()));
        }

        if (createdUntil != null)
        {
            predicate = predicate.and(attributes -> FILE_TIME_UNTIL.apply(createdUntil, attributes.getCreationTime()));
        }

        if (updatedSince != null)
        {
            predicate = predicate.and(attributes -> FILE_TIME_SINCE.apply(updatedSince, attributes.getLastModifiedTime()));
        }

        if (updatedUntil != null)
        {
            predicate = predicate.and(attributes -> FILE_TIME_UNTIL.apply(updatedUntil, attributes.getLastModifiedTime()));
        }

        if (accessedSince != null)
        {
            predicate = predicate.and(attributes -> FILE_TIME_SINCE.apply(accessedSince, attributes.getLastAccessTime()));
        }

        if (accessedUntil != null)
        {
            predicate = predicate.and(attributes -> FILE_TIME_SINCE.apply(accessedUntil, attributes.getLastAccessTime()));
        }

        return predicate;
    }

    public LocalFilePredicateBuilder setCreatedSince(LocalDateTime createdSince)
    {
        this.createdSince = createdSince;
        return this;
    }

    public LocalFilePredicateBuilder setCreatedUntil(LocalDateTime createdUntil)
    {
        this.createdUntil = createdUntil;
        return this;
    }

    public LocalFilePredicateBuilder setUpdatedSince(LocalDateTime updatedSince)
    {
        this.updatedSince = updatedSince;
        return this;
    }

    public LocalFilePredicateBuilder setUpdatedUntil(LocalDateTime updatedUntil)
    {
        this.updatedUntil = updatedUntil;
        return this;
    }

    public LocalFilePredicateBuilder setAccessedSince(LocalDateTime accessedSince)
    {
        this.accessedSince = accessedSince;
        return this;
    }

    public LocalFilePredicateBuilder setAccessedUntil(LocalDateTime accessedUntil)
    {
        this.accessedUntil = accessedUntil;
        return this;
    }
}
