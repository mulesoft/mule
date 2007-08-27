/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.file;

import org.mule.config.MuleProperties;
import org.mule.config.i18n.CoreMessages;
import org.mule.providers.AbstractConnector;
import org.mule.providers.file.filters.FilenameWildcardFilter;
import org.mule.transformers.NoActionTransformer;
import org.mule.transformers.simple.ByteArrayToSerializable;
import org.mule.transformers.simple.SerializableToByteArray;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.util.FileUtils;
import org.mule.util.MapUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>FileConnector</code> is used for setting up listeners on a directory and
 * for writing files to a directory. The connecotry provides support for defining
 * file output patterns and filters for receiving files.
 */

public class FileConnector extends AbstractConnector
{
    /**
     * logger used by this class
     */
    private static Log logger = LogFactory.getLog(FileConnector.class);

    // These are properties that can be overridden on the Receiver by the endpoint
    // declaration
    public static final String PROPERTY_POLLING_FREQUENCY = "pollingFrequency";
    public static final String PROPERTY_FILE_AGE = "fileAge";
    public static final String PROPERTY_FILENAME = "filename";
    public static final String PROPERTY_ORIGINAL_FILENAME = "originalFilename";
    public static final String PROPERTY_OUTPUT_PATTERN = "outputPattern";
    public static final String PROPERTY_MOVE_TO_PATTERN = "moveToPattern";
    public static final String PROPERTY_MOVE_TO_DIRECTORY = "moveToDirectory";
    public static final String PROPERTY_DELETE_ON_READ = "autoDelete";
    public static final String PROPERTY_DIRECTORY = "directory";
    public static final String PROPERTY_SERVICE_OVERRIDE = "serviceOverrides";
    public static final String PROPERTY_WRITE_TO_DIRECTORY = "writeToDirectoryName";
    public static final String PROPERTY_READ_FROM_DIRECTORY = "readFromDirectoryName";

    public static final long DEFAULT_POLLING_FREQUENCY = 1000;

    /**
     * Time in milliseconds to poll. On each poll the poll() method is called
     */
    private long pollingFrequency = 0;

    private String moveToPattern = null;

    private String writeToDirectoryName = null;

    private String moveToDirectoryName = null;

    private String readFromDirectoryName = null;

    private String outputPattern = null;

    private boolean outputAppend = false;

    private boolean autoDelete = true;

    private boolean checkFileAge = false;

    private long fileAge = 0;

    private FileOutputStream outputStream = null;

    private boolean serialiseObjects = false;

    public FilenameParser filenameParser;

    /*
     * (non-Javadoc)
     *
     * @see org.mule.providers.AbstractConnector#doInitialise()
     */
    public FileConnector()
    {
        filenameParser = new SimpleFilenameParser();
    }

    protected Object getReceiverKey(UMOComponent component, UMOEndpoint endpoint)
    {
        if (endpoint.getFilter() != null)
        {
            return endpoint.getEndpointURI().getAddress() + "/"
                   + ((FilenameWildcardFilter) endpoint.getFilter()).getPattern();
        }
        return endpoint.getEndpointURI().getAddress();
    }

    /**
     * Registers a listener for a particular directory The following properties can
     * be overriden in the endpoint declaration
     * <ul>
     * <li>moveToDirectory</li>
     * <li>filterPatterns</li>
     * <li>filterClass</li>
     * <li>pollingFrequency</li>
     * </ul>
     */
    public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        String readDir = endpoint.getEndpointURI().getAddress();
        if (null != getReadFromDirectory())
        {
            readDir = getReadFromDirectory();
        }

        long polling = this.pollingFrequency;

        String moveTo = moveToDirectoryName;
        String moveToPattern = getMoveToPattern();

        Map props = endpoint.getProperties();
        if (props != null)
        {
            // Override properties on the endpoint for the specific endpoint
            String read = (String)props.get(PROPERTY_READ_FROM_DIRECTORY);
            if (read != null)
            {
                readDir = read;
            }
            String move = (String)props.get(PROPERTY_MOVE_TO_DIRECTORY);
            if (move != null)
            {
                moveTo = move;
            }
            String tempMoveToPattern = (String) props.get(PROPERTY_MOVE_TO_PATTERN);
            if (tempMoveToPattern != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("set moveTo Pattern to: " + tempMoveToPattern);
                }
                moveToPattern = tempMoveToPattern;
            }

