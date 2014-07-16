/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;

import static org.junit.Assert.assertFalse;

import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.ftp.server.FTPTestClient;
import org.mule.transport.ftp.server.MuleFtplet;
import org.mule.transport.ftp.server.Server;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.ftpserver.ftplet.Ftplet;
import org.junit.Rule;

/**
 * Abstract FTP test class. Sets up the ftp server and starts/stops it during the
 * test lifecycle.
 */
public abstract class AbstractFtpServerTestCase extends AbstractServiceAndFlowTestCase implements MuleFtplet.Callback
{
    public static final String TEST_MESSAGE = "Test FTP message";

    private static final String DEFAULT_FTP_HOST = "localhost";
    private static int DEFAULT_TIMEOUT = 10000;
    public static final String FTP_SERVER_BASE_DIR = "target/ftpserver";

    /**
     * We only test against an embedded server, but you can test against remote
     * servers by changing this value. Some tests may start/stop the ftp server so
     * you will have to implement remote start/stop as well.
     */
    private String ftpHost;

    private int ftpPort;
    private String ftpUser = "anonymous";
    private String ftpPassword = "password";
    private int timeout;
    private Server server = null;
    private FTPTestClient ftpClient = null;

    /**
     * Subclasses can overwrite Ftplet that will be registered when creating the server.
     */
    protected Ftplet ftplet = new MuleFtplet(this);

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public AbstractFtpServerTestCase(ConfigVariant variant, String configResources, String ftpHost, int timeout)
    {
        super(variant, configResources);
        this.ftpHost = ftpHost;
        this.timeout = timeout;
    }

    public AbstractFtpServerTestCase(ConfigVariant variant, String configResources, int timeout)
    {
        this(variant, configResources, DEFAULT_FTP_HOST, timeout);
    }

    public AbstractFtpServerTestCase(ConfigVariant variant, String configResources)
    {
        this(variant, configResources, DEFAULT_TIMEOUT);
    }
    
    protected void startServer() throws Exception
    {
        server = new Server(ftpPort, ftplet);
        // this is really ugly, but the above doesn't get to waiting.
        // need to improve this as part of ftp server work
        synchronized(this)
        {
            wait(500);
        }
    }

    protected void stopServer() throws Exception
    {
        // stop the server
        if (null != server)
        {
            server.stop();
        }
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        this.ftpPort = dynamicPort.getNumber();
        ftpClient = new FTPTestClient(this.ftpHost, this.ftpPort, this.ftpUser, this.ftpPassword);
        // make sure we start out with a clean ftp server base
        createFtpServerBaseDir();

        startServer();
        if (!ftpClient.testConnection())
        {
            throw new IOException("could not connect to ftp server");
        }
    }

    @Override
    protected void doTearDown() throws Exception
    {
        // give Mule some time to disconnect from the FTP server
        Thread.sleep(500);

        ftpClient.disconnect(); // we dont need the connection anymore for this test
        stopServer();

        deleteFtpServerBaseDir();

        super.doTearDown();
    }

    private void createFtpServerBaseDir()
    {
        deleteFtpServerBaseDir();
        File ftpBaseDir = new File(FTP_SERVER_BASE_DIR);
        ftpBaseDir.mkdirs();
    }

    protected void createFtpServerDir(String directoryName)
    {
        File ftpDir = new File(FTP_SERVER_BASE_DIR, directoryName);
        ftpDir.mkdirs();
    }

    private void deleteFtpServerBaseDir()
    {
        File ftpServerBase = new File(FTP_SERVER_BASE_DIR);
        FileUtils.deleteTree(ftpServerBase);
    }

    protected int getTimeout()
    {
        return timeout;
    }

    public FTPTestClient getFtpClient()
    {
        return ftpClient;
    }

    /**
     * Return the endpoint denoted by the ftp configuration
     */
    public String getMuleFtpEndpoint()
    {
        return "ftp://" + ftpUser + ":" + ftpPassword + "@" + ftpHost + ":" + ftpPort;
    }

    protected void createFileOnFtpServer(String fileName) throws IOException
    {
        File outFile = new File(FTP_SERVER_BASE_DIR, fileName);
        assertFalse(outFile.exists());

        Writer outWriter = new FileWriter(outFile);
        outWriter.write(TEST_MESSAGE);
        outWriter.close();
    }

    protected boolean fileExists(String fileName)
    {
        File file = new File(FTP_SERVER_BASE_DIR, fileName);
        return file.exists();
    }

    //
    // callback methods from MuleFtplet
    //
    @Override
    public void fileUploadCompleted()
    {
        // subclasses can override this method
    }

    @Override
    public void fileMoveCompleted()
    {
        // subclasses can override this method
    }

    protected File getFtpServerBaseDir()
    {
        return new File(FTP_SERVER_BASE_DIR);
    }
    protected File createDataFile(File folder, final String testMessage) throws Exception
    {
        return createDataFile(folder, "UTF-8", testMessage);

    }

    protected File createDataFile(File folder, String encoding, final String testMessage) throws Exception
    {
        File target = File.createTempFile("mule-file-test-", ".txt", folder);
        target.deleteOnExit();
        FileUtils.writeStringToFile(target, testMessage, encoding);

        return target;
    }
}
