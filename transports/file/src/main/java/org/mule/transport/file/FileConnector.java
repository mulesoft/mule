/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.simple.ByteArrayToSerializable;
import org.mule.transformer.simple.SerializableToByteArray;
import org.mule.transport.AbstractConnector;
import org.mule.transport.file.filters.FilenameWildcardFilter;
import org.mule.transport.file.i18n.FileMessages;
import org.mule.util.FileUtils;

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

    private static Log logger = LogFactory.getLog(FileConnector.class);

    public static final String FILE = "file";
    private static final String DEFAULT_WORK_FILENAME_PATTERN = "#[function:uuid].#[function:systime].#[header:inbound:originalFilename]";

    // These are properties that can be overridden on the Receiver by the endpoint declaration
    // inbound only
    public static final String PROPERTY_FILE_AGE = "fileAge";
    public static final String PROPERTY_MOVE_TO_PATTERN = "moveToPattern";
    public static final String PROPERTY_MOVE_TO_DIRECTORY = "moveToDirectory";
    public static final String PROPERTY_READ_FROM_DIRECTORY = "readFromDirectoryName";
    // outbound only
    public static final String PROPERTY_OUTPUT_PATTERN = "outputPattern";

    // message properties
    public static final String PROPERTY_FILENAME = "filename";
    public static final String PROPERTY_ORIGINAL_FILENAME = "originalFilename";
    public static final String PROPERTY_ORIGINAL_DIRECTORY = "originalDirectory";
    public static final String PROPERTY_DIRECTORY = "directory";
    public static final String PROPERTY_SOURCE_FILENAME = "sourceFileName";
    public static final String PROPERTY_SOURCE_DIRECTORY = "sourceDirectory";
    public static final String PROPERTY_WRITE_TO_DIRECTORY = "writeToDirectoryName";
    public static final String PROPERTY_FILE_SIZE = "fileSize";
    public static final String PROPERTY_FILE_TIMESTAMP = "timestamp";

    public static final long DEFAULT_POLLING_FREQUENCY = 1000;

    /**
     * Time in milliseconds to poll. On each poll the poll() method is called
     */
    private long pollingFrequency = 0;

    private String moveToPattern = null;

    private String writeToDirectoryName = null;

    private String moveToDirectoryName = null;

    private String workDirectoryName = null;

    private String workFileNamePattern = DEFAULT_WORK_FILENAME_PATTERN;

    private String readFromDirectoryName = null;

    private String outputPattern = null;

    private boolean outputAppend = false;

    private boolean autoDelete = true;

    private boolean checkFileAge = false;

    private long fileAge = 0;

    private FileOutputStream outputStream = null;

    private boolean serialiseObjects = false;

    private boolean streaming = true;

    public FilenameParser filenameParser;

    private boolean recursive = false;

    public FileConnector(MuleContext context)
    {
        super(context);
        filenameParser = new ExpressionFilenameParser();
    }

    @Override
    protected void configureDispatcherPool()
    {
        if (isOutputAppend())
        {
            setMaxDispatchersActive(getDispatcherThreadingProfile().getMaxThreadsActive());
        }
        else
        {
            super.configureDispatcherPool();
        }
    }

    @Override
    public void setMaxDispatchersActive(int value)
    {
        if (isOutputAppend() && value != 1)
        {
            logger.warn("MULE-1773: cannot configure maxDispatchersActive when using outputAppend. New value not set");
        }
        else
        {
            super.setMaxDispatchersActive(value);
        }
    }

    @Override
    protected Object getReceiverKey(FlowConstruct flowConstruct, InboundEndpoint endpoint)
    {
        if (endpoint.getFilter() != null && endpoint.getFilter() instanceof FilenameWildcardFilter)
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
    @Override
    public MessageReceiver createReceiver(FlowConstruct flowConstruct, InboundEndpoint endpoint) throws Exception
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
            String read = (String) props.get(PROPERTY_READ_FROM_DIRECTORY);
            if (read != null)
            {
                readDir = read;
            }
            String move = (String) props.get(PROPERTY_MOVE_TO_DIRECTORY);
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
        }

        try
        {
            return serviceDescriptor.createMessageReceiver(this, flowConstruct, endpoint, new Object[]{readDir,
                    moveTo, moveToPattern, Long.valueOf(polling)});

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
        return FILE;
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

    @Override
    protected void doDispose()
    {
        try
        {
            doStop();
        }
        catch (MuleException e)
        {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        if (filenameParser != null)
        {
            filenameParser.setMuleContext(muleContext);
        }

        // MULE-1773: limit the number of dispatchers per endpoint to 1 until
        // there is a proper (Distributed)LockManager in place (MULE-2402).
        // We also override the setter to prevent "wrong" configuration for now.
        if (isOutputAppend())
        {
            super.setMaxDispatchersActive(1);
        }
    }

    @Override
    protected void doConnect() throws Exception
    {
        // template method, nothing to do
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // template method, nothing to do
    }

    @Override
    protected void doStart() throws MuleException
    {
        // template method, nothing to do
    }

    @Override
    protected void doStop() throws MuleException
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

    public String getMoveToDirectory()
    {
        return moveToDirectoryName;
    }

    public void setMoveToDirectory(String dir)
    {
        this.moveToDirectoryName = dir;
    }

    public void setWorkDirectory(String workDirectoryName) throws IOException
    {
        this.workDirectoryName = workDirectoryName;
        if (workDirectoryName != null)
        {
            File workDirectory = FileUtils.openDirectory(workDirectoryName);
            if (!workDirectory.canWrite())
            {
                throw new IOException(
                        "Error on initialization, Work Directory '" + workDirectory +"' is not writeable");
            }
        }
    }

    public String getWorkDirectory()
    {
        return workDirectoryName;
    }

    public void setWorkFileNamePattern(String workFileNamePattern)
    {
        this.workFileNamePattern = workFileNamePattern;
    }

    public String getWorkFileNamePattern()
    {
        return workFileNamePattern;
    }

    public boolean isOutputAppend()
    {
        return outputAppend;
    }

    public void setOutputAppend(boolean outputAppend)
    {
        this.outputAppend = outputAppend;
    }

    public String getOutputPattern()
    {
        return outputPattern;
    }

    public void setOutputPattern(String outputPattern)
    {
        this.outputPattern = outputPattern;
    }

    public FileOutputStream getOutputStream()
    {
        return outputStream;
    }

    public void setOutputStream(FileOutputStream outputStream)
    {
        this.outputStream = outputStream;
    }

    public long getPollingFrequency()
    {
        return pollingFrequency;
    }

    public void setPollingFrequency(long pollingFrequency)
    {
        this.pollingFrequency = pollingFrequency;
    }

    public long getFileAge()
    {
        return fileAge;
    }

    public boolean getCheckFileAge()
    {
        return checkFileAge;
    }

    public void setFileAge(long fileAge)
    {
        this.fileAge = fileAge;
        this.checkFileAge = true;
    }

    public String getWriteToDirectory()
    {
        return writeToDirectoryName;
    }

    public void setWriteToDirectory(String dir) throws IOException
    {
        this.writeToDirectoryName = dir;
        if (writeToDirectoryName != null)
        {
            File writeToDirectory = FileUtils.openDirectory(writeToDirectoryName);
            if (!writeToDirectory.canWrite())
            {
                throw new IOException(
                        "Error on initialization, " + writeToDirectory
                        + " does not exist or is not writeable");
            }
        }
    }

    public String getReadFromDirectory()
    {
        return readFromDirectoryName;
    }

    public void setReadFromDirectory(String dir) throws IOException
    {
        this.readFromDirectoryName = dir;
        if (readFromDirectoryName != null)
        {
            // check if the directory exists/can be read
            FileUtils.openDirectory((readFromDirectoryName));
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
     * @param event  the current event being processed
     * @return the output stream to use for this request or null if the transport
     *         does not support streaming
     * @throws org.mule.api.MuleException
     */
    @Override
    public OutputStream getOutputStream(OutboundEndpoint endpoint, MuleEvent event) throws MuleException
    {
        MuleMessage message = event.getMessage();
        String address = endpoint.getEndpointURI().getAddress();
        String writeToDirectory = message.getOutboundProperty(FileConnector.PROPERTY_WRITE_TO_DIRECTORY);
        if (writeToDirectory == null)
        {
            writeToDirectory = getWriteToDirectory();
        }
        if (writeToDirectory != null)
        {
            address = getFilenameParser().getFilename(message, writeToDirectory);
        }

        String filename;
        String outPattern = (String) endpoint.getProperty(FileConnector.PROPERTY_OUTPUT_PATTERN);
        if (outPattern == null)
        {
            outPattern = message.getOutboundProperty(FileConnector.PROPERTY_OUTPUT_PATTERN);
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
                filename = message.getOutboundProperty(FileConnector.PROPERTY_FILENAME);
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

            return new FileOutputStream(file, isOutputAppend());
        }
        catch (IOException e)
        {
            throw new DispatchException(CoreMessages.streamingFailedNoStream(), event, endpoint, e);
        }
    }

    protected void move(final File sourceFile, File destinationFile) throws DefaultMuleException
    {
        if (destinationFile != null)
        {
            // move sourceFile to new destination. Do not use FileUtils here as it ultimately
            // falls back to copying the file which will cause problems on large files again -
            // which is what we're trying to avoid in the first place
            boolean fileWasMoved = sourceFile.renameTo(destinationFile);

            // move didn't work - bail out
            if (!fileWasMoved)
            {
                throw new DefaultMuleException(FileMessages.failedToMoveFile(sourceFile.getAbsolutePath(),
                    destinationFile.getAbsolutePath()));
            }
        }
    }

    private String generateFilename(MuleMessage message, String pattern)
    {
        if (pattern == null)
        {
            pattern = getOutputPattern();
        }
        return getFilenameParser().getFilename(message, pattern);
    }

    public boolean isStreaming()
    {
        return streaming;
    }

    public void setStreaming(boolean streaming)
    {
        this.streaming = streaming;
    }

    @Override
    public MuleMessageFactory createMuleMessageFactory() throws CreateException
    {
        // See MULE-3209, MULE-3199
        if (isStreaming())
        {
            return new FileMuleMessageFactory();
        }
        else
        {
            return super.createMuleMessageFactory();
        }
    }
    public boolean isRecursive()
    {
        return recursive;
    }

    public void setRecursive(boolean recursive)
    {
        this.recursive = recursive;
    }
}
