/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ftp;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.providers.file.FilenameParser;
import org.mule.providers.file.SimpleFilenameParser;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.ConnectorException;
import org.mule.umo.provider.UMOMessageReceiver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class FtpConnector extends AbstractServiceEnabledConnector
{

    public static final String PROPERTY_POLLING_FREQUENCY = "pollingFrequency";
    public static final String PROPERTY_FILENAME = "filename";
    public static final String PROPERTY_OUTPUT_PATTERN = "outputPattern";
    public static final String PROPERTY_PASSIVE_MODE = "passive";
    public static final String PROPERTY_BINARY_TRANSFER = "binary";

    /**
     * Time in milliseconds to poll. On each poll the poll() method is called
     */
    private long pollingFrequency = 0;

    private String outputPattern = null;

    private FilenameParser filenameParser = new SimpleFilenameParser();

    private boolean passive = true;

    private boolean binary = true;

    /**
     * Whether to test FTP connection on each take from pool.
     */
    private boolean validateConnections = true;

    private Map pools = new HashMap();

    public String getProtocol()
    {
        return "ftp";
    }

    public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        long polling = pollingFrequency;
        Map props = endpoint.getProperties();
        if (props != null)
        {
            // Override properties on the endpoint for the specific endpoint
            String tempPolling = (String)props.get(PROPERTY_POLLING_FREQUENCY);
            if (tempPolling != null)
            {
                polling = Long.parseLong(tempPolling);
            }
        }
        if (polling <= 0)
        {
            polling = 1000;
        }
        logger.debug("set polling frequency to: " + polling);
        return serviceDescriptor.createMessageReceiver(this, component, endpoint, new Object[]{new Long(
            polling)});
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

    public FTPClient getFtp(UMOEndpointURI uri) throws Exception
    {
        ObjectPool pool = getFtpPool(uri);
        return (FTPClient)pool.borrowObject();
    }

    public void releaseFtp(UMOEndpointURI uri, FTPClient client) throws Exception
    {
        if (isCreateDispatcherPerRequest())
        {
            destroyFtp(uri, client);
        }
        else
        {
            if (client != null && client.isConnected())
            {
                ObjectPool pool = getFtpPool(uri);
                pool.returnObject(client);
            }
        }
    }

    public void destroyFtp(UMOEndpointURI uri, FTPClient client) throws Exception
    {
        if (client != null && client.isConnected())
        {
            ObjectPool pool = getFtpPool(uri);
            pool.invalidateObject(client);
        }
    }

    protected synchronized ObjectPool getFtpPool(UMOEndpointURI uri)
    {
        String key = uri.getUsername() + ":" + uri.getPassword() + "@" + uri.getHost() + ":" + uri.getPort();
        ObjectPool pool = (ObjectPool)pools.get(key);
        if (pool == null)
        {
            pool = new GenericObjectPool(new FtpConnectionFactory(uri));
            ((GenericObjectPool)pool).setTestOnBorrow(this.validateConnections);
            pools.put(key, pool);
        }
        return pool;
    }

    /**
     * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
     */
    protected class FtpConnectionFactory implements PoolableObjectFactory
    {
        private UMOEndpointURI uri;

        public FtpConnectionFactory(UMOEndpointURI uri)
        {
            this.uri = uri;
        }

        public Object makeObject() throws Exception
        {
            FTPClient client = new FTPClient();
            try
            {
                if (uri.getPort() > 0)
                {
                    client.connect(uri.getHost(), uri.getPort());
                }
                else
                {
                    client.connect(uri.getHost());
                }
                if (!FTPReply.isPositiveCompletion(client.getReplyCode()))
                {
                    throw new IOException("Ftp error: " + client.getReplyCode());
                }
                if (!client.login(uri.getUsername(), uri.getPassword()))
                {
                    throw new IOException("Ftp error: " + client.getReplyCode());
                }
                if (!client.setFileType(FTP.BINARY_FILE_TYPE))
                {
                    throw new IOException("Ftp error. Couldn't set BINARY transfer type.");
                }
            }
            catch (Exception e)
            {
                if (client.isConnected())
                {
                    client.disconnect();
                }
                throw e;
            }
            return client;
        }

        public void destroyObject(Object obj) throws Exception
        {
            FTPClient client = (FTPClient)obj;
            client.logout();
            client.disconnect();
        }

        public boolean validateObject(Object obj)
        {
            FTPClient client = (FTPClient)obj;
            try
            {
                client.sendNoOp();
                return true;
            }
            catch (IOException e)
            {
                return false;
            }
        }

        public void activateObject(Object obj) throws Exception
        {
            FTPClient client = (FTPClient)obj;
            client.setReaderThread(true);
        }

        public void passivateObject(Object obj) throws Exception
        {
            FTPClient client = (FTPClient)obj;
            client.setReaderThread(false);
        }
    }

    protected void doStop() throws UMOException
    {
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
            throw new ConnectorException(new Message(Messages.FAILED_TO_STOP_X, "FTP Connector"), this, e);
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
    public void enterActiveOrPassiveMode(FTPClient client, UMOImmutableEndpoint endpoint)
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
    public void setupFileType(FTPClient client, UMOImmutableEndpoint endpoint) throws Exception
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
                type = FTP.BINARY_FILE_TYPE;
            }
            else
            {
                if (logger.isTraceEnabled())
                {
                    logger.trace("Using FTP ASCII type");
                }
                type = FTP.ASCII_FILE_TYPE;
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
                type = FTP.BINARY_FILE_TYPE;
            }
            else
            {
                if (logger.isTraceEnabled())
                {
                    logger.trace("Using FTP ASCII type (endpoint override)");
                }
                type = FTP.ASCII_FILE_TYPE;
            }
        }

        client.setFileType(type);
    }

}
