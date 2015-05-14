/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.transport.sftp.notification.SftpNotifier;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains reusable methods not directly related to usage of the jsch sftp library
 * (they can be found in the class SftpClient).
 *
 * @author Magnus Larsson
 */
public class SftpReceiverRequesterUtil {
    private transient Log logger = LogFactory.getLog(getClass());

    private final SftpConnector connector;
    private final ImmutableEndpoint endpoint;
    private final FilenameFilter filenameFilter;
    private final SftpUtil sftpUtil;
    private final long fileAge;
    private final boolean checkFileAge;
    private final long sizeCheckDelayMs;

    public SftpReceiverRequesterUtil(ImmutableEndpoint endpoint) {
        this.endpoint = endpoint;
        this.connector = (SftpConnector) endpoint.getConnector();

        sftpUtil = new SftpUtil(endpoint);
        // Get size check parameter
        sizeCheckDelayMs = sftpUtil.getSizeCheckWaitTime();

        if (endpoint.getFilter() instanceof FilenameFilter) {
            this.filenameFilter = (FilenameFilter) endpoint.getFilter();
        } else {
            this.filenameFilter = null;
        }

        // Override the value from the Endpoint?
        if (endpoint.getProperty(SftpConnector.PROPERTY_FILE_AGE) != null) {
            checkFileAge = true;
            fileAge = Long.valueOf((String) endpoint.getProperty(SftpConnector.PROPERTY_FILE_AGE));
        } else {
            fileAge = connector.getFileAge();
            checkFileAge = connector.getCheckFileAge();
        }
        logger.debug("fileAge : " + fileAge);

    }

    // Get files in directory configured on the endpoint
    public List<FileDescriptor> getAvailableFiles(boolean onlyGetTheFirstOne) throws Exception {
        // This sftp client instance is only for checking available files. This
        // instance cannot be shared
        // with clients that retrieve files because of thread safety

        if (logger.isDebugEnabled()) {
            logger.debug("Checking files at endpoint " + endpoint.getEndpointURI());
        }

        SftpClient client = null;

        try {
            client = connector.createSftpClient(endpoint);

            long fileAge = connector.getFileAge();
            boolean checkFileAge = connector.getCheckFileAge();

            // Override the value from the Endpoint?
            if (endpoint.getProperty(SftpConnector.PROPERTY_FILE_AGE) != null) {
                checkFileAge = true;
                fileAge = Long.valueOf((String) endpoint.getProperty(SftpConnector.PROPERTY_FILE_AGE));
            }

            logger.debug("fileAge: " + fileAge);

            final FileDescriptor[] files = client.getFileDescriptors();

            // Only return files that have completely been written and match
            // fileExtension
            final List<FileDescriptor> completedFiles = new ArrayList<>(files.length);

            for (final FileDescriptor fileDesc : files) {
                final String file = fileDesc.getFilename();
                // Skip if no match.
                // Note, Mule also uses this filter. We use the filter here because
                // we don't want to
                // run the below tests (checkFileAge, sizeCheckDelayMs etc) for files
                // that Mule
                // later should have ignored. Thus this is an "early" filter so that
                // improves performance.
                if (filenameFilter != null && !filenameFilter.accept(null, file)) {
                    continue;
                }

                if (isFileCompleted(file, client)) {
                    completedFiles.add(fileDesc);
                    if (onlyGetTheFirstOne) {
                        break;
                    }
                }
            }
            return completedFiles;
        } finally {
            if (client != null) {
                connector.releaseClient(endpoint, client);
            }
        }

    }

    private boolean isFileCompleted(final String file, SftpClient client) throws Exception {
        if (checkFileAge || sizeCheckDelayMs >= 0) {
            // See if the file is still growing (either by age or size),
            // leave it alone if it is
            return canProcessFile(file, client, fileAge, sizeCheckDelayMs);
        }
        return true;
    }

