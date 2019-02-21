/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mule.context.notification.EndpointMessageNotification.MESSAGE_DISPATCHED;
import static org.mule.context.notification.EndpointMessageNotification.MESSAGE_SENT;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.client.MuleClient;
import org.mule.api.context.notification.EndpointMessageNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.RollbackSourceCallback;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.api.model.Model;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.util.sftp.SftpServer;
import org.mule.transport.sftp.util.ValueHolder;
import org.mule.util.StringMessageUtils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.slf4j.Logger;

/**
 * @author Lennart Häggkvist, Magnus Larsson Date: Jun 8, 2009
 */
public abstract class AbstractSftpTestCase extends AbstractServiceAndFlowTestCase
{
    private static final Logger LOGGER = getLogger(AbstractSftpTestCase.class);
    private static final String HOST = "localhost";
    private static final String USER = "muletest1";
    private static final String PASSWORD = "muletest1";
    private static final String FILENAME_HEADER = "filename";
    protected static final Map<String, Object> MESSAGE_PROPERTIES = new HashMap<String, Object>();
    protected static String INBOUND_ENDPOINT_DIR = "inbound";
    protected static String OUTBOUND_ENDPOINT_DIR = "outbound";
    {
        MESSAGE_PROPERTIES.put(FILENAME_HEADER, FILENAME);
    }

    protected static final String FILENAME = "file.txt";

    @Rule
    public DynamicPort port = new DynamicPort("SFTP_PORT");

    protected SftpServer sftpServer;
    protected SftpClient sftpClient;

    public AbstractSftpTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    /**
     * Deletes all files in the directory, useful when testing to ensure that no
     * files are in the way...
     */
    // protected void cleanupRemoteFtpDirectory(String
    // endpointName) throws IOException
    // {
    // SftpClient sftpClient = getSftpClient(endpointName);
    //
    // EndpointURI endpointURI = getUriByEndpointName(endpointName);
    // sftpClient.changeWorkingDirectory(sftpClient.getAbsolutePath(endpointURI.getPath()));
    //
    // String[] files = sftpClient.listFiles();
    // for (String file : files)
    // {
    // sftpClient.deleteFile(file);
    // }
    // }

    /**
     * Deletes a directory with all its files and sub-directories. The reason it do a
     * "chmod 700" before the delete is that some tests changes the permission, and
     * thus we have to restore the right to delete it...
     * @param endpointName
     * @param relativePath
     *
     * @throws IOException
     */
    protected void recursiveDelete(SftpClient client,
                                   String endpointName,
                                   String relativePath) throws IOException
    {
        EndpointURI endpointURI = getUriByEndpointName(endpointName);
        String path = endpointURI.getPath() + relativePath;

        try
        {
            // Ensure that we can delete the current directory and the below
            // directories (if write is not permitted then delete is either)
            client.chmod(path, 00700);

            client.changeWorkingDirectory(client.getAbsolutePath(path));

            // Delete all sub-directories
            String[] directories = client.listDirectories();
            for (String directory : directories)
            {
                recursiveDelete(client, endpointName, relativePath + "/" + directory);
            }

            // Needs to change the directory back after the recursiveDelete
            client.changeWorkingDirectory(client.getAbsolutePath(path));

            // Delete all files
            String[] files = client.listFiles();
            for (String file : files)
            {
                client.deleteFile(file);
            }

            // Delete the directory
            try
            {
                client.deleteDirectory(path);
            }
            catch (Exception e)
            {
                if (LOGGER.isDebugEnabled()) LOGGER.debug("Failed delete directory " + path, e);
            }

        }
        catch (Exception e)
        {
            if (LOGGER.isDebugEnabled()) LOGGER.debug("Failed to recursivly delete directory " + path, e);
        }
    }

    /** Creates the <i>directoryName</i> under the endpoint path */
    protected void createRemoteDirectory(String endpointName, String directoryName)
        throws IOException
    {
        SftpClient client = getSftpClient(endpointName);

        try
        {
            EndpointURI endpointURI = getUriByEndpointName(endpointName);
            client.changeWorkingDirectory(client.getAbsolutePath(endpointURI.getPath()));

            try
            {
                client.mkdir(directoryName);
            }
            catch (IOException e)
            {
                e.printStackTrace();
                // Expected if the directory didnt exist
            }

            try
            {
                client.changeWorkingDirectory(endpointURI.getPath() + "/" + directoryName);
            }
            catch (IOException e)
            {
                fail("The directory should have been created");
            }
        }
        finally
        {
            client.disconnect();
        }
    }

