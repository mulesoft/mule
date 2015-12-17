/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.mule.transport.sftp.AuthenticationMethodValidator.validateAuthenticationMethods;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.ConnectorException;
import org.mule.api.transport.MessageReceiver;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.AbstractConnector;
import org.mule.transport.file.ExpressionFilenameParser;
import org.mule.transport.file.FilenameParser;
import org.mule.transport.sftp.notification.SftpNotifier;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * <code>SftpConnector</code> sends and receives file messages over sftp using jsch
 * library Improves on SFTP with VFS Connector in the following ways: 1. Streams
 * files instead of reading them into memory. The SftpMessageReceiver is a
 * "non-materializing stream receiver" which does not read the file to memory. The
 * SftpMessageDispatcher also never materializes the stream and delegates the jsch
 * library for materialization. 3. Uses jsch library directly instead of using VFS as
 * middle-man. 3. More explicit connection lifefecyle management. 4. Leverages sftp
 * stat to determine if a file size changes (simpler and also less memory intensive)
 */
public class SftpConnector extends AbstractConnector
{

    public static final String PROPERTY_POLLING_FREQUENCY = "pollingFrequency";
    public static final String PROPERTY_DIRECTORY = "directory";
    public static final String PROPERTY_OUTPUT_PATTERN = "outputPattern";
    public static final String PROPERTY_FILENAME = "filename";
    public static final String PROPERTY_ORIGINAL_FILENAME = "originalFilename";
    public static final String PROPERTY_SELECT_EXPRESSION = "selectExpression";
    public static final String PROPERTY_FILE_EXTENSION = "fileExtension";
    public static final String PROPERTY_INCLUDE_SUBFOLDERS = "includeSubfolders";
    public static final String PROPERTY_IDENTITY_FILE = "identityFile";
    public static final String PROPERTY_PASS_PHRASE = "passphrase";
    public static final String PROPERTY_FILE_AGE = "fileAge";
    public static final String PROPERTY_TEMP_DIR = "tempDir";
    public static final String PROPERTY_SIZE_CHECK_WAIT_TIME = "sizeCheckWaitTime";
    public static final String PROPERTY_ARCHIVE_DIR = "archiveDir";
    public static final String PROPERTY_ARCHIVE_TEMP_RECEIVING_DIR = "archiveTempReceivingDir";
    public static final String PROPERTY_ARCHIVE_TEMP_SENDING_DIR = "archiveTempSendingDir";
    public static final String PROPERTY_DUPLICATE_HANDLING = "duplicateHandling";
    public static final String PROPERTY_USE_TEMP_FILE_TIMESTAMP_SUFFIX = "useTempFileTimestampSuffix";
    public static final String PROPERTY_DUPLICATE_HANDLING_THROW_EXCEPTION = "throwException";
    public static final String PROPERTY_DUPLICATE_HANDLING_OVERWRITE = "overwrite";
    public static final String PROPERTY_DUPLICATE_HANDLING_APPEND = "append";
    public static final String PROPERTY_DUPLICATE_HANDLING_ASS_SEQ_NO = "addSeqNo";
    public static final String PROPERTY_MAX_CONNECTION_POOL_SIZE = "maxConnectionPoolSize";
    public static final String PROPERTY_KEEP_FILE_ON_ERROR = "keepFileOnError";

    public static final int DEFAULT_POLLING_FREQUENCY = 1000;

    /**
     * logger used by this class
     */
    protected final static Log logger = LogFactory.getLog(SftpConnector.class);

    private FilenameParser filenameParser = new ExpressionFilenameParser();

    private long pollingFrequency;
    private boolean autoDelete = true;
    private String outputPattern;

    private String identityFile;
    private String passphrase;

    private boolean checkFileAge = false;
    private long fileAge = 0;

    private String tempDirInbound = null;
    private String tempDirOutbound = null;