            String tempPolling = (String) props.get(PROPERTY_POLLING_FREQUENCY);
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
                logger.debug("set polling frequency to: " + polling);
            }
            String tempFileAge = (String) props.get(PROPERTY_FILE_AGE);
            if (tempFileAge != null)
            {
                try
                {
                    setFileAge(Long.parseLong(tempFileAge));
                }
                catch (Exception ex1)
                {
                    logger.error("Failed to set fileAge", ex1);
                }
            }
            Map srvOverride = (Map) props.get(PROPERTY_SERVICE_OVERRIDE);
            if (srvOverride != null)
            {
                if (serviceOverrides == null)
                {
                    serviceOverrides = new Properties();
                }
                serviceOverrides.setProperty(MuleProperties.CONNECTOR_INBOUND_TRANSFORMER,
                    NoActionTransformer.class.getName());
                serviceOverrides.setProperty(MuleProperties.CONNECTOR_OUTBOUND_TRANSFORMER,
                    NoActionTransformer.class.getName());
            }
        }

        try
        {
            return serviceDescriptor.createMessageReceiver(this, component, endpoint, new Object[]{readDir,
                moveTo, moveToPattern, new Long(polling)});

        }
        catch (Exception e)
        {
            throw new InitialisationException(
                CoreMessages.failedToCreateObjectWith("Message Receiver",
                    serviceDescriptor), e, this);
        }
    }

    public String getProtocol()
    {
        return "file";
    }

    public FilenameParser getFilenameParser()
    {
        return filenameParser;
    }

    public void setFilenameParser(FilenameParser filenameParser)
    {
        this.filenameParser = filenameParser;
    }

    protected void doDispose()
    {
        try
        {
            doStop();
        }
        catch (UMOException e)
        {
            logger.error(e.getMessage(), e);
        }
    }


    protected void doInitialise() throws InitialisationException
    {
        // template method, nothing to do
    }

    protected void doConnect() throws Exception
    {
        // template method, nothing to do
    }

    protected void doDisconnect() throws Exception
    {
        // template method, nothing to do
    }

    protected void doStart() throws UMOException
    {
        // template method, nothing to do
    }

    protected void doStop() throws UMOException
    {
        if (outputStream != null)
        {
            try
            {
                outputStream.close();
            }
            catch (IOException e)
            {
                logger.warn("Failed to close file output stream on stop: " + e);
            }
        }
    }

    /**
     * @return Returns the moveToDirectoryName.
     */
    public String getMoveToDirectory()
    {
        return moveToDirectoryName;
    }

    /**
     * @param dir The moveToDirectoryName to set.
     */
    public void setMoveToDirectory(String dir)
    {
        this.moveToDirectoryName = dir;
    }

    /**
     * @return Returns the outputAppend.
     */
    public boolean isOutputAppend()
    {
        return outputAppend;
    }

    /**
     * @param outputAppend The outputAppend to set.
     */
    public void setOutputAppend(boolean outputAppend)
    {
        this.outputAppend = outputAppend;
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
     * @return Returns the outputStream.
     */
    public FileOutputStream getOutputStream()
    {
        return outputStream;
    }

    /**
     * @param outputStream The outputStream to set.
     */
    public void setOutputStream(FileOutputStream outputStream)
    {
        this.outputStream = outputStream;
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
     * @return Returns the fileAge.
     */
    public long getFileAge()
    {
        return fileAge;
    }

    public boolean getCheckFileAge()
    {
        return checkFileAge;
    }

    /**
     * @param fileAge The fileAge in milliseconds to set.
     */
    public void setFileAge(long fileAge)
    {
        this.fileAge = fileAge;
        this.checkFileAge = true;
    }

    /**
     * @return Returns the writeToDirectory.
     */
    public String getWriteToDirectory()
    {
        return writeToDirectoryName;
    }

    /**
     * @param dir The writeToDirectory to set.
     */
    public void setWriteToDirectory(String dir) throws IOException
    {
        this.writeToDirectoryName = dir;
        if (writeToDirectoryName != null)
        {
            File writeToDirectory = FileUtils.openDirectory((writeToDirectoryName));
            if (!(writeToDirectory.canRead()) || !writeToDirectory.canWrite())
            {
                throw new IOException(
                    "Error on initialization, Write To directory does not exist or is not read/write");
            }
        }
    }

    /**
     * @return Returns the readFromDirectory.
     */
    public String getReadFromDirectory()
    {
        return readFromDirectoryName;
    }

    /**
     * @param dir The readFromDirectory to set.
     */
    public void setReadFromDirectory(String dir) throws IOException
    {
        this.readFromDirectoryName = dir;
        if (readFromDirectoryName != null)
        {
            File readFromDirectory = FileUtils.openDirectory((readFromDirectoryName));
            if (!readFromDirectory.canRead())
            {
                throw new IOException(
                    "Error on initialization, read from directory does not exist or is not readable");
            }
        }
    }

    public boolean isSerialiseObjects()
    {
        return serialiseObjects;
    }

    public void setSerialiseObjects(boolean serialiseObjects)
    {
        // set serialisable transformers on the connector if this is set
        if (serialiseObjects)
        {
            if (serviceOverrides == null)
            {
                serviceOverrides = new Properties();
            }
            serviceOverrides.setProperty(MuleProperties.CONNECTOR_INBOUND_TRANSFORMER,
                ByteArrayToSerializable.class.getName());
            serviceOverrides.setProperty(MuleProperties.CONNECTOR_OUTBOUND_TRANSFORMER,
                SerializableToByteArray.class.getName());
        }

        this.serialiseObjects = serialiseObjects;
    }

    public boolean isAutoDelete()
    {
        return autoDelete;
    }

    public void setAutoDelete(boolean autoDelete)
    {
        this.autoDelete = autoDelete;
        if (!autoDelete)
        {
            if (serviceOverrides == null)
            {
                serviceOverrides = new Properties();
            }
            if (serviceOverrides.getProperty(MuleProperties.CONNECTOR_MESSAGE_ADAPTER) == null)
            {
                serviceOverrides.setProperty(MuleProperties.CONNECTOR_MESSAGE_ADAPTER,
                    FileMessageAdapter.class.getName());
            }
        }
    }

    public String getMoveToPattern()
    {
        return moveToPattern;
    }

    public void setMoveToPattern(String moveToPattern)
    {
        this.moveToPattern = moveToPattern;
    }

    /**
     * Well get the output stream (if any) for this type of transport. Typically this
     * will be called only when Streaming is being used on an outbound endpoint
     *
     * @param endpoint the endpoint that releates to this Dispatcher
     * @param message the current message being processed
     * @return the output stream to use for this request or null if the transport
     *         does not support streaming
     * @throws org.mule.umo.UMOException
     */
    public OutputStream getOutputStream(UMOImmutableEndpoint endpoint, UMOMessage message)
        throws UMOException
    {
        String address = endpoint.getEndpointURI().getAddress();
        String writeToDirectory = message.getStringProperty(
            FileConnector.PROPERTY_WRITE_TO_DIRECTORY, null);
        if (writeToDirectory == null)
        {
            writeToDirectory = getWriteToDirectory();
        }
        if (writeToDirectory != null)
        {
            address = getFilenameParser().getFilename(message, writeToDirectory);
        }

        String filename;
        String outPattern = (String)endpoint.getProperty(FileConnector.PROPERTY_OUTPUT_PATTERN);
        if (outPattern == null)
        {
            outPattern = message.getStringProperty(FileConnector.PROPERTY_OUTPUT_PATTERN, null);
        }
        if (outPattern == null)
        {
            outPattern = getOutputPattern();
        }
        try
        {
            if (outPattern != null)
            {
                filename = generateFilename(message, outPattern);
            }
            else
            {
                filename = message.getStringProperty(FileConnector.PROPERTY_FILENAME, null);
                if (filename == null)
                {
                    filename = generateFilename(message, null);
                }
            }

            if (filename == null)
            {
                throw new IOException("Filename is null");
            }
            File file = FileUtils.createFile(address + "/" + filename);
            if (logger.isInfoEnabled())
            {
                logger.info("Writing file to: " + file.getAbsolutePath());
            }

            return new FileOutputStream(file, MapUtils.getBooleanValue(endpoint.getProperties(),
                "outputAppend", isOutputAppend()));
        }
        catch (IOException e)
        {
            throw new DispatchException(CoreMessages.streamingFailedNoStream(), message,
                endpoint, e);
        }
    }

    private String generateFilename(UMOMessage message, String pattern)
    {
        if (pattern == null)
        {
            pattern = getOutputPattern();
        }
        return getFilenameParser().getFilename(message, pattern);
    }
}
