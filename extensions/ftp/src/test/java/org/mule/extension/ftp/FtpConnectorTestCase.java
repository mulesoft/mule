/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.junit.rules.ExpectedException.none;
import org.mule.api.MuleEvent;
import org.mule.extension.ftp.internal.FtpConnector;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.module.extension.file.api.FilePayload;
import org.mule.module.extension.file.api.FileWriteMode;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.infrastructure.client.ftp.FTPTestClient;
import org.mule.test.infrastructure.process.rules.FtpServer;
import org.mule.util.IOUtils;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

public abstract class FtpConnectorTestCase extends ExtensionFunctionalTestCase
{

    protected static final String HELLO_WORLD = "Hello World!";
    protected static final String HELLO_FILE_NAME = "hello.json";
    protected static final String HELLO_PATH = "files/" + HELLO_FILE_NAME;
    protected static final String DEFAULT_FTP_HOST = "localhost";
    protected static final String FTP_SERVER_BASE_DIR = "target/ftpserver";
    protected static final String BASE_DIR = "base";

    @Rule
    public ExpectedException expectedException = none();

    @Rule
    public SystemProperty baseDirSystemProperty = new SystemProperty("baseDir", BASE_DIR);

    @Rule
    public FtpServer ftpServer = new FtpServer("ftpPort", new File(FTP_SERVER_BASE_DIR, BASE_DIR));


    private String ftpUser = "anonymous";
    private String ftpPassword = "password";
    protected FTPTestClient ftpClient;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        ftpClient = new FTPTestClient(DEFAULT_FTP_HOST, ftpServer.getPort(), this.ftpUser, this.ftpPassword);
        // make sure we start out with a clean ftp server base

        if (!ftpClient.testConnection())
        {
            throw new IOException("could not connect to ftp server");
        }
        ftpClient.changeWorkingDirectory(BASE_DIR);
    }

    @Override
    protected void doTearDown() throws Exception
    {
        if (ftpClient.isConnected())
        {
            ftpClient.disconnect();
        }

        super.doTearDown();
    }

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {FtpConnector.class};
    }

    protected void createHelloWorldFile() throws IOException
    {
        ftpClient.makeDir("files");
        ftpClient.putFile(HELLO_FILE_NAME, "files", HELLO_WORLD);
    }

    protected MuleEvent readHelloWorld() throws Exception
    {
        return getPath(HELLO_PATH);
    }

    protected FilePayload readPath(String path) throws Exception
    {
        return (FilePayload) getPath(path).getMessage().getPayload();
    }

    protected void doWrite(String path, Object content, FileWriteMode mode, boolean createParent) throws Exception
    {
        MuleEvent event = getTestEvent(content);
        event.setFlowVariable("path", path);
        event.setFlowVariable("createParent", createParent);
        event.setFlowVariable("mode", mode);

        runFlow("write", event);
    }

    private MuleEvent getPath(String path) throws Exception
    {
        MuleEvent event = getTestEvent("");
        event.setFlowVariable("path", path);
        return runFlow("read", event);
    }

    protected String readPathAsString(String path) throws Exception
    {
        return IOUtils.toString(readPath(path).getContent());
    }
}
