/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ftp;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.transport.ConnectorException;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.MessageReceiver;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.model.streaming.CallbackOutputStream;
import org.mule.transport.AbstractConnector;
import org.mule.transport.ConnectException;
import org.mule.transport.file.FilenameParser;
import org.mule.transport.file.SimpleFilenameParser;
import org.mule.util.ClassUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
    public static final String PROPERTY_POLLING_FREQUENCY = "pollingFrequency"; // inbound only
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

    private FilenameParser filenameParser = new SimpleFilenameParser();

    private boolean passive = true;

    private boolean binary = true;

    /**
     * Whether to test FTP connection on each take from pool.
     */
    private boolean validateConnections = true;

    /** Streaming is off by default until MULE-3192 gets fixed */
    private boolean streaming = false;
    
    private Map pools;

    private String connectionFactoryClass = DEFAULT_FTP_CONNECTION_FACTORY_CLASS;

    public String getProtocol()
    {
        return FTP;
    }

    public MessageReceiver createReceiver(Service service, InboundEndpoint endpoint) throws Exception
    {
        List args = getReceiverArguments(endpoint.getProperties());
        return serviceDescriptor.createMessageReceiver(this, service, endpoint, args.toArray());
    }

    protected List getReceiverArguments(Map endpointProperties)
    {
        List args = new ArrayList();
        
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
        args.add(new Long(polling));
        
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
        ObjectPool pool = (ObjectPool) pools.get(key);
        if (pool == null)
        {
            try
            {
                FtpConnectionFactory connectionFactory =
                        (FtpConnectionFactory) ClassUtils.instanciateClass(getConnectionFactoryClass(),
                                                                            new Object[] {uri}, getClass());
                pool = new GenericObjectPool(connectionFactory);
                ((GenericObjectPool) pool).setTestOnBorrow(this.validateConnections);
                pools.put(key, pool);
            }
            catch (Exception ex)
            {
                throw new MuleRuntimeException(
                        MessageFactory.createStaticMessage("Hmm, couldn't instanciate FTP connection factory."), ex);
            }
        }
        return pool;
    }


    protected void doInitialise() throws InitialisationException
    {
        try
        {
            Class objectFactoryClass = ClassUtils.loadClass(this.connectionFactoryClass, getClass());
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

        pools = new HashMap();
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect() throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

    protected void doStart() throws MuleException
    {
        // template method
    }

    protected void doStop() throws MuleException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Stopping all pools");
        }
        try
        {
            for (Iterator it = pools.values().iterator(); it.hasNext();)
            {
                ObjectPool pool = (ObjectPool)it.next();
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
     * Whether to test FTP connection on each take from pool.
     */
    public boolean isValidateConnections()
    {
        return validateConnections;
    }

    /**
     * Whether to test FTP connection on each take from pool. This takes care of a
     * failed (or restarted) FTP server at the expense of an additional NOOP command
     * packet being sent, but increases overall availability. <p/> Disable to gain
     * slight performance gain or if you are absolutely sure of the FTP server
     * availability. <p/> The default value is <code>true</code>
     */
    public void setValidateConnections(final boolean validateConnections)
    {
        this.validateConnections = validateConnections;
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
     * @param message the current message being processed
     * @return the output stream to use for this request or null if the transport
     *         does not support streaming
     * @throws org.mule.api.MuleException
     */
    public OutputStream getOutputStream(ImmutableEndpoint endpoint, MuleMessage message)
        throws MuleException
    {
        try
        {
            final EndpointURI uri = endpoint.getEndpointURI();
            String filename = getFilename(endpoint, message);

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
            throw new DispatchException(CoreMessages.streamingFailedNoStream(), message, endpoint, e);
        }
    }

    private String getFilename(ImmutableEndpoint endpoint, MuleMessage message) throws IOException
    {
        String filename = (String) message.getProperty(FtpConnector.PROPERTY_FILENAME);
        String outPattern = (String) endpoint.getProperty(FtpConnector.PROPERTY_OUTPUT_PATTERN);
        if (outPattern == null)
        {
            outPattern = message.getStringProperty(FtpConnector.PROPERTY_OUTPUT_PATTERN, getOutputPattern());
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

        this.enterActiveOrPassiveMode(client, endpoint);
        this.setupFileType(client, endpoint);

        String path = uri.getPath();
        // MULE-2400: if the path begins with '~' we must strip the first '/' to make things
        // work with FTPClient
        if ((path.length() >= 2) && (path.charAt(1) == '~'))
        {
            path = path.substring(1);
        }
        
        if (!client.changeWorkingDirectory(path))
        {
            throw new IOException(MessageFormat.format("Failed to change working directory to {0}. Ftp error: {1}",
                                                       path, new Integer(client.getReplyCode())));
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

    public boolean isStreaming()
    {
        return streaming;
    }

    public void setStreaming(boolean streaming)
    {
        this.streaming = streaming;
    }
}