    private Map<EndpointURI, GenericObjectPool> pools = new HashMap<EndpointURI, GenericObjectPool>();

    private String duplicateHandling = null;
    private Boolean useTempFileTimestampSuffix = null;
    private Long sizeCheckWaitTime = null;
    private String archiveDir = "";
    private String archiveTempReceivingDir = "";
    private String archiveTempSendingDir = "";
    private String preferredAuthenticationMethods;

    /**
     * Should the file be kept if an error occurs when writing the file on the
     * outbound endpoint?
     */
    private Boolean keepFileOnError;

    /**
     * max pool size. 0 for no pool, -1 for no limit, otherwise the specified value
     */
    private int maxConnectionPoolSize;

    /**
     * Value that can be set via the System property
     * 'mule.sftp.transport.maxConnectionPoolSize'. If it's set the value is used
     * instead of <i>maxConnectionPoolSize</i>
     */
    private static final Integer overrideMaxConnectionPoolSize;

    static
    {
        String propValue = System.getProperty("mule.sftp.transport.maxConnectionPoolSize");
        if (propValue != null)
        {
            logger.info("Will override the maxConnectionPoolSize to " + propValue
                        + " from the system property 'mule.sftp.transport.maxConnectionPoolSize'.");
            overrideMaxConnectionPoolSize = Integer.parseInt(propValue);
        }
        else
        {
            overrideMaxConnectionPoolSize = null;
        }
    }

    public SftpConnector(MuleContext context)
    {
        super(context);
        filenameParser = new ExpressionFilenameParser();
    }

    public String getProtocol()
    {
        return "sftp";
    }

    @Override
    public MessageReceiver createReceiver(FlowConstruct flow, InboundEndpoint endpoint) throws Exception
    {
        long polling = pollingFrequency;

        // Override properties on the endpoint for the specific endpoint
        String tempPolling = (String) endpoint.getProperty(PROPERTY_POLLING_FREQUENCY);
        if (tempPolling != null)
        {
            polling = Long.parseLong(tempPolling);
        }

        if (polling <= 0)
        {
            polling = DEFAULT_POLLING_FREQUENCY;
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Set polling frequency to: " + polling);
        }

        return serviceDescriptor.createMessageReceiver(this, flow, endpoint, new Object[] {polling});
    }

    public SftpClient createSftpClient(ImmutableEndpoint endpoint) throws Exception
    {
        return createSftpClient(endpoint, null);
    }

    public SftpClient createSftpClient(ImmutableEndpoint endpoint, SftpNotifier notifier) throws Exception
    {
        SftpClient client = null;
        boolean ok = false;

        try
        {
            if (useConnectionPool())
            {
                ObjectPool pool = getClientPool(endpoint);
                client = (SftpClient) pool.borrowObject();
            }
            else
            {
                client = SftpConnectionFactory.createClient(endpoint, preferredAuthenticationMethods);
            }

            // We have to set the working directory before returning
            String dir = endpoint.getEndpointURI().getPath();
            client.changeWorkingDirectory(dir);
            if (logger.isDebugEnabled())
            {
                logger.debug("Successfully changed working directory to: " + dir);
            }

            // TODO ML: Is this always necessary?
            client.setNotifier(notifier);

            ok = true;

        }
        finally
        {
            // Release the client if it was created but something failed after that,
            // otherwise we start to waste ssh-processes...
            if (!ok && client != null)
            {
                releaseClient(endpoint, client);
            }
        }

        return client;
    }

    /**
     * @return True if connection pooling is used, otherwise false
     */
    public boolean useConnectionPool()
    {
        return getMaxConnectionPoolSize() != 0;
    }

