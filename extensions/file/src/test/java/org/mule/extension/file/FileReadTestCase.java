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
import static org.mule.runtime.api.metadata.MediaType.JSON;

import org.mule.extension.file.api.LocalFileAttributes;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.extension.file.api.stream.AbstractFileInputStream;

import java.io.File;
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

        assertThat(response.getMessage().getDataType().getMediaType().getPrimaryType(), is(JSON.getPrimaryType()));
        assertThat(response.getMessage().getDataType().getMediaType().getSubType(), is(JSON.getSubType()));

        AbstractFileInputStream payload = (AbstractFileInputStream) response.getMessage().getPayload();
        assertThat(payload.isLocked(), is(false));
        assertThat(IOUtils.toString(payload), is(HELLO_WORLD));
    }

    @Test
    public void readBinary() throws Exception
    {
        final byte[] binaryPayload = HELLO_WORLD.getBytes();
        final String binaryFileName = "binary.bin";
        File binaryFile = new File(temporaryFolder.getRoot(), binaryFileName);
        FileUtils.writeByteArrayToFile(binaryFile, binaryPayload);

        MuleEvent response = getPath(binaryFile.getAbsolutePath());

        assertThat(response.getMessage().getDataType().getMediaType().getPrimaryType(), is(MediaType.BINARY.getPrimaryType()));
        assertThat(response.getMessage().getDataType().getMediaType().getSubType(), is(MediaType.BINARY.getSubType()));

        AbstractFileInputStream payload = (AbstractFileInputStream) response.getMessage().getPayload();
        assertThat(payload.isLocked(), is(false));

        byte[] readContent = new byte[new Long(binaryFile.length()).intValue()];
        IOUtils.read(payload, readContent);
        assertThat(new String(readContent), is(HELLO_WORLD));
    }

    @Test
    public void readWithForcedMimeType() throws Exception
    {
        MuleEvent event = flowRunner("readWithForcedMimeType").withFlowVariable("path", HELLO_PATH).run();
        assertThat(event.getMessage().getDataType().getMediaType().getPrimaryType(), equalTo("test"));
        assertThat(event.getMessage().getDataType().getMediaType().getSubType(), equalTo("test"));
    }

    @Test
    public void readUnexisting() throws Exception
    {
        expectedException.expectCause(instanceOf(IllegalArgumentException.class));
        readPath("files/not-there.txt");
    }

    @Test
    public void readWithLockAndWithoutEnoughPermissions() throws Exception
    {
        expectedException.expectCause(instanceOf(IllegalArgumentException.class));
        File forbiddenFile = temporaryFolder.newFile("forbiddenFile");
        forbiddenFile.createNewFile();
        forbiddenFile.setWritable(false);
        readWithLock(forbiddenFile.getAbsolutePath());
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
        final AbstractFileInputStream payload = (AbstractFileInputStream) readWithLock().getPayload();
        IOUtils.toString(payload);

        assertThat(payload.isLocked(), is(false));
    }

    @Test
    public void readLockReleasedOnEarlyClose() throws Exception
    {
        final AbstractFileInputStream payload = (AbstractFileInputStream) readWithLock().getPayload();
        payload.close();

        assertThat(payload.isLocked(), is(false));
    }

    @Test
    public void getProperties() throws Exception
    {
        LocalFileAttributes filePayload = (LocalFileAttributes) readHelloWorld().getMessage().getAttributes();
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

    private MuleMessage readWithLock() throws Exception
    {
        return readWithLock(HELLO_PATH);
    }

    private MuleMessage readWithLock(String path) throws Exception
    {
        MuleMessage message = flowRunner("readWithLock").withFlowVariable("path", path).run().getMessage();
        assertThat(((AbstractFileInputStream) message.getPayload()).isLocked(), is(true));

        return message;
    }

    private void assertTime(LocalDateTime dateTime, FileTime fileTime)
    {
        assertThat(dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), is(fileTime.toInstant().toEpochMilli()));
    }

}
