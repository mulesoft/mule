/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.ConnectorException;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.MessageReceiver;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.model.streaming.CallbackOutputStream;
import org.mule.transport.AbstractConnector;
import org.mule.transport.ConnectException;
import org.mule.transport.file.ExpressionFilenameParser;
import org.mule.transport.file.FilenameParser;
import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

public class FtpConnector extends AbstractConnector
{

    public static final String FTP = "ftp";

    // endpoint properties
    public static final int DEFAULT_POLLING_FREQUENCY = 1000;
    public static final String PROPERTY_OUTPUT_PATTERN = "outputPattern"; // outbound only
    public static final String PROPERTY_PASSIVE_MODE = "passive";
    public static final String PROPERTY_BINARY_TRANSFER = "binary";

    // message properties
    public static final String PROPERTY_FILENAME = "filename";


    /**
     *  TODO it makes sense to have a type-safe adapter for FTP specifically, but without
     *  Java 5's covariant return types the benefits are diminished. Keeping it simple for now.
     */
    public static final String DEFAULT_FTP_CONNECTION_FACTORY_CLASS = "org.mule.transport.ftp.FtpConnectionFactory";

    /**
     * Time in milliseconds to poll. On each poll the poll() method is called
     */
    private long pollingFrequency;

    private String outputPattern;

    private FilenameParser filenameParser = new ExpressionFilenameParser();

    private boolean passive = true;

    private boolean binary = true;

    /** Streaming is off by default until MULE-3192 gets fixed */
    private boolean streaming = false;

    private int connectionTimeout = 0;

    private Map<String, ObjectPool> pools;

    private String connectionFactoryClass = DEFAULT_FTP_CONNECTION_FACTORY_CLASS;

    public FtpConnector(MuleContext context)
    {
        super(context);
    }

    public String getProtocol()
    {
        return FTP;
    }

    @Override
    public MessageReceiver createReceiver(FlowConstruct flowConstruct, InboundEndpoint endpoint) throws Exception
    {
        List<?> args = getReceiverArguments(endpoint.getProperties());
        return serviceDescriptor.createMessageReceiver(this, flowConstruct, endpoint, args.toArray());
    }

    protected List<?> getReceiverArguments(Map endpointProperties)
    {
        List<Object> args = new ArrayList<Object>();

        long polling = getPollingFrequency();
        if (endpointProperties != null)
        {
            // Override properties on the endpoint for the specific endpoint
            String tempPolling = (String) endpointProperties.get(PROPERTY_POLLING_FREQUENCY);
            if (tempPolling != null)
            {
                polling = Long.parseLong(tempPolling);
            }
        }
        if (polling <= 0)
        {
            polling = DEFAULT_POLLING_FREQUENCY;
        }
        logger.debug("set polling frequency to " + polling);
        args.add(polling);

        return args;
    }

    /**
     * @return Returns the pollingFrequency.
     */
    public long getPollingFrequency()
    {
        return pollingFrequency;
    }

    /**
     * @param pollingFrequency The pollingFrequency to set.
     */
    public void setPollingFrequency(long pollingFrequency)
    {
        this.pollingFrequency = pollingFrequency;
    }

    /**
     * Getter for property 'connectionFactoryClass'.
     *
     * @return Value for property 'connectionFactoryClass'.
     */
    public String getConnectionFactoryClass()
    {
        return connectionFactoryClass;
    }

    /**
     * Setter for property 'connectionFactoryClass'. Should be an instance of
     * {@link FtpConnectionFactory}.
     *
     * @param connectionFactoryClass Value to set for property 'connectionFactoryClass'.
     */
    public void setConnectionFactoryClass(final String connectionFactoryClass)
    {
        this.connectionFactoryClass = connectionFactoryClass;
    }

