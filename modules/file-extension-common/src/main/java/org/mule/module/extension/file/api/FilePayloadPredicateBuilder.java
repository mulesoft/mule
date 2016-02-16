/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.file.api;

import org.mule.extension.annotation.api.Alias;
import org.mule.extension.annotation.api.Parameter;
import org.mule.extension.annotation.api.param.Optional;
import org.mule.module.extension.file.api.matcher.PathMatcherPredicate;
import org.mule.module.extension.file.api.matcher.TimeSinceFunction;
import org.mule.module.extension.file.api.matcher.TimeUntilFunction;

import java.time.LocalDateTime;
import java.util.function.Predicate;

/**
 * Builds a {@link Predicate} which verifies that a {@link FileAttributes} instance
 * is compliant with a number of criterias. This builder is stateful and not
 * thread-safe. A new instance should be use per each desired {@link Predicate}.
 * <p>
 * This builder can either be used programmatically or through Mule's SDK since
 * its internal state is annotated with the {@link Parameter} annotation.
 * <p>
 * Criterias are evaluated using an {@code AND} operator, meaning that for
 * the predicate to accept a file, ALL the criterias must be complied with.
 * <p>
 * None of the criteria fields are mandatory. If a particular criteria is not
 * specified, then it's simply not applied on the evaluation.
 * <p>
 * The class is also given the &quot;matcher&quot; alias to make it DSL/XML
 * friendly.
 *
 * @since 4.0
 */
@Alias("matcher")
public class FilePayloadPredicateBuilder
{

    private static final TimeUntilFunction FILE_TIME_UNTIL = new TimeUntilFunction();
    private static final TimeSinceFunction FILE_TIME_SINCE = new TimeSinceFunction();

    /**
     * A matching pattern to be applied on the file name. This pattern
     * needs to be consistent with the rules of {@link PathMatcherPredicate}
     */
    @Parameter
    @Optional
    private String filenamePattern;

    /**
     * A matching pattern to be applied on the file path. This pattern
     * needs to be consistent with the rules of {@link PathMatcherPredicate}
     */
    @Parameter
    @Optional
    private String pathPattern;

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

    /**
     * If {@code true}, the predicate will only accept files which are directories.
     * If {@code false}, the predicate will only accept files which are not directories.
     * If not set, then the criteria doesn't apply.
     */
    @Parameter
    @Optional
    private Boolean directory;

    /**
     * If {@code true}, the predicate will only accept files which are not directories nor symbolic links.
     * If {@code false}, the predicate will only accept files which are not directories nor symbolic links.
     * If not set, then the criteria doesn't apply.
     */
    @Parameter
    @Optional
    private Boolean regularFile;

    /**
     * If {@code true}, the predicate will only accept files which are symbolic links.
     * If {@code false}, the predicate will only accept files which are symbolic links.
     * If not set, then the criteria doesn't apply.
     */
    @Parameter
    @Optional
    private Boolean symbolicLink;

    /**
     * The minimum file size in bytes. Files smaller than this are rejected
     */
    @Parameter
    @Optional
    private Long minSize;

    /**
     * The maximum file size in bytes. Files larger than this are rejected
     */
    @Parameter
    @Optional
    private Long maxSize;


