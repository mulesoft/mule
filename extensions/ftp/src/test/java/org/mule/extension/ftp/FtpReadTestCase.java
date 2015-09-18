/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.transformer.types.MimeTypes.JSON;
import org.mule.api.MuleEvent;
import org.mule.module.extension.file.api.FilePayload;
import org.mule.util.IOUtils;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.Test;

public class FtpReadTestCase extends FtpConnectorTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "ftp-read-config.xml";
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

        FilePayload payload = (FilePayload) response.getMessage().getPayload();
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
        FilePayload payload = readWithLock();
        IOUtils.toString(payload.getContent());

        assertThat(payload.isLocked(), is(false));
    }

    @Test
    public void readLockReleasedOnEarlyClose() throws Exception
    {
        FilePayload payload = readWithLock();
        payload.close();

        assertThat(payload.isLocked(), is(false));
    }

    @Test
    public void getProperties() throws Exception
    {
        FilePayload filePayload = (FilePayload) readHelloWorld().getMessage().getPayload();
        FTPFile file = ftpClient.get(HELLO_PATH);

        assertThat(filePayload.getName(), equalTo(file.getName()));
        assertThat(filePayload.getPath(), equalTo(Paths.get("/", BASE_DIR, HELLO_PATH).toString()));
        assertThat(filePayload.getSize(), is(file.getSize()));
        assertTime(filePayload.getLastModifiedTime(), file.getTimestamp());
        assertThat(filePayload.isDirectory(), is(false));
        assertThat(filePayload.isSymbolicLink(), is(false));
        assertThat(filePayload.isRegularFile(), is(true));
    }

    private void assertTime(LocalDateTime dateTime, Calendar calendar)
    {
        assertThat(dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), is(calendar.toInstant().toEpochMilli()));
    }

    private FilePayload readWithLock() throws Exception
    {
        FilePayload payload = (FilePayload) runFlow("readWithLock").getMessage().getPayload();
        assertThat(payload.isLocked(), is(true));

        return payload;
    }
}
