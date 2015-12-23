/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.transformer.types.MimeTypes.JSON;
import org.mule.api.MuleEvent;
import org.mule.extension.file.api.LocalFilePayload;
import org.mule.util.IOUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.Test;

public class FileReadTestCase extends FileConnectorTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "file-read-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        createHelloWorldFile();
    }

    @Test
    public void read() throws Exception
    {
        MuleEvent response = readHelloWorld();

        assertThat(response.getMessage().getDataType().getMimeType(), is(JSON));

        LocalFilePayload payload = (LocalFilePayload) response.getMessage().getPayload();
        assertThat(payload.isLocked(), is(false));
        assertThat(IOUtils.toString(payload.getContent()), is(HELLO_WORLD));
    }

    @Test
    public void readWithForcedMimeType() throws Exception
    {
        MuleEvent event = getTestEvent("");
        event.setFlowVariable("path", HELLO_PATH);
        event = runFlow("readWithForcedMimeType", event);
        assertThat(event.getMessage().getDataType().getMimeType(), equalTo("test/test"));
    }

    @Test
    public void readUnexisting() throws Exception
    {
        expectedException.expectCause(instanceOf(IllegalArgumentException.class));
        readPath("files/not-there.txt");
    }

    @Test
    public void readDirectory() throws Exception
    {
        expectedException.expectCause(instanceOf(IllegalArgumentException.class));
        readPath("files");
    }

    @Test
    public void readLockReleasedOnContentConsumed() throws Exception
    {
        LocalFilePayload payload = readWithLock();
        IOUtils.toString(payload.getContent());

        assertThat(payload.isLocked(), is(false));
    }

    @Test
    public void readLockReleasedOnEarlyClose() throws Exception
    {
        LocalFilePayload payload = readWithLock();
        payload.close();

        assertThat(payload.isLocked(), is(false));
    }

    @Test
    public void getProperties() throws Exception
    {
        LocalFilePayload filePayload = (LocalFilePayload) readHelloWorld().getMessage().getPayload();
        Path file = Paths.get(baseDir.getValue()).resolve(HELLO_PATH);
        assertExists(true, file.toFile());

        BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
        assertTime(filePayload.getCreationTime(), attributes.creationTime());
        assertThat(filePayload.getName(), equalTo(file.getFileName().toString()));
        assertTime(filePayload.getLastAccessTime(), attributes.lastAccessTime());
        assertTime(filePayload.getLastModifiedTime(), attributes.lastModifiedTime());
        assertThat(filePayload.getPath(), is(file.toAbsolutePath().toString()));
        assertThat(filePayload.getSize(), is(attributes.size()));
        assertThat(filePayload.isDirectory(), is(false));
        assertThat(filePayload.isSymbolicLink(), is(false));
        assertThat(filePayload.isRegularFile(), is(true));
    }

    private LocalFilePayload readWithLock() throws Exception
    {
        LocalFilePayload payload = (LocalFilePayload) runFlow("readWithLock").getMessage().getPayload();
        assertThat(payload.isLocked(), is(true));

        return payload;
    }

    private void assertTime(LocalDateTime dateTime, FileTime fileTime)
    {
        assertThat(dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), is(fileTime.toInstant().toEpochMilli()));
    }

}
