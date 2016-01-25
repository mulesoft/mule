/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.file;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.module.extension.file.api.FileAttributes;
import org.mule.module.extension.file.api.FilePayloadPredicateBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class FileAttributesPredicateBuilderTestCase extends AbstractMuleTestCase
{

    private static final String FILENAME = "Mule.java";
    private static final String PATH = "a/b/c/" + FILENAME;
    private static final LocalDateTime CREATION_TIME = LocalDateTime.of(1983, 4, 20, 21, 15);
    private static final LocalDateTime MODIFIED_TIME = LocalDateTime.of(2011, 2, 5, 22, 00);
    private static final LocalDateTime ACCESSED_TIME = LocalDateTime.of(2015, 4, 20, 00, 00);
    private static final long SIZE = 1024;

    private FilePayloadPredicateBuilder builder = new FilePayloadPredicateBuilder();
    private FileAttributes payload;

    @Before
    public void before()
    {
        payload = mock(FileAttributes.class);
        when(payload.getName()).thenReturn(FILENAME);
        when(payload.getPath()).thenReturn(PATH);
        when(payload.getCreationTime()).thenReturn(CREATION_TIME);
        when(payload.getLastModifiedTime()).thenReturn(MODIFIED_TIME);
        when(payload.getLastAccessTime()).thenReturn(ACCESSED_TIME);
        when(payload.getSize()).thenReturn(SIZE);
        when(payload.isRegularFile()).thenReturn(true);
        when(payload.isSymbolicLink()).thenReturn(false);
        when(payload.isDirectory()).thenReturn(false);
    }

    @Test
    public void matchesAll()
    {
        builder
                .setFilenamePattern("glob:*.{java, js}")
                .setPathPattern("glob:**.{java, js}")
                .setCreatedSince(LocalDateTime.of(1980, 1, 1, 0, 0))
                .setCreatedUntil(LocalDateTime.of(1990, 1, 1, 0, 0))
                .setUpdatedSince(LocalDateTime.of(2010, 9, 24, 0, 0))
                .setUpdatedUntil(LocalDateTime.of(2013, 11, 3, 6, 0))
                .setAccessedSince(LocalDateTime.of(2013, 11, 3, 0, 0))
                .setAccessedUntil(LocalDateTime.of(2015, 4, 20, 0, 0))
                .setRegularFile(true)
                .setDirectory(false)
                .setSymbolicLink(false)
                .setMinSize(1L)
                .setMaxSize(1024L);

        assertMatch();
    }

    @Test
    public void matchesManyButFailsOne()
    {
        matchesAll();
        builder.setMaxSize(1L);

        assertReject();
    }

    @Test
    public void matchFilenameLiterally()
    {
        builder.setFilenamePattern(FILENAME);
        assertMatch();
    }

    @Test
    public void rejectFilenameLiterally()
    {
        builder.setFilenamePattern("fail.pdf");
        assertReject();
    }

    @Test
    public void matchFilenameByGlob()
    {
        builder.setFilenamePattern("glob:*.{java, js}");
        assertMatch();
    }

    @Test
    public void rejectFilenameByGlob()
    {
        builder.setFilenamePattern("glob:*.{pdf}");
        assertReject();
    }

    @Test
    public void matchFilenameByRegex()
    {
        when(payload.getName()).thenReturn("20060101_test.csv");
        builder.setFilenamePattern("regex:[0-9]*_test.csv");
        assertMatch();
    }

    @Test
    public void rejectFilenameByRegex()
    {
        when(payload.getName()).thenReturn("20060101_TEST.csv");
        builder.setFilenamePattern("regex:[0-9]*_test.csv");
        assertReject();
    }

    @Test
    public void matchPathLiterally()
    {
        builder.setPathPattern(PATH);
        assertMatch();
    }

    @Test
    public void rejectPathLiterally()
    {
        builder.setPathPattern("a/b/d/Mule.pdf");
        assertReject();
    }

    @Test
    public void matchPathByGlob()
    {
        builder.setPathPattern("glob:**.{java, js}");
        assertMatch();
    }

    @Test
    public void rejectPathByGlob()
    {
        builder.setPathPattern("glob:*.{java, js}");
        assertReject();
    }

    @Test
    public void matchPathByRegex()
    {
        when(payload.getPath()).thenReturn("a/b/c/20060101_test.csv");
        builder.setPathPattern("regex:a/b/c/[0-9]*_test.csv");
        assertMatch();
    }

    @Test
    public void rejectPathByRegex()
    {
        when(payload.getName()).thenReturn("20060101_TEST.csv");
        builder.setFilenamePattern("regex:[0-9]*_test.csv");
        assertReject();
    }

    @Test
    public void createdSince()
    {
        builder.setCreatedSince(LocalDateTime.of(1980, 1, 1, 0, 0));
        assertMatch();
    }

    @Test
    public void createdUntil()
    {
        builder.setCreatedUntil(LocalDateTime.of(1990, 1, 1, 0, 0));
        assertMatch();
    }

    @Test
    public void rejectCreatedSince()
    {
        builder.setCreatedSince(LocalDateTime.of(1984, 1, 1, 0, 0));
        assertReject();
    }

    @Test
    public void rejectCreatedUntil()
    {
        builder.setCreatedUntil(LocalDateTime.of(1982, 4, 2, 0, 0));
        assertReject();
    }

    @Test
    public void updateSince()
    {
        builder.setUpdatedSince(LocalDateTime.of(2010, 9, 24, 0, 0));
        assertMatch();
    }

    @Test
    public void updatedUntil()
    {
        builder.setUpdatedUntil(LocalDateTime.of(2013, 11, 3, 6, 0));
        assertMatch();
    }

    @Test
    public void rejectUpdatedSince()
    {
        builder.setUpdatedSince(LocalDateTime.of(2015, 1, 1, 0, 0));
        assertReject();
    }

    @Test
    public void rejectUpdatedUntil()
    {
        builder.setUpdatedUntil(LocalDateTime.of(2010, 9, 24, 0, 0));
        assertReject();
    }

    @Test
    public void accessedSince()
    {
        builder.setAccessedSince(LocalDateTime.of(2013, 11, 3, 0, 0));
        assertMatch();
    }

    @Test
    public void accessedUntil()
    {
        builder.setAccessedUntil(LocalDateTime.of(2015, 4, 20, 0, 0));
        assertMatch();
    }

    @Test
    public void rejectAccessedSince()
    {
        builder.setAccessedSince(LocalDateTime.of(2016, 1, 1, 0, 0));
        assertReject();
    }

    @Test
    public void rejectAccessedUntil()
    {
        builder.setUpdatedUntil(LocalDateTime.of(2010, 9, 24, 0, 0));
        assertReject();
    }

    @Test
    public void minSize()
    {
        builder.setMinSize(1L);
        assertMatch();
    }

    @Test
    public void maxSize()
    {
        builder.setMaxSize(1024L);
        assertMatch();
    }

    @Test
    public void rejectMinSize()
    {
        builder.setMinSize(2048L);
        assertReject();
    }

    @Test
    public void rejectMaxSize()
    {
        builder.setMaxSize(500L);
        assertReject();
    }

    @Test
    public void regularFile()
    {
        when(payload.isRegularFile()).thenReturn(true);
        builder.setRegularFile(true);
        assertMatch();
    }

    @Test
    public void rejectNotRegularFile()
    {
        when(payload.isRegularFile()).thenReturn(false);
        builder.setRegularFile(true);
        assertReject();
    }

    @Test
    public void rejectRegularFile()
    {
        when(payload.isRegularFile()).thenReturn(true);
        builder.setRegularFile(false);
        assertReject();
    }

    @Test
    public void isDirectory()
    {
        when(payload.isDirectory()).thenReturn(true);
        builder.setDirectory(true);
        assertMatch();
    }

    @Test
    public void rejectNotDirectory()
    {
        when(payload.isDirectory()).thenReturn(false);
        builder.setDirectory(true);
        assertReject();
    }

    @Test
    public void rejectDirectory()
    {
        when(payload.isDirectory()).thenReturn(true);
        builder.setDirectory(false);
        assertReject();
    }


    @Test
    public void isSymbolicLink()
    {
        when(payload.isSymbolicLink()).thenReturn(true);
        builder.setSymbolicLink(true);
        assertMatch();
    }

    @Test
    public void rejectNotSymbolicLink()
    {
        when(payload.isSymbolicLink()).thenReturn(false);
        builder.setSymbolicLink(true);
        assertReject();
    }

    @Test
    public void rejectSymbolicLink()
    {
        when(payload.isSymbolicLink()).thenReturn(true);
        builder.setSymbolicLink(false);
        assertReject();
    }

    private void assertMatch()
    {
        assertThat(builder.build().test(payload), is(true));
    }

    private void assertReject()
    {
        assertThat(builder.build().test(payload), is(false));
    }
}