    public void releaseClient(ImmutableEndpoint endpoint, SftpClient client) throws Exception
    {
        if (useConnectionPool())
        {
            if (getDispatcherFactory().isCreateDispatcherPerRequest())
            {
                destroyClient(endpoint, client);
            }
            else
            {
                if (client != null && client.isConnected())
                {
                    ObjectPool pool = getClientPool(endpoint);
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Releasing connection for endpoint " + endpoint.getEndpointURI());
                    }
                    pool.returnObject(client);
                }
            }
        }
        else
        {
            client.disconnect();
        }
    }

    public void destroyClient(ImmutableEndpoint endpoint, SftpClient client) throws Exception
    {
        if (useConnectionPool())
        {
            if ((client != null) && (client.isConnected()))
            {
                ObjectPool pool = getClientPool(endpoint);
                pool.invalidateObject(client);
            }
        }
    }

    protected synchronized ObjectPool getClientPool(ImmutableEndpoint endpoint)
    {
        GenericObjectPool pool = pools.get(endpoint.getEndpointURI());

        if (pool == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Pool is null - creating one for endpoint " + endpoint.getEndpointURI()
                             + " with max size " + getMaxConnectionPoolSize());
            }
            SftpConnectionFactory factory = new SftpConnectionFactory(endpoint);
            factory.setPreferredAuthenticationMethods(preferredAuthenticationMethods);
            pool = new GenericObjectPool(factory, getMaxConnectionPoolSize());
            pool.setTestOnBorrow(isValidateConnections());
            pools.put(endpoint.getEndpointURI(), pool);
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Using existing pool for endpoint " + endpoint.getEndpointURI() + ". Active: "
                             + pool.getNumActive() + ", Idle:" + pool.getNumIdle());
            }
        }

        return pool;
    }

    /*
     * (non-Javadoc)
     * @see org.mule.transport.AbstractConnector#doConnect()
     */
    protected void doConnect() throws Exception
    {
        // Do nothing!
    }

    /*
     * (non-Javadoc)
     * @see org.mule.transport.AbstractConnector#doDisconnect()
     */
    protected void doDisconnect() throws Exception
    {
        // Do nothing!
    }

    /*
     * (non-Javadoc)
     * @see org.mule.transport.AbstractConnector#doDispose()
     */
    protected void doDispose()
    {
        // Do nothing!
    }

    /*
     * (non-Javadoc)
     * @see org.mule.transport.AbstractConnector#doInitialise()
     */
    protected void doInitialise() throws InitialisationException
    {
        if (filenameParser != null)
        {
            filenameParser.setMuleContext(muleContext);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.mule.transport.AbstractConnector#doStart()
     */
    protected void doStart() throws MuleException
    {
        // Do nothing!
    }

    /*
     * (non-Javadoc)
     * @see org.mule.transport.AbstractConnector#doStop()
     */
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
            throw new ConnectorException(CoreMessages.failedToStop("SFTP Connector"), this, e);
        }
        finally
        {
            pools.clear();
        }
    }

    public long getPollingFrequency()
    {
        return pollingFrequency;
    }

    public void setPollingFrequency(long pollingFrequency)
    {
        this.pollingFrequency = pollingFrequency;
    }

    public FilenameParser getFilenameParser()
    {
        return filenameParser;
    }

    public void setFilenameParser(FilenameParser filenameParser)
    {
        this.filenameParser = filenameParser;
        if (filenameParser != null)
        {
            filenameParser.setMuleContext(muleContext);
        }
    }

    public String getOutputPattern()
    {
        return outputPattern;
    }

    public void setOutputPattern(String outputPattern)
    {
        this.outputPattern = outputPattern;
    }

    public boolean isAutoDelete()
    {
        return autoDelete;
    }

    public void setAutoDelete(boolean autoDelete)
    {
        this.autoDelete = autoDelete;
    }

    public String getIdentityFile()
    {
        return identityFile;
    }

    public void setIdentityFile(String identityFile)
    {
        this.identityFile = identityFile;
    }

    public String getPassphrase()
    {
        return passphrase;
    }

    public void setPassphrase(String passphrase)
    {
        this.passphrase = passphrase;
    }

    /**
     * Returns the file age.
     * 
     * @return Returns the fileAge in milliseconds.
     */
    public long getFileAge()
    {
        return fileAge;
    }

    /**
     * Sets the file age.
     * 
     * @param fileAge the fileAge in milliseconds to set.
     */
    public void setFileAge(long fileAge)
    {
        this.fileAge = fileAge;
        this.checkFileAge = true;
    }

    public boolean getCheckFileAge()
    {
        return checkFileAge;
    }

    public String getTempDirInbound()
    {
        return tempDirInbound;
    }

    public void setTempDirInbound(String pTempDirInbound)
    {
        tempDirInbound = pTempDirInbound;
    }

    public String getTempDirOutbound()
    {
        return tempDirOutbound;
    }

    public void setTempDirOutbound(String pTempDirOutbound)
    {
        tempDirOutbound = pTempDirOutbound;
    }

    public void setDuplicateHandling(String duplicateHandling)
    {
        this.duplicateHandling = duplicateHandling;
    }

    public String getDuplicateHandling()
    {
        return duplicateHandling;
    }

    public void setUseTempFileTimestampSuffix(Boolean useTempFileTimestampSuffix)
    {
        this.useTempFileTimestampSuffix = useTempFileTimestampSuffix;
    }

    public Boolean isUseTempFileTimestampSuffix()
    {
        return useTempFileTimestampSuffix;
    }

    public void setSizeCheckWaitTime(Long sizeCheckWaitTime)
    {
        this.sizeCheckWaitTime = sizeCheckWaitTime;
    }

    public Long getSizeCheckWaitTime()
    {
        return sizeCheckWaitTime;
    }

    public void setArchiveDir(String archiveDir)
    {
        this.archiveDir = archiveDir;
    }

    public String getArchiveDir()
    {
        return archiveDir;
    }

    public void setArchiveTempReceivingDir(String archiveTempReceivingDir)
    {
        this.archiveTempReceivingDir = archiveTempReceivingDir;
    }

    public String getArchiveTempReceivingDir()
    {
        return archiveTempReceivingDir;
    }

    public void setArchiveTempSendingDir(String archiveTempSendingDir)
    {
        this.archiveTempSendingDir = archiveTempSendingDir;
    }

    public String getArchiveTempSendingDir()
    {
        return archiveTempSendingDir;
    }

    /**
     * @see SftpConnector#maxConnectionPoolSize
     */
    public void setMaxConnectionPoolSize(int maxConnectionPoolSize)
    {
        this.maxConnectionPoolSize = maxConnectionPoolSize;
    }

    /**
     * @return the max connection pool size. If the system parameter
     *         mule.sftp.transport.maxConnectionPoolSize is set, that value will be
     *         used instead.
     */
    public int getMaxConnectionPoolSize()
    {
        if (overrideMaxConnectionPoolSize != null)
        {
            return overrideMaxConnectionPoolSize;
        }
        return maxConnectionPoolSize;
    }

    public Boolean isKeepFileOnError()
    {
        return keepFileOnError;
    }

    public void setKeepFileOnError(Boolean pKeepFileOnError)
    {
        keepFileOnError = pKeepFileOnError;
    }

    public String getPreferredAuthenticationMethods()
    {
        return preferredAuthenticationMethods;
    }

    public void setPreferredAuthenticationMethods(String preferredAuthenticationMethods)
    {
        validateAuthenticationMethods(preferredAuthenticationMethods);
        
        this.preferredAuthenticationMethods = preferredAuthenticationMethods;
    }


    //Since SFTP does not have any connection at the connector level and
    //since we need to fix SFTP connection at the inbound level we just let
    //each receiver to do it's own reconnection.
    @Override
    public void connect() throws Exception
    {
        setConnected(true);
        connectConnectorAndReceivers();
    }


    @Override
    public void disconnect() throws Exception
    {
        setConnected(false);
    }
}