    /**
     * Builds a {@link Predicate} from the criterias in {@code this} builder's state.
     *
     * @return a {@link Predicate}
     */
    public Predicate<FileAttributes> build()
    {
        Predicate<FileAttributes> predicate = payload -> true;
        if (filenamePattern != null)
        {
            PathMatcherPredicate pathMatcher = new PathMatcherPredicate(filenamePattern);
            predicate = predicate.and(payload -> pathMatcher.test(payload.getName()));
        }

        if (pathPattern != null)
        {
            PathMatcherPredicate pathMatcher = new PathMatcherPredicate(pathPattern);
            predicate = predicate.and(payload -> pathMatcher.test(payload.getPath()));
        }

        if (createdSince != null)
        {
            predicate = predicate.and(filePayload -> FILE_TIME_SINCE.apply(createdSince, filePayload.getCreationTime()));
        }

        if (createdUntil != null)
        {
            predicate = predicate.and(filePayload -> FILE_TIME_UNTIL.apply(createdUntil, filePayload.getCreationTime()));
        }

        if (updatedSince != null)
        {
            predicate = predicate.and(filePayload -> FILE_TIME_SINCE.apply(updatedSince, filePayload.getLastModifiedTime()));
        }

        if (updatedUntil != null)
        {
            predicate = predicate.and(filePayload -> FILE_TIME_UNTIL.apply(updatedUntil, filePayload.getLastModifiedTime()));
        }

        if (accessedSince != null)
        {
            predicate = predicate.and(filePayload -> FILE_TIME_SINCE.apply(accessedSince, filePayload.getLastAccessTime()));
        }

        if (accessedUntil != null)
        {
            predicate = predicate.and(filePayload -> FILE_TIME_SINCE.apply(accessedUntil, filePayload.getLastAccessTime()));
        }

        if (directory != null)
        {
            predicate = predicate.and(filePayload -> directory.equals(filePayload.isDirectory()));
        }

        if (regularFile != null)
        {
            predicate = predicate.and(filePayload -> regularFile.equals(filePayload.isRegularFile()));
        }

        if (symbolicLink != null)
        {
            predicate = predicate.and(filePayload -> symbolicLink.equals(filePayload.isSymbolicLink()));
        }

        if (minSize != null)
        {
            predicate = predicate.and(filePayload -> filePayload.getSize() >= minSize);
        }

        if (maxSize != null)
        {
            predicate = predicate.and(filePayload -> filePayload.getSize() <= maxSize);
        }

        return predicate;
    }

    public FilePayloadPredicateBuilder setFilenamePattern(String filenamePattern)
    {
        this.filenamePattern = filenamePattern;
        return this;
    }

    public FilePayloadPredicateBuilder setPathPattern(String pathPattern)
    {
        this.pathPattern = pathPattern;
        return this;
    }

    public FilePayloadPredicateBuilder setCreatedSince(LocalDateTime createdSince)
    {
        this.createdSince = createdSince;
        return this;
    }

    public FilePayloadPredicateBuilder setCreatedUntil(LocalDateTime createdUntil)
    {
        this.createdUntil = createdUntil;
        return this;
    }

    public FilePayloadPredicateBuilder setUpdatedSince(LocalDateTime updatedSince)
    {
        this.updatedSince = updatedSince;
        return this;
    }

    public FilePayloadPredicateBuilder setUpdatedUntil(LocalDateTime updatedUntil)
    {
        this.updatedUntil = updatedUntil;
        return this;
    }

    public FilePayloadPredicateBuilder setAccessedSince(LocalDateTime accessedSince)
    {
        this.accessedSince = accessedSince;
        return this;
    }

    public FilePayloadPredicateBuilder setAccessedUntil(LocalDateTime accessedUntil)
    {
        this.accessedUntil = accessedUntil;
        return this;
    }

    public FilePayloadPredicateBuilder setDirectory(Boolean directory)
    {
        this.directory = directory;
        return this;
    }

    public FilePayloadPredicateBuilder setRegularFile(Boolean regularFile)
    {
        this.regularFile = regularFile;
        return this;
    }

    public FilePayloadPredicateBuilder setSymbolicLink(Boolean symbolicLink)
    {
        this.symbolicLink = symbolicLink;
        return this;
    }

    public FilePayloadPredicateBuilder setMinSize(Long minSize)
    {
        this.minSize = minSize;
        return this;
    }

    public FilePayloadPredicateBuilder setMaxSize(Long maxSize)
    {
        this.maxSize = maxSize;
        return this;
    }
}