    public InputStream retrieveFile(String fileName, SftpNotifier notifier) throws Exception {
        // Getting a new SFTP client dedicated to the SftpInputStream below
        SftpClient client = connector.createSftpClient(endpoint, notifier);

        // Check usage of tmpSendingDir
        String tmpSendingDir = sftpUtil.getTempDirInbound();
        if (tmpSendingDir != null) {
            // Check usage of unique names of files during transfer
            boolean addUniqueSuffix = sftpUtil.isUseTempFileTimestampSuffix();

            // TODO: is it possibly to move this to some kind of init method?
            client.createSftpDirIfNotExists(endpoint, tmpSendingDir);
            String tmpSendingFileName = tmpSendingDir + "/" + fileName;

            if (addUniqueSuffix) {
                tmpSendingFileName = sftpUtil.createUniqueSuffix(tmpSendingFileName);
            }
            String fullTmpSendingPath = endpoint.getEndpointURI().getPath() + "/" + tmpSendingFileName;

            if (logger.isDebugEnabled()) {
                logger.debug("Move " + fileName + " to " + fullTmpSendingPath);
            }
            client.rename(fileName, fullTmpSendingPath);
            fileName = tmpSendingFileName;
            if (logger.isDebugEnabled()) {
                logger.debug("Move done");
            }
        }

        // Archive functionality...
        String archive = sftpUtil.getArchiveDir();

        // Retrieve the file stream
        InputStream fileInputStream = client.retrieveFile(fileName);

        if (!"".equals(archive)) {
            String archiveTmpReceivingDir = sftpUtil.getArchiveTempReceivingDir();
            String archiveTmpSendingDir = sftpUtil.getArchiveTempSendingDir();

            InputStream is = new SftpInputStream(client, fileInputStream, fileName, determineAutoDelete(),
                endpoint);

            // TODO ML FIX. Refactor to util-class...
            int idx = fileName.lastIndexOf('/');
            String fileNamePart = fileName.substring(idx + 1);

            // don't use new File() directly, see MULE-1112
            File archiveFile = FileUtils.newFile(archive, fileNamePart);

            // Should temp dirs be used when handling the archive file?
            if ("".equals(archiveTmpReceivingDir) || "".equals(archiveTmpSendingDir)) {
                return archiveFile(is, archiveFile);
            } else {
                return archiveFileUsingTempDirs(archive, archiveTmpReceivingDir, archiveTmpSendingDir, is,
                        fileNamePart, archiveFile);
            }
        }

        // This special InputStream closes the SftpClient when the stream is closed.
        // The stream will be materialized in a Message Dispatcher or Service
        // Component
        return new SftpInputStream(client, fileInputStream, fileName, determineAutoDelete(), endpoint);
    }

    private boolean determineAutoDelete()
    {
        boolean autoDelete;
        String autoDeleteProperty = (String) endpoint.getProperty("autoDelete");
        if (autoDeleteProperty == null)
        {
            autoDelete = connector.isAutoDelete();
        }
        else
        {
            autoDelete = Boolean.valueOf(autoDeleteProperty);
        }
        return autoDelete;
    }

    private InputStream archiveFileUsingTempDirs(String archive,
                                                 String archiveTmpReceivingDir,
                                                 String archiveTmpSendingDir,
                                                 InputStream is,
                                                 String fileNamePart,
                                                 File archiveFile) throws IOException {

        File archiveTmpReceivingFolder = FileUtils.newFile(archive + '/' + archiveTmpReceivingDir);
        File archiveTmpReceivingFile = FileUtils.newFile(archive + '/' + archiveTmpReceivingDir, fileNamePart);
        if (!archiveTmpReceivingFolder.exists()) {
            if (logger.isInfoEnabled()) {
                logger.info("Creates " + archiveTmpReceivingFolder.getAbsolutePath());
            }
            if (!archiveTmpReceivingFolder.mkdirs())
                throw new IOException("Failed to create archive-tmp-receiving-folder: "
                        + archiveTmpReceivingFolder);
        }

        File archiveTmpSendingFolder = FileUtils.newFile(archive + '/' + archiveTmpSendingDir);
        File archiveTmpSendingFile = FileUtils.newFile(archive + '/' + archiveTmpSendingDir, fileNamePart);
        if (!archiveTmpSendingFolder.exists()) {
            if (logger.isInfoEnabled()) {
                logger.info("Creates " + archiveTmpSendingFolder.getAbsolutePath());
            }
            if (!archiveTmpSendingFolder.mkdirs())
                throw new IOException("Failed to create archive-tmp-sending-folder: "
                        + archiveTmpSendingFolder);
        }

        if (logger.isInfoEnabled()) {
            logger.info("Copy SftpInputStream to archiveTmpReceivingFile... "
                    + archiveTmpReceivingFile.getAbsolutePath());
        }
        sftpUtil.copyStreamToFile(is, archiveTmpReceivingFile);

        // TODO. ML FIX. Should be performed before the sftp:delete - operation, i.e.
        // in the SftpInputStream in the operation above...
        if (logger.isInfoEnabled()) {
            logger.info("Move archiveTmpReceivingFile (" + archiveTmpReceivingFile
                    + ") to archiveTmpSendingFile (" + archiveTmpSendingFile + ")...");
        }
        FileUtils.moveFile(archiveTmpReceivingFile, archiveTmpSendingFile);

        if (logger.isDebugEnabled()) {
            logger.debug("Return SftpFileArchiveInputStream for archiveTmpSendingFile ("
                    + archiveTmpSendingFile + ")...");
        }
        return new SftpFileArchiveInputStream(archiveTmpSendingFile, archiveFile);
    }