    protected EndpointURI getUriByEndpointName(String endpointName) throws IOException
    {
        ImmutableEndpoint endpoint = getImmutableEndpoint(endpointName);
        return endpoint.getEndpointURI();
    }

    /**
     * @return the endpoint address in the form 'sftp://user:password@host/path'
     */
    protected String getAddressByEndpoint(String endpointName)
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupObject(endpointName);
        EndpointURI endpointURI = endpoint.getEndpointURI();

        return "sftp://" + endpointURI.getUser() + ":" + endpointURI.getPassword() + "@"
               + endpointURI.getHost() + endpointURI.getPath();
    }

    protected String getPathByEndpoint(SftpClient client, String endpointName)
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupObject(endpointName);
        EndpointURI endpointURI = endpoint.getEndpointURI();

        return client.getAbsolutePath(endpointURI.getPath());
    }

    /**
     * Returns a SftpClient that is logged in to the sftp server that the endpoint is
     * configured against.
     */
    protected SftpClient getSftpClient(String endpointName) throws IOException
    {
        ImmutableEndpoint endpoint = getImmutableEndpoint(endpointName);
        EndpointURI endpointURI = endpoint.getEndpointURI();
        SftpClient client = new SftpClient(endpointURI.getHost());
        client.setPort(endpointURI.getPort());

        SftpConnector sftpConnector = (SftpConnector) endpoint.getConnector();

        if (sftpConnector.getIdentityFile() != null)
        {
            try
            {
                client.login(endpointURI.getUser(), sftpConnector.getIdentityFile(),
                    sftpConnector.getPassphrase());
            }
            catch (Exception e)
            {
                fail("Login failed: " + e);
            }
        }
        else
        {
            try
            {
                client.login(endpointURI.getUser(), endpointURI.getPassword());
            }
            catch (Exception e)
            {
                fail("Login failed: " + e);
            }
        }
        return client;
    }

    /**
     * Returns a SftpClient that is logged in to the sftp server that the endpoint is
     * configured against.
     */
    protected SftpClient getSftpClient(String host, int clientPort, String user, String password)
        throws IOException
    {
        SftpClient client = new SftpClient(host);
        client.setPort(clientPort);
        try
        {
            client.login(user, password);
        }
        catch (Exception e)
        {
            fail("Login failed: " + e);
        }
        return client;
    }


    /** Checks if the file exists on the server */
    protected boolean verifyFileExists(SftpClient client, EndpointURI endpointURI, String file)
        throws IOException
    {
        return verifyFileExists(client, endpointURI.getPath(), file);
    }

    protected boolean verifyFileExists(SftpClient client, String path, String file) throws IOException
    {
        client.changeWorkingDirectory(client.getAbsolutePath(path));
        String[] files = client.listFiles();

        for (String remoteFile : files)
        {
            if (remoteFile.equals(file))
            {
                return true;
            }
        }
        return false;
    }

    /** Base method for executing tests... */
    protected void executeBaseTest(String inputEndpointName,
                                   String sendUrl,
                                   String filename,
                                   final int size,
                                   String receivingTestComponentName,
                                   long timeout) throws Exception
    {
        executeBaseTest(inputEndpointName, sendUrl, filename, size, receivingTestComponentName, timeout, null);
    }

    protected void executeBaseTest(String inputEndpointName,
                                   String sendUrl,
                                   String filename,
                                   final int size,
                                   String receivingTestComponentName,
                                   long timeout,
                                   String expectedFailingConnector) throws Exception
    {
        executeBaseTest(inputEndpointName, sendUrl, filename, size, receivingTestComponentName, timeout,
            expectedFailingConnector, null);
    }

    /** Base method for executing tests... */
    protected void executeBaseTest(String inputEndpointName,
                                   String sendUrl,
                                   String filename,
                                   final int size,
                                   String receivingTestComponentName,
                                   long timeout,
                                   String expectedFailingConnector,
                                   String serviceName) throws Exception
    {
        // Do some cleaning so that the endpoint doesn't have any other files
        // We don't need to do this anymore since we are deleting and then creating
        // the directory for each test
        // cleanupRemoteFtpDirectory(client, inputEndpointName);

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger loopCount = new AtomicInteger(0);
        final AtomicInteger totalReceivedSize = new AtomicInteger(0);

        // Random byte that we want to send a lot of
        final int testByte = 42;

        EventCallback callback = new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {

                if (LOGGER.isInfoEnabled()) LOGGER.info("called " + loopCount.incrementAndGet() + " times");

                InputStream sftpInputStream = (InputStream) context.getMessage().getPayload();
                BufferedInputStream bif = new BufferedInputStream(sftpInputStream);
                byte[] buffer = new byte[1024 * 4];

                try
                {
                    int n;
                    while (-1 != (n = bif.read(buffer)))
                    {
                        totalReceivedSize.addAndGet(n);

                        // Simple check to verify the data...
                        for (byte b : buffer)
                        {
                            if (b != testByte)
                            {
                                fail("Incorrect received byte (was '" + b + "', expected '" + testByte + "'");
                            }
                        }
                    }
                }
                finally
                {
                    bif.close();
                }
                latch.countDown();
            }
        };
        getFunctionalTestComponent(receivingTestComponentName).setEventCallback(callback);

        final ValueHolder<Exception> exceptionHolder = new ValueHolder<Exception>();
        if (expectedFailingConnector != null)
        {
            // Register an exception-listener on the connector that expects to fail
            // and count down the latch after saving the thrown exception
            muleContext.setExceptionListener(new SystemExceptionHandler()
            {
                @Override
                public void handleException(Exception e, RollbackSourceCallback rollbackMethod)
                {
                    if (LOGGER.isInfoEnabled()) LOGGER.info("expected exception occurred: " + e, e);
                    exceptionHolder.value = e;
                    latch.countDown();
                }

                @Override
                public void handleException(Exception exception)
                {
                    handleException(exception, null);
                }
            });

            if (serviceName != null && !(serviceName.length() == 0))
            {
                muleContext.getRegistry().lookupService(serviceName).setExceptionListener(
                    new MessagingExceptionHandler()
                    {
                        @Override
                        public MuleEvent handleException(Exception e, MuleEvent event)
                        {
                            if (LOGGER.isInfoEnabled()) LOGGER.info("expected exception occurred: " + e, e);
                            exceptionHolder.value = e;
                            latch.countDown();
                            return event;
                        }
                    });
            }
        }

        // InputStream that generates the data without using a file
        InputStream os = new InputStream()
        {
            int totSize = 0;

            @Override
            public int read() throws IOException
            {
                totSize++;
                if (totSize <= size)
                {
                    return testByte;
                }
                else
                {
                    return -1;
                }
            }
        };

        HashMap<String, Object> props = new HashMap<String, Object>();
        props.put(SftpConnector.PROPERTY_FILENAME, filename);
        props.put(SftpConnector.PROPERTY_ORIGINAL_FILENAME, filename);

        if (LOGGER.isInfoEnabled())
        {
            LOGGER.info(StringMessageUtils.getBoilerPlate("Note! If this test fails due to timeout please add '-Dmule.test.timeoutSecs=XX' to the mvn command!"));
        }

        executeBaseAssertionsBeforeCall();

        // Send the content using stream
        MuleClient client = muleContext.getClient();
        client.dispatch(sendUrl, os, props);

        boolean workDone = latch.await(timeout, TimeUnit.MILLISECONDS);

        assertTrue(
            "Test timed out. It took more than "
                            + timeout
                            + " milliseconds. If this error occurs the test probably needs a longer time out (on your computer/network)",
            workDone);

        // Rethrow any exception that we have caught in an exception-listener
        if (exceptionHolder.value != null)
        {
            throw exceptionHolder.value;
        }
        executeBaseAssertionsAfterCall(size, totalReceivedSize.intValue());
    }

    /**
     * To be overridden by the test-classes if required
     */
    protected void executeBaseAssertionsBeforeCall()
    {
        // empty
    }

    /**
     * To be overridden by the test-classes if required
     */
    protected void executeBaseAssertionsAfterCall(int sendSize, int receivedSize)
    {

        // Make sure that the file we received had the same size as the one we sent
        if (LOGGER.isInfoEnabled())
        {
            LOGGER.info("Sent size: " + sendSize);
            LOGGER.info("Received size: " + receivedSize);
        }

        assertEquals("The received file should have the same size as the sent file", sendSize, receivedSize);
    }

    protected ImmutableEndpoint getImmutableEndpoint(String endpointName)
        throws IOException
    {
        ImmutableEndpoint endpoint = null;

        Object o = muleContext.getRegistry().lookupObject(endpointName);
        if (o instanceof ImmutableEndpoint)
        {
            // For Inbound and Outbound Endpoints
            endpoint = (ImmutableEndpoint) o;

        }
        else if (o instanceof EndpointBuilder)
        {
            // For Endpoint-references
            EndpointBuilder eb = (EndpointBuilder) o;
            try
            {
                endpoint = eb.buildInboundEndpoint();
            }
            catch (Exception e)
            {
                throw new IOException(e.getMessage());
            }
        }
        return endpoint;
    }

    protected void remoteChmod(SftpClient client,
                               String endpointName,
                               int permissions) throws SftpException
    {
        ChannelSftp channelSftp = client.getChannelSftp();

        ImmutableEndpoint endpoint = (ImmutableEndpoint) muleContext.getRegistry().lookupObject(endpointName);
        EndpointURI endpointURI = endpoint.getEndpointURI();

        // RW - so that we can do initial cleanup
        channelSftp.chmod(permissions, client.getAbsolutePath(endpointURI.getPath()));
    }

    /**
     * Initiates a list of sftp-endpoint-directories. Ensures that affected services
     * are stopped during the initiation.
     *
     * @param serviceNames
     * @param endpointNames
     * @throws Exception
     */
    protected void initEndpointDirectories(String[] serviceNames, String[] endpointNames) throws Exception
    {

        // Stop all named services
        List<Service> services = new ArrayList<Service>();
        for (String serviceName : serviceNames)
        {
            try
            {
                Service service = muleContext.getRegistry().lookupService(serviceName);
                service.stop();
                services.add(service);
            }
            catch (Exception e)
            {
                LOGGER.error("Error '" + e.getMessage() + "' occured while stopping the service "
                             + serviceName + ". Perhaps the service did not exist in the config?");
                throw e;
            }
        }

        // Now init the directory for each named endpoint, one by one
        for (String endpointName : endpointNames)
        {
            initEndpointDirectory(endpointName);
        }

        // We are done, startup the services again so that the test can begin...
        for (Service service : services)
        {
            service.start();
        }
    }

    /**
     * Ensures that the directory exists and is writable by deleting the directory
     * and then recreate it.
     *
     * @param endpointName
     * @throws org.mule.api.MuleException
     * @throws java.io.IOException
     * @throws com.jcraft.jsch.SftpException
     */
    protected void initEndpointDirectory(String endpointName)
        throws MuleException, IOException, SftpException
    {
        SftpClient client = getSftpClient(endpointName);
        try
        {
            ChannelSftp channelSftp = client.getChannelSftp();
            try
            {
                recursiveDelete(client, endpointName, "");
            }
            catch (IOException e)
            {
                if (LOGGER.isErrorEnabled())
                    LOGGER.error("Failed to recursivly delete endpoint " + endpointName, e);
            }

            String path = getPathByEndpoint(client, endpointName);
            channelSftp.mkdir(path);
        }
        finally
        {
            client.disconnect();
            if (LOGGER.isDebugEnabled()) LOGGER.debug("Done init endpoint directory: " + endpointName);
        }
    }

    /**
     * Helper method for initiating a test and wait for the test to complete. The
     * method sends a file to an inbound endpoint and waits for a dispatch event on a
     * outbound endpoint, i.e. that the file has been consumed by the inbound
     * endpoint and that the content of the file has been sent to the outgoing
     * endpoint.
     *
     * @param p where inboundEndpoint and outboundEndpoint are mandatory, @see
     *            DispatchParameters for details.
     */
    @SuppressWarnings("rawtypes")
    protected void dispatchAndWaitForDelivery(final DispatchParameters p)
    {
        // Declare countdown latch and listener
        final CountDownLatch latch = new CountDownLatch(1);
        EndpointMessageNotificationListener listener = null;

        try
        {
            // Next create a listener that listens for dispatch events on the
            // outbound endpoint
            listener = new EndpointMessageNotificationListener()
            {
                @Override
                public void onNotification(ServerNotification notification)
                {

                    // Only care about EndpointMessageNotification
                    if (notification instanceof EndpointMessageNotification)
                    {
                        EndpointMessageNotification endpointNotification = (EndpointMessageNotification) notification;

                        // Extract action and name of the endpoint
                        int action = endpointNotification.getAction();
                        String endpoint = endpointNotification.getEndpoint();

                        // If it is a dispatch event on our outbound endpoint then
                        // countdown the latch.
                        if ((action == MESSAGE_DISPATCHED || action == MESSAGE_SENT)
                            && endpoint.equals(p.getOutboundEndpoint()))
                        {
                            if (LOGGER.isDebugEnabled())
                                LOGGER.debug("Expected notification received on " + p.getOutboundEndpoint()
                                             + " (action: " + action + "), time to countdown the latch");
                            latch.countDown();
                        }
                    }
                }
            };

            // Now register the listener
            muleContext.getNotificationManager().addListener(listener);

            // Initiate the test by sending a file to the SFTP server, which the
            // inbound-endpoint then can pick up

            // Prepare message headers, set filename-header and if supplied any
            // headers supplied in the call.
            Map<String, Object> headers = new HashMap<String, Object>();
            headers.put("filename", p.getFilename());

            if (p.getHeaders() != null)
            {
                headers.putAll(p.getHeaders());
            }

            // Setup connect string and perform the actual dispatch
            String connectString = (p.getSftpConnector() == null) ? "" : "?connector=" + p.getSftpConnector();
            org.mule.api.client.MuleClient muleClient = muleContext.getClient();
            muleClient.dispatch(getAddressByEndpoint(p.getInboundEndpoint()) + connectString,
                TEST_MESSAGE, headers);

            // Wait for the delivery to occur...
            if (LOGGER.isDebugEnabled()) LOGGER.debug("Waiting for file to be delivered to the endpoint...");
            boolean workDone = latch.await(p.getTimeout(), TimeUnit.MILLISECONDS);
            if (LOGGER.isDebugEnabled())
                LOGGER.debug((workDone)
                                       ? "File delivered, continue..."
                                       : "No file delivered, timeout occurred!");

            // Raise a fault if the test timed out
            // FIXME DZ: i dont this is necessary since we have an overall test
            // timeout
            // assertTrue("Test timed out. It took more than " + p.getTimeout() +
            // " milliseconds. If this error occurs the test probably needs a longer time out (on your computer/network)",
            // workDone);

        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("An unexpected error occurred: " + e.getMessage());

        }
        finally
        {
            // Always remove the listener if created
            if (listener != null)
            {
                muleContext.getNotificationManager().removeListener(listener);
            }
        }
    }

    protected Exception dispatchAndWaitForException(final DispatchParameters p,
                                                    String expectedFailingConnector)
    {
        return dispatchAndWaitForException(p, expectedFailingConnector, null);
    }

    /**
     * Helper method for initiating a test and wait for an exception to be caught by
     * the sftp-connector.
     *
     * @param p where sftpConnector and inboundEndpoint are mandatory, @see
     *            DispatchParameters for details.
     */
    protected Exception dispatchAndWaitForException(final DispatchParameters p,
                                                    String expectedFailingConnector,
                                                    String serviceName)
    {
        // Declare countdown latch and listener
        final CountDownLatch latch = new CountDownLatch(1);
        SystemExceptionHandler listener = null;
        MessagingExceptionHandler messagingListener = null;
        SystemExceptionHandler currentExceptionListener = null;
        MessagingExceptionHandler currentMessagingListener = null;
        final ValueHolder<Exception> exceptionHolder = new ValueHolder<Exception>();

        try
        {
            // Next create a listener that listens for exception on the
            // sftp-connector
            listener = new SystemExceptionHandler()
            {
                @Override
                public void handleException(Exception e, RollbackSourceCallback rollbackMethod)
                {
                    exceptionHolder.value = e;
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug("Expected exception occurred: " + e.getMessage()
                                     + ", time to countdown the latch");
                    latch.countDown();
                }

                @Override
                public void handleException(Exception exception)
                {
                    handleException(exception, null);
                }
            };

            messagingListener = new MessagingExceptionHandler()
            {
                @Override
                public MuleEvent handleException(Exception e, MuleEvent event)
                {
                    exceptionHolder.value = e;
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug("Expected exception occurred: " + e.getMessage()
                                     + ", time to countdown the latch");
                    latch.countDown();
                    return event;
                }
            };

            currentMessagingListener = muleContext.getRegistry()
                .lookupService(serviceName)
                .getExceptionListener();
            muleContext.getRegistry().lookupService(serviceName).setExceptionListener(messagingListener);

            // Now register an exception-listener on the connector that expects to
            // fail
            currentExceptionListener = muleContext.getExceptionListener();
            muleContext.setExceptionListener(listener);

            // Initiate the test by sending a file to the SFTP server, which the
            // inbound-endpoint then can pick up

            // Prepare message headers, set filename-header and if supplied any
            // headers supplied in the call.
            Map<String, Object> headers = new HashMap<String, Object>();
            headers.put("filename", p.getFilename());

            if (p.getHeaders() != null)
            {
                headers.putAll(p.getHeaders());
            }

            // Setup connect string and perform the actual dispatch
            String connectString = (p.getSftpConnector() == null) ? "" : "?connector=" + p.getSftpConnector();
            org.mule.api.client.MuleClient muleClient = muleContext.getClient();
            muleClient.dispatch(getAddressByEndpoint(p.getInboundEndpoint()) + connectString,
                TEST_MESSAGE, headers);

            // Wait for the exception to occur...
            if (LOGGER.isDebugEnabled()) LOGGER.debug("Waiting for an exception to occur...");
            boolean workDone = latch.await(p.getTimeout(), TimeUnit.MILLISECONDS);
            if (LOGGER.isDebugEnabled())
                LOGGER.debug((workDone)
                                       ? "Exception occurred, continue..."
                                       : "No exception, instead a timeout occurred!");

            // Raise a fault if the test timed out
            assertTrue(
                "Test timed out. It took more than "
                                + p.getTimeout()
                                + " milliseconds. If this error occurs the test probably needs a longer time out (on your computer/network)",
                workDone);

        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("An unexpected error occurred: " + e.getMessage());

        }
        finally
        {
            // Always reset the current listener
            muleContext.setExceptionListener(currentExceptionListener);
            muleContext.getRegistry().lookupService(serviceName).setExceptionListener(
                currentMessagingListener);
        }

        return exceptionHolder.value;
    }

    protected void recursiveDeleteInLocalFilesystem(File parent) throws IOException
    {
        // If this file is a directory then first delete all its children
        if (parent.isDirectory())
        {
            for (File child : parent.listFiles())
            {
                recursiveDeleteInLocalFilesystem(child);
            }
        }

        // Now delete this file, but first check write permissions on its parent...
        File parentParent = parent.getParentFile();

        if (!parentParent.canWrite())
        {
            if (!parentParent.setWritable(true))
            {
                throw new IOException("Failed to set readonly-folder: " + parentParent + " to writeable");
            }
        }

        if (parent.exists())
        {
            if (!parent.setWritable(true))
            {
                throw new IOException("Failed to set readonly-folder: " + parent + " to writeable");
            }

            if (!FileUtils.deleteFile(parent))
            {
                throw new IOException("Failed to delete folder: " + parent);
            }
        }
    }

    /**
     * Asserts that there are no files found on the path <i>path</i>.
     *
     * @param path The path in the local filesystem to check
     * @throws IOException Exception
     */
    protected void assertNoFilesInLocalFilesystem(String path) throws IOException
    {
        assertFilesInLocalFilesystem(path, new String[]{});
    }

    /**
     * Asserts that no files are found on the path that the <i>endpointName</i> use.
     * @param endpointName The endpoint name
     *
     * @throws IOException Exception
     */
    protected void assertNoFilesInEndpoint(String endpointName) throws IOException
    {
        assertFilesInEndpoint(endpointName, new String[]{});
    }

    /**
     * Asserts that no files are found on the sub directory <i>subDirectory</i> under
     * the path that <i>endpointName</i> use.
     * @param endpointName The endpoint name
     * @param subDirectory The sub directory
     *
     * @throws IOException Exception
     */
    protected void assertNoFilesInEndpoint(String endpointName, String subDirectory)
        throws IOException
    {
        assertFilesInEndpoint(endpointName, subDirectory, new String[]{});
    }

    /**
     * Asserts that only the <i>expectedFile</i> is found on the path <i>path</i>,
     * where filenames can be expressed as a regular expression.
     *
     * @param path The path in the local filesystem to check
     * @param expectedFile Expected file
     * @throws IOException Exception
     */
    protected void assertFilesInLocalFilesystem(String path, String expectedFile) throws IOException
    {
        assertFilesInLocalFilesystem(path, new String[]{expectedFile});
    }

    /**
     * Asserts that only the <i>expectedFiles</i> are found on the path <i>path</i>,
     * where filenames can be expressed as a regular expression.
     *
     * @param path The path in the local filesystem to check
     * @param expectedFiles Expected files
     * @throws IOException Exception
     */
    protected void assertFilesInLocalFilesystem(String path, String[] expectedFiles) throws IOException
    {

        File parent = new File(path);
        String[] files = parent.list();

        assertFilesInFileArray(path, expectedFiles, files);
    }

    /**
     * Asserts that only the <i>expectedFile</i> is found on the path that the
     * <i>endpointName</i> use, where filenames can be expressed as a regular
     * expression.
     * @param endpointName The endpoint name
     * @param expectedFile Expected file
     *
     * @throws IOException Exception
     */
    protected void assertFilesInEndpoint(String endpointName, String expectedFile)
        throws IOException
    {
        assertFilesInEndpoint(endpointName, null, new String[]{expectedFile});
    }

    /**
     * Asserts that only the <i>expectedFiles</i> are found on the path that the
     * <i>endpointName</i> use, where filenames can be expressed as a regular
     * expression.
     * @param endpointName The endpoint name
     * @param expectedFiles Expected files
     *
     * @throws IOException Exception
     */
    protected void assertFilesInEndpoint(String endpointName, String[] expectedFiles)
        throws IOException
    {
        assertFilesInEndpoint(endpointName, null, expectedFiles);
    }

    /**
     * Asserts that only the <i>expectedFile</i> is found on the sub directory
     * <i>subDirectory</i> under the path that <i>endpointName</i> use, where
     * filenames can be expressed as a regular expression.
     * @param endpointName The endpoint name
     * @param subDirectory The sub directory
     * @param expectedFile Expected files
     *
     * @throws IOException Exception
     */
    protected void assertFilesInEndpoint(String endpointName,
                                         String subDirectory,
                                         String expectedFile) throws IOException
    {
        assertFilesInEndpoint(endpointName, subDirectory, new String[]{expectedFile});
    }

    /**
     * Asserts that only the <i>expectedFiles</i> are found on the sub directory
     * <i>subDirectory</i> under the path that <i>endpointName</i> use, where
     * filenames can be expressed as a regular expression.
     * @param endpointName The endpoint name
     * @param subDirectory The sub directory
     * @param expectedFiles Expected files
     *
     * @throws IOException Exception
     */
    protected void assertFilesInEndpoint(String endpointName,
                                         String subDirectory,
                                         String[] expectedFiles) throws IOException
    {
        SftpClient client = getSftpClient(endpointName);
        ImmutableEndpoint tEndpoint = muleContext.getRegistry().lookupObject(endpointName);
        try
        {
            String path = tEndpoint.getEndpointURI().getPath();
            if (subDirectory != null)
            {
                path += '/' + subDirectory;
            }
            assertFilesInPath(client, path, expectedFiles);
        }
        finally
        {
            client.disconnect();
        }
    }

    /**
     * Asserts that only the <i>expectedFiles</i> are found on the path <i>path</i>,
     * where filenames can be expressed as a regular expression.
     *
     * @param client SftpClient
     * @param path The path to check
     * @param expectedFiles Expected files
     * @throws IOException Exception
     */
    private void assertFilesInPath(SftpClient client, String path, String[] expectedFiles)
        throws IOException
    {

        client.changeWorkingDirectory(client.getAbsolutePath(path));
        String[] files = client.listFiles();

        assertFilesInFileArray(path, expectedFiles, files);
    }

    /**
     * Asserts that only the <i>expectedFiles</i> are found in the file-array
     * <i>path</i>, where filenames can be expressed as a regular expression.
     *
     * @param path
     * @param expectedFiles
     * @param foundFiles
     */
    private void assertFilesInFileArray(String path, String[] expectedFiles, String[] foundFiles)
    {

        // First, make a list of the array of found files
        List<String> foundFileList = new ArrayList<String>(foundFiles.length);
        foundFileList.addAll(Arrays.asList(foundFiles));
        List<String> missingExpectedFiles = new ArrayList<String>();

        // lookup each expected file in the list of found files and remove each found
        // file that match the expected file
        // Note that the expected file can contain a regexp
        for (String expectedFile : expectedFiles)
        {
            String foundFile = lookupListByRegexp(foundFileList, expectedFile);
            if (foundFile != null)
            {
                foundFileList.remove(foundFile);
            }
            else
            {
                missingExpectedFiles.add(expectedFile);
            }
        }
        // Check if that no remaining files are left in the list of found files, i.e.
        // unwanted found files
        assertTrue("Expected files not found on path " + path + ". File(s):" + missingExpectedFiles,
            missingExpectedFiles.size() == 0);
        assertTrue("The following file(s) was found but not expected: " + foundFileList + " on path " + path,
            foundFileList.size() == 0);
    }

    /**
     * Return the first string in a string-list that matches the regexp
     *
     * @param list
     * @param regexp
     * @return the first string that match the regexp or null if no match
     */
    private String lookupListByRegexp(List<String> list, String regexp)
    {

        // Look for matches of the regexp in the list
        for (String value : list)
        {
            if (value.matches(regexp))
            {
                // Found it, return the full string that matched
                return value;
            }
        }

        // Noop, nothing found, return null
        return null;
    }

    /**
     * Helper class for dynamic assignment of parameters to the method
     * dispatchAndWaitForDelivery() Only inboundEndpoint and outboundEndpoint are
     * mandatory, the rest of the parameters are optional.
     *
     * @author Magnus Larsson
     */
    public class DispatchParameters
    {
        /**
         * Optional name of sftp-connector, if not supplied it is assumed that only
         * one sftp-conector is speficied in the mule configuration. If more than one
         * sftp-connector is specified this paramter has to be specified to point out
         * what connector to use for the dispatch.
         */
        private String sftpConnector = null;

        /**
         * Mandatory name of the inbound endpoint, i.e. where to dispatch the file
         */
        private String inboundEndpoint = null;

        /**
         * Optional message headers
         */
        private Map<String, String> headers = null;

        /**
         * Optional content of the file, if not specified then it defaults to
         * AbstractMuleTestCase.TEST_MESSAGE.
         */
        private String message = TEST_MESSAGE;

        /**
         * Optional name of the file, defaults to FILE_NAME
         */
        private String filename = FILENAME;

        /**
         * Mandatory name of the outbound endpoint, i.e. where we will wait for a
         * message to be delivered to in the end
         */
        private String outboundEndpoint = null;

        /**
         * Optional timeout for how long we will wait for a message to be delivered
         * to the outbound endpoint
         */
        private long timeout = 100000;

        public DispatchParameters(String inboundEndpoint, String outboundEndpoint)
        {
            this.inboundEndpoint = inboundEndpoint;
            this.outboundEndpoint = outboundEndpoint;
        }

        public String getSftpConnector()
        {
            return sftpConnector;
        }

        public void setSftpConnector(String sftpConnector)
        {
            this.sftpConnector = sftpConnector;
        }

        public String getInboundEndpoint()
        {
            return inboundEndpoint;
        }

        public void setInboundEndpoint(String inboundEndpoint)
        {
            this.inboundEndpoint = inboundEndpoint;
        }

        public Map<String, String> getHeaders()
        {
            return headers;
        }

        public void setHeaders(Map<String, String> headers)
        {
            this.headers = headers;
        }

        public String getMessage()
        {
            return message;
        }

        public void setMessage(String message)
        {
            this.message = message;
        }

        public String getFilename()
        {
            return filename;
        }

        public void setFilename(String filename)
        {
            this.filename = filename;
        }

        public String getOutboundEndpoint()
        {
            return outboundEndpoint;
        }

        public void setOutboundEndpoint(String outboundEndpoint)
        {
            this.outboundEndpoint = outboundEndpoint;
        }

        public long getTimeout()
        {
            return timeout;
        }

        public void setTimeout(long timeout)
        {
            this.timeout = timeout;
        }
    }

    /**
     * Check that all of the connectors are running. I don't know if this makes a
     * difference in test reliability per se; it may just delay the start of the test
     * long enough for the MuleContext to be usable.
     */
    public void checkConnectors()
    {
        assertTrue("context is not started", muleContext.getLifecycleManager().getState().isStarted());
        Map<String, Connector> connectorMap = muleContext.getRegistry().lookupByType(Connector.class);
        Map<String, Service> serviceMap = muleContext.getRegistry().lookupByType(Service.class);
        Map<String, Model> modelMap = muleContext.getRegistry().lookupByType(Model.class);

        Iterator<Map.Entry<String, Connector>> connectorItr = connectorMap.entrySet().iterator();
        while (connectorItr.hasNext())
        {
            Map.Entry<String, Connector> pairs = connectorItr.next();
            LOGGER.debug("checking connector : " + pairs.getKey());
            assertTrue(pairs.getKey() + " is not started", pairs.getValue().isStarted());
        }

        Iterator<Map.Entry<String, Service>> serviceItr = serviceMap.entrySet().iterator();
        while (serviceItr.hasNext())
        {
            Map.Entry<String, Service> pairs = serviceItr.next();
            assertTrue(pairs.getKey() + " is not started", pairs.getValue().isStarted());
        }

        Iterator<Map.Entry<String, Model>> modelItr = modelMap.entrySet().iterator();
        while (modelItr.hasNext())
        {
            Map.Entry<String, Model> pairs = modelItr.next();
            assertTrue(pairs.getKey() + " is not started", pairs.getValue().getLifecycleState().isStarted());
        }
    }


    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        super.doSetUpBeforeMuleContextCreation();
        if (startServerOnStartUp())
        {
            startSftpServerAndClient();
        }
    }

    /**
      * @return true if the SFTP server and client must be created as setup of the test case
     */
    protected boolean startServerOnStartUp()
    {
        return true;
    }

    protected void startSftpServerAndClient() throws IOException
    {
        sftpServer = new SftpServer(this.port.getNumber());
        sftpServer.start();
        sftpClient = getSftpClient(HOST, port.getNumber(), USER, PASSWORD);
    }

    protected void stopSftpServerAndClient() throws Exception
    {
        sftpClient.disconnect();
        sftpServer.stop();
    }

}