    public FTPClient getFtp(EndpointURI uri) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(">>> retrieving client for " + uri);
        }
        return (FTPClient) getFtpPool(uri).borrowObject();
    }

    public void releaseFtp(EndpointURI uri, FTPClient client) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("<<< releasing client for " + uri);
        }
        if (dispatcherFactory.isCreateDispatcherPerRequest())
        {
            destroyFtp(uri, client);
        }
        else
        {
            getFtpPool(uri).returnObject(client);
        }
    }

    public void destroyFtp(EndpointURI uri, FTPClient client) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("<<< destroying client for " + uri);
        }
        try
        {
            getFtpPool(uri).invalidateObject(client);
        }
        catch (Exception e)
        {
            // no way to test if pool is closed except try to access it
            logger.debug(e.getMessage());
        }
    }

    protected synchronized ObjectPool getFtpPool(EndpointURI uri)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("=== get pool for " + uri);
        }
        String key = uri.getUser() + ":" + uri.getPassword() + "@" + uri.getHost() + ":" + uri.getPort();
        ObjectPool pool = pools.get(key);
        if (pool == null)
        {
            try
            {
                FtpConnectionFactory connectionFactory =
                        (FtpConnectionFactory) ClassUtils.instanciateClass(getConnectionFactoryClass(),
                                                                            new Object[] {uri}, getClass());
                connectionFactory.setConnectionTimeout(connectionTimeout);
                GenericObjectPool genericPool = createPool(connectionFactory);
                pools.put(key, genericPool);
                pool = genericPool;
            }
            catch (Exception ex)
            {
                throw new MuleRuntimeException(
                        MessageFactory.createStaticMessage("Hmm, couldn't instanciate FTP connection factory."), ex);
            }
        }
        return pool;
    }

    protected GenericObjectPool createPool(FtpConnectionFactory connectionFactory)
    {
        GenericObjectPool genericPool = new GenericObjectPool(connectionFactory);
        byte poolExhaustedAction = ThreadingProfile.DEFAULT_POOL_EXHAUST_ACTION;

        ThreadingProfile receiverThreadingProfile = this.getReceiverThreadingProfile();
        if (receiverThreadingProfile != null)
        {
            int threadingProfilePoolExhaustedAction = receiverThreadingProfile.getPoolExhaustedAction();
            if (threadingProfilePoolExhaustedAction == ThreadingProfile.WHEN_EXHAUSTED_WAIT)
            {
                poolExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;
            }
            else if (threadingProfilePoolExhaustedAction == ThreadingProfile.WHEN_EXHAUSTED_ABORT)
            {
                poolExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_FAIL;
            }
            else if (threadingProfilePoolExhaustedAction == ThreadingProfile.WHEN_EXHAUSTED_RUN)
            {
                poolExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
            }
        }

        genericPool.setWhenExhaustedAction(poolExhaustedAction);
        genericPool.setTestOnBorrow(isValidateConnections());
        return genericPool;
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        if (filenameParser != null)
        {
            filenameParser.setMuleContext(muleContext);
        }

        try
        {
            Class<?> objectFactoryClass = ClassUtils.loadClass(this.connectionFactoryClass, getClass());
            if (!FtpConnectionFactory.class.isAssignableFrom(objectFactoryClass))
            {
                throw new InitialisationException(MessageFactory.createStaticMessage(
                        "FTP connectionFactoryClass is not an instance of org.mule.transport.ftp.FtpConnectionFactory"),
                        this);
            }
        }
        catch (ClassNotFoundException e)
        {
            throw new InitialisationException(e, this);
        }

        pools = new HashMap<String, ObjectPool>();
    }

    @Override
    protected void doDispose()
    {
        // template method
    }

    @Override
    protected void doConnect() throws Exception
    {
        // template method
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // template method
    }

    @Override
    protected void doStart() throws MuleException
    {
        // template method
    }

    @Override
    protected void doStop() throws MuleException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Stopping all pools");
        }
        try
        {
            for (ObjectPool pool : pools.values())
            {
                pool.close();
            }
        }
        catch (Exception e)
        {
            throw new ConnectorException(CoreMessages.failedToStop("FTP Connector"), this, e);
        }
        finally
        {
            pools.clear();
        }
    }

    /**
     * @return Returns the outputPattern.
     */
    public String getOutputPattern()
    {
        return outputPattern;
    }

    /**
     * @param outputPattern The outputPattern to set.
     */
    public void setOutputPattern(String outputPattern)
    {
        this.outputPattern = outputPattern;
    }

    /**
     * @return Returns the filenameParser.
     */
    public FilenameParser getFilenameParser()
    {
        return filenameParser;
    }

    /**
     * @param filenameParser The filenameParser to set.
     */
    public void setFilenameParser(FilenameParser filenameParser)
    {
        this.filenameParser = filenameParser;
        if (filenameParser != null)
        {
            filenameParser.setMuleContext(muleContext);
        }
    }

    /**
     * Getter for FTP passive mode.
     *
     * @return true if using FTP passive mode
     */
    public boolean isPassive()
    {
        return passive;
    }

    /**
     * Setter for FTP passive mode.
     *
     * @param passive passive mode flag
     */
    public void setPassive(final boolean passive)
    {
        this.passive = passive;
    }

    public void setConnectionTimeout(int connectionTimeout)
    {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Passive mode is OFF by default. The value is taken from the connector
     * settings. In case there are any overriding properties set on the endpoint,
     * those will be used.
     *
     * @see #setPassive(boolean)
     */
    public void enterActiveOrPassiveMode(FTPClient client, ImmutableEndpoint endpoint)
    {
        // well, no endpoint URI here, as we have to use the most common denominator
        // in API :(
        final String passiveString = (String)endpoint.getProperty(FtpConnector.PROPERTY_PASSIVE_MODE);
        if (passiveString == null)
        {
            // try the connector properties then
            if (isPassive())
            {
                if (logger.isTraceEnabled())
                {
                    logger.trace("Entering FTP passive mode");
                }
                client.enterLocalPassiveMode();
            }
            else
            {
                if (logger.isTraceEnabled())
                {
                    logger.trace("Entering FTP active mode");
                }
                client.enterLocalActiveMode();
            }
        }
        else
        {
            // override with endpoint's definition
            final boolean passiveMode = Boolean.valueOf(passiveString).booleanValue();
            if (passiveMode)
            {
                if (logger.isTraceEnabled())
                {
                    logger.trace("Entering FTP passive mode (endpoint override)");
                }
                client.enterLocalPassiveMode();
            }
            else
            {
                if (logger.isTraceEnabled())
                {
                    logger.trace("Entering FTP active mode (endpoint override)");
                }
                client.enterLocalActiveMode();
            }
        }
    }

    /**
     * Getter for FTP transfer type.
     *
     * @return true if using FTP binary type
     */
    public boolean isBinary()
    {
        return binary;
    }

    /**
     * Setter for FTP transfer type.
     *
     * @param binary binary type flag
     */
    public void setBinary(final boolean binary)
    {
        this.binary = binary;
    }

    /**
     * Transfer type is BINARY by default. The value is taken from the connector
     * settings. In case there are any overriding properties set on the endpoint,
     * those will be used. <p/> The alternative type is ASCII. <p/>
     *
     * @see #setBinary(boolean)
     */
    public void setupFileType(FTPClient client, ImmutableEndpoint endpoint) throws Exception
    {
        int type;

        // well, no endpoint URI here, as we have to use the most common denominator
        // in API :(
        final String binaryTransferString = (String)endpoint.getProperty(FtpConnector.PROPERTY_BINARY_TRANSFER);
        if (binaryTransferString == null)
        {
            // try the connector properties then
            if (isBinary())
            {
                if (logger.isTraceEnabled())
                {
                    logger.trace("Using FTP BINARY type");
                }
                type = org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE;
            }
            else
            {
                if (logger.isTraceEnabled())
                {
                    logger.trace("Using FTP ASCII type");
                }
                type = org.apache.commons.net.ftp.FTP.ASCII_FILE_TYPE;
            }
        }
        else
        {
            // override with endpoint's definition
            final boolean binaryTransfer = Boolean.valueOf(binaryTransferString).booleanValue();
            if (binaryTransfer)
            {
                if (logger.isTraceEnabled())
                {
                    logger.trace("Using FTP BINARY type (endpoint override)");
                }
                type = org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE;
            }
            else
            {
                if (logger.isTraceEnabled())
                {
                    logger.trace("Using FTP ASCII type (endpoint override)");
                }
                type = org.apache.commons.net.ftp.FTP.ASCII_FILE_TYPE;
            }
        }

        client.setFileType(type);
    }

    /**
     * Well get the output stream (if any) for this type of transport. Typically this
     * will be called only when Streaming is being used on an outbound endpoint
     *
     * @param endpoint the endpoint that releates to this Dispatcher
     * @param event the current event being processed
     * @return the output stream to use for this request or null if the transport
     *         does not support streaming
     */
    @Override
    public OutputStream getOutputStream(OutboundEndpoint endpoint, MuleEvent event) throws MuleException
    {
        try
        {
            final EndpointURI uri = endpoint.getEndpointURI();
            String filename = getFilename(endpoint, event.getMessage());

            final FTPClient client;
            try
            {
                client = this.createFtpClient(endpoint);
            }
            catch (Exception e)
            {
                throw new ConnectException(e, this);
            }

            try
            {
                OutputStream out = client.storeFileStream(filename);
                if (out == null)
                {
                    throw new IOException("FTP operation failed: " + client.getReplyString());
                }

                return new CallbackOutputStream(out,
                        new CallbackOutputStream.Callback()
                        {
                            public void onClose() throws Exception
                            {
                                try
                                {
                                    if (!client.completePendingCommand())
                                    {
                                        client.logout();
                                        client.disconnect();
                                        throw new IOException("FTP Stream failed to complete pending request");
                                    }
                                }
                                finally
                                {
                                    releaseFtp(uri, client);
                                }
                            }
                        });
            }
            catch (Exception e)
            {
                logger.debug("Error getting output stream: ", e);
                releaseFtp(uri, client);
                throw e;
            }
        }
        catch (ConnectException ce)
        {
            // Don't wrap a ConnectException, otherwise the retry policy will not go into effect.
            throw ce;
        }
        catch (Exception e)
        {
            throw new DispatchException(CoreMessages.streamingFailedNoStream(), event, endpoint, e);
        }
    }

    private String getFilename(ImmutableEndpoint endpoint, MuleMessage message) throws IOException
    {
        String filename = message.getOutboundProperty(FtpConnector.PROPERTY_FILENAME);
        String outPattern = (String) endpoint.getProperty(FtpConnector.PROPERTY_OUTPUT_PATTERN);
        if (outPattern == null)
        {
            outPattern = message.getOutboundProperty(FtpConnector.PROPERTY_OUTPUT_PATTERN, getOutputPattern());
        }
        if (outPattern != null || filename == null)
        {
            filename = generateFilename(message, outPattern);
        }
        if (filename == null)
        {
            throw new IOException("Filename is null");
        }
        return filename;
    }

    private String generateFilename(MuleMessage message, String pattern)
    {
        if (pattern == null)
        {
            pattern = getOutputPattern();
        }
        return getFilenameParser().getFilename(message, pattern);
    }

    /**
     * Creates a new FTPClient that logs in and changes the working directory using the data
     * provided in <code>endpoint</code>.
     */
    protected FTPClient createFtpClient(ImmutableEndpoint endpoint) throws Exception
    {
        EndpointURI uri = endpoint.getEndpointURI();
        FTPClient client = this.getFtp(uri);
        client.setDataTimeout(endpoint.getResponseTimeout());

        this.enterActiveOrPassiveMode(client, endpoint);
        this.setupFileType(client, endpoint);

        String path = uri.getPath();

        // only change directory if one was configured
        if (StringUtils.isNotBlank(path))
        {
            // MULE-2400: if the path begins with '~' we must strip the first '/' to make things
            // work with FTPClient
            if ((path.length() >= 2) && (path.charAt(1) == '~'))
            {
                path = path.substring(1);
            }

            //Checking if it is a file or a directory
            boolean isFile = this.isFile(endpoint, client);
            if (!isFile && !client.changeWorkingDirectory(path))
            {
                throw new IOException(MessageFormat.format("Failed to change working directory to {0}. Ftp error: {1}",
                                                           path, client.getReplyCode()));
            }
            else if (isFile)
            {
                // Changing the working directory to the parent folder, it should be better if
                // the ftpClient API would provide a way to retrieve the parent folder
                FTPFile[] listFiles = client.listFiles(path);
                String directory = path.replaceAll(listFiles[0].getName(), "");
                client.changeWorkingDirectory(directory);
            }
        }
        return client;
    }

    /**
     * Override this method to do extra checking on the file.
     */
    protected boolean validateFile(FTPFile file)
    {
        return true;
    }

    protected boolean isFile(ImmutableEndpoint endpoint, FTPClient client) throws IOException 
    {
          //Checking if it is a file or a directory
          String path = endpoint.getEndpointURI().getPath();
          FTPFile[] listFiles = client.listFiles(path);
          return listFiles.length == 1 && listFiles[0].isFile();
    }

    public boolean isStreaming()
    {
        return streaming;
    }

    public void setStreaming(boolean streaming)
    {
        this.streaming = streaming;
    }

}