    private InputStream archiveFile(InputStream is, File archiveFile) throws IOException {
        File archiveFolder = FileUtils.newFile(archiveFile.getParentFile().getPath());
        if (!archiveFolder.exists()) {
            if (logger.isInfoEnabled()) {
                logger.info("Creates " + archiveFolder.getAbsolutePath());
            }
            if (!archiveFolder.mkdirs())
                throw new IOException("Failed to create archive-folder: " + archiveFolder);
        }

        if (logger.isInfoEnabled()) {
            logger.info("Copy SftpInputStream to archiveFile... " + archiveFile.getAbsolutePath());
        }
        sftpUtil.copyStreamToFile(is, archiveFile);

        if (logger.isDebugEnabled()) {
            logger.debug("*** Return SftpFileArchiveInputStream for archiveFile...");
        }
        return new SftpFileArchiveInputStream(archiveFile);
    }

    /**
     * Checks if the file has been changed.
     * <p/>
     * Note! This assumes that the time on both servers are synchronized!
     *
     * @param fileName The file to check
     * @param client instance of StftClient
     * @param fileAge How old the file should be to be considered "old" and not
     *            changed
     * @param sizeCheckDelayMs Wait time (in ms) between size-checks to determine if
     *                         a file is ready to be processed.
     * @return true if the file has changed
     * @throws Exception Error
     */
    protected boolean canProcessFile(String fileName, SftpClient client, long fileAge, long sizeCheckDelayMs)
            throws Exception {
        if (fileAge > 0 && !isOldFile(fileName, client, fileAge)) {
            return false;
        }

        return !(sizeCheckDelayMs > 0 && isSizeModified(fileName, client, sizeCheckDelayMs));
    }

    private boolean isSizeModified(String fileName, SftpClient client, long sizeCheckDelayMs) throws IOException, InterruptedException {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Perform size check with a delay of: " + sizeCheckDelayMs + " ms.");
            }

            long fileSize1 = client.getSize(fileName);
            Thread.sleep(sizeCheckDelayMs);
            long fileSize2 = client.getSize(fileName);

            if (fileSize1 == fileSize2) {
                if (logger.isDebugEnabled()) {
                    logger.debug("File is stable (not growing), ready for retrieval: " + fileName);
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("File is growing, deferring retrieval: " + fileName);
                }
                return true;
            }
            return false;
        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Cannot check if size of file '%s' was modified or not", fileName));
            }

            // Assumes the file size has changed
            return true;
        }
    }

    private boolean isOldFile(String fileName, SftpClient client, long fileAge) throws IOException {
        try {
            long lastModifiedTime = client.getLastModifiedTime(fileName);
            long now = System.currentTimeMillis();
            long diff = now - lastModifiedTime;

            // If the diff is negative it's a sign that the time on the test server
            // and the ftps-server is not synchronized
            if (diff < fileAge) {
                if (logger.isDebugEnabled()) {
                    logger.debug("The file has not aged enough yet, will return nothing for: " + fileName
                            + ". The file must be " + (fileAge - diff) + "ms older, was " + diff);
                }

                return false;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("The file " + fileName + " has aged enough. Was " + diff);
            }
            return true;
        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Cannot check if age of file '%s' is old enough", fileName));
            }

            // Assumes the file is not old enough
            return false;
        }
    }
}
