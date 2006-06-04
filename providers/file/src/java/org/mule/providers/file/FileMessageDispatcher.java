/* 
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.providers.file;

import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.file.filters.FilenameWildcardFilter;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;

/**
 * <code>FileMessageDispatcher</code> is used to read/write files to the
 * filesystem and
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class FileMessageDispatcher extends AbstractMessageDispatcher {
    private FileConnector connector;

    public FileMessageDispatcher(UMOImmutableEndpoint endpoint) {
        super(endpoint);
        this.connector = (FileConnector)endpoint.getConnector();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnectorSession#dispatch(org.mule.umo.UMOEvent)
     */
    protected void doDispatch(UMOEvent event) throws Exception {
        Object data = event.getTransformedMessage();
        //Wrap the transformed message before passing it to the filename parser
        UMOMessage message = new MuleMessage(data, event.getMessage());

        byte[] buf;
        if (data instanceof byte[]) {
            buf = (byte[]) data;
        } else {
            buf = data.toString().getBytes(event.getEncoding());
        }
        FileOutputStream fos = (FileOutputStream)getOutputStream(event.getEndpoint(), message);
        try {
            fos.write(buf);
        } finally {
            fos.close();
        }
    }

    /**
     * Well get the output stream (if any) for this type of transport.  Typically this will be called only when Streaming
     * is being used on an outbound endpoint
     *
     * @param endpoint the endpoint that releates to this Dispatcher
     * @param message  the current message being processed
     * @return the output stream to use for this request or null if the transport does not support streaming
     * @throws org.mule.umo.UMOException
     */
    public OutputStream getOutputStream(UMOImmutableEndpoint endpoint, UMOMessage message) throws UMOException {
        String address = endpoint.getEndpointURI().getAddress();
        String filename = message.getStringProperty(FileConnector.PROPERTY_FILENAME, null);

        try {
            if (filename == null) {
                String outPattern = message.getStringProperty(FileConnector.PROPERTY_OUTPUT_PATTERN, null);
                if (outPattern == null) {
                    outPattern = connector.getOutputPattern();
                }
                filename = generateFilename(message, outPattern);
            }

            if (filename == null) {
                throw new IOException("Filename is null");
            }

            File file = FileUtils.createFile(address + "/" + filename);
            if(logger.isInfoEnabled()) {
                logger.info("Writing file to: " + file.getAbsolutePath());
            }
            return new FileOutputStream(file, connector.isOutputAppend());
        } catch (IOException e) {
            throw new DispatchException(new Message(Messages.STREAMING_FAILED_NO_STREAM), message, endpoint, e);
        }
    }

    /**
     * There is no associated session for a file connector
     *
     * @return
     * @throws UMOException
     */
    public Object getDelegateSession() throws UMOException {
        return null;
    }

    /**
     * Will attempt to do a receive from a directory, if the endpointUri
     * resolves to a file name the file will be returned, otherwise the first
     * file in the directory according to the filename filter configured on the
     * connector.
     *
     * @param endpoint an endpoint a path to a file or directory
     * @param timeout     this is ignored when doing a receive on this dispatcher
     * @return a message containing file contents or null if there was notthing
     *         to receive
     * @throws Exception
     */

    protected UMOMessage doReceive(UMOImmutableEndpoint endpoint, long timeout) throws Exception {

        File file = new File(endpoint.getEndpointURI().getAddress());
        File result = null;
        FilenameFilter filenameFilter = null;
        String filter = (String) endpoint.getProperty("filter");
        if (filter != null) {
            filter = URLDecoder.decode(filter, MuleManager.getConfiguration().getEncoding());
            filenameFilter = new FilenameWildcardFilter(filter);
        }
        if (file.exists()) {
            if (file.isFile()) {
                result = file;
            } else if (file.isDirectory()) {
                result = getNextFile(endpoint.getEndpointURI().getAddress(), filenameFilter);
            }
            if (result != null) {
                boolean checkFileAge = connector.getCheckFileAge();
                if (checkFileAge) {
                    long fileAge = connector.getFileAge();
                    long lastMod = result.lastModified();
                    long now = (new java.util.Date()).getTime();
                    if ((now - lastMod) < fileAge) {
                        return null;
                    }
                }

                MuleMessage message = new MuleMessage(connector.getMessageAdapter(result));
                if (connector.getMoveToDirectory() != null) {
                    {
                        File destinationFile = new File(connector.getMoveToDirectory(), result.getName());
                        if (!result.renameTo(destinationFile)) {
                            logger.error("Failed to move file: " + result.getAbsolutePath() + " to "
                                    + destinationFile.getAbsolutePath());
                        }
                    }
                }
                result.delete();
                return message;
            }
        }
        return null;
    }

    private File getNextFile(String dir, FilenameFilter filter) throws UMOException {
        File[] files = new File[]{};
        File file = new File(dir);
        File result = null;
        try {
            if (file.exists()) {
                if (file.isFile()) {
                    result = file;
                } else if (file.isDirectory()) {
                    if (filter != null) {
                        files = file.listFiles(filter);
                    } else {
                        files = file.listFiles();
                    }
                    if (files.length > 0) {
                        result = files[0];
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw new MuleException(new Message("file", 1), e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnectorSession#send(org.mule.umo.UMOEvent)
     */
    protected UMOMessage doSend(UMOEvent event) throws Exception {
        doDispatch(event);
        return event.getMessage();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnectorSession#getConnector()
     */
    public UMOConnector getConnector() {
        return connector;
    }

    private String generateFilename(UMOMessage message, String pattern)  {
        if (pattern == null) {
            pattern = connector.getOutputPattern();
        }
        return connector.getFilenameParser().getFilename(message, pattern);
    }

    protected void doDispose() {
        // no op
    }

    protected void doConnect(UMOImmutableEndpoint endpoint) throws Exception {
        // no op
    }

    protected void doDisconnect() throws Exception {
        // no op
    }

}
