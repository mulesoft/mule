/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
public class SftpReceiverRequesterUtil
{
    private transient Log logger = LogFactory.getLog(getClass());

    private final SftpConnector connector;
    private final ImmutableEndpoint endpoint;
    private final FilenameFilter filenameFilter;
    private final SftpUtil sftpUtil;

    public SftpReceiverRequesterUtil(ImmutableEndpoint endpoint)
    {
        this.endpoint = endpoint;
        this.connector = (SftpConnector) endpoint.getConnector();

        sftpUtil = new SftpUtil(endpoint);

        if (endpoint.getFilter() instanceof FilenameFilter)
        {
            this.filenameFilter = (FilenameFilter) endpoint.getFilter();
        }
        else
        {
            this.filenameFilter = null;
        }

    }

    // Get files in directory configured on the endpoint
    public String[] getAvailableFiles(boolean onlyGetTheFirstOne) throws Exception
    {
        // This sftp client instance is only for checking available files. This
        // instance cannot be shared
        // with clients that retrieve files because of thread safety

        if (logger.isDebugEnabled())
        {
            logger.debug("Checking files at endpoint " + endpoint.getEndpointURI());
        }

        SftpClient client = null;

        try
        {
            client = connector.createSftpClient(endpoint);

            long fileAge = connector.getFileAge();
            boolean checkFileAge = connector.getCheckFileAge();

            // Override the value from the Endpoint?
            if (endpoint.getProperty(SftpConnector.PROPERTY_FILE_AGE) != null)
            {
                checkFileAge = true;
                fileAge = Long.valueOf((String) endpoint.getProperty(SftpConnector.PROPERTY_FILE_AGE));
            }

            logger.debug("fileAge : " + fileAge);

            // Get size check parameter
            long sizeCheckDelayMs = sftpUtil.getSizeCheckWaitTime();

            String[] files = client.listFiles();

            // Only return files that have completely been written and match
            // fileExtension
            List<String> completedFiles = new ArrayList<String>(files.length);

            for (String file : files)
            {
                // Skip if no match.
                // Note, Mule also uses this filter. We use the filter here because
                // we don't want to
                // run the below tests (checkFileAge, sizeCheckDelayMs etc) for files
                // that Mule
                // later should have ignored. Thus this is an "early" filter so that
                // improves performance.
                if (filenameFilter != null && !filenameFilter.accept(null, file))
                {
                    continue;
                }

                if (checkFileAge || sizeCheckDelayMs >= 0)
                {
                    // See if the file is still growing (either by age or size),
                    // leave it alone if it is
                    if (!hasChanged(file, client, fileAge, sizeCheckDelayMs))
                    {
                        // logger.debug("marking file [" + files[i] +
                        // "] as in transit.");
                        // client.rename(files[i], files[i] + ".transtit");
                        // completedFiles.add( files[i] + ".transtit" );
                        completedFiles.add(file);
                        if (onlyGetTheFirstOne)
                        {
                            break;
                        }
                    }
                }
                else
                {
                    completedFiles.add(file);
                    if (onlyGetTheFirstOne)
                    {
                        break;
                    }
                }
            }
            return completedFiles.toArray(new String[completedFiles.size()]);
        }
        finally
        {
            if (client != null)
            {
                connector.releaseClient(endpoint, client);
            }
        }
    }

    public InputStream retrieveFile(String fileName, SftpNotifier notifier) throws Exception
    {
        // Getting a new SFTP client dedicated to the SftpInputStream below
        SftpClient client = connector.createSftpClient(endpoint, notifier);

        // Check usage of tmpSendingDir
        String tmpSendingDir = sftpUtil.getTempDirInbound();
        if (tmpSendingDir != null)
        {
            // Check usage of unique names of files during transfer
            boolean addUniqueSuffix = sftpUtil.isUseTempFileTimestampSuffix();

            // TODO: is it possibly to move this to some kind of init method?
            client.createSftpDirIfNotExists(endpoint, tmpSendingDir);
            String tmpSendingFileName = tmpSendingDir + "/" + fileName;

            if (addUniqueSuffix)
            {
                tmpSendingFileName = sftpUtil.createUniqueSuffix(tmpSendingFileName);
            }
            String fullTmpSendingPath = endpoint.getEndpointURI().getPath() + "/" + tmpSendingFileName;

            if (logger.isDebugEnabled())
            {
                logger.debug("Move " + fileName + " to " + fullTmpSendingPath);
            }
            client.rename(fileName, fullTmpSendingPath);
            fileName = tmpSendingFileName;
            if (logger.isDebugEnabled())
            {
                logger.debug("Move done");
            }
        }

        // Archive functionality...
        String archive = sftpUtil.getArchiveDir();

        // Retrieve the file stream
        InputStream fileInputStream = client.retrieveFile(fileName);

        if (!"".equals(archive))
        {
            String archiveTmpReceivingDir = sftpUtil.getArchiveTempReceivingDir();
            String archiveTmpSendingDir = sftpUtil.getArchiveTempSendingDir();

            InputStream is = new SftpInputStream(client, fileInputStream, fileName, connector.isAutoDelete(),
                endpoint);

            // TODO ML FIX. Refactor to util-class...
            int idx = fileName.lastIndexOf('/');
            String fileNamePart = fileName.substring(idx + 1);

            // don't use new File() directly, see MULE-1112
            File archiveFile = FileUtils.newFile(archive, fileNamePart);

            // Should temp dirs be used when handling the archive file?
            if ("".equals(archiveTmpReceivingDir) || "".equals(archiveTmpSendingDir))
            {
                return archiveFile(is, archiveFile);
            }
            else
            {
                return archiveFileUsingTempDirs(archive, archiveTmpReceivingDir, archiveTmpSendingDir, is,
                    fileNamePart, archiveFile);
            }
        }

        // This special InputStream closes the SftpClient when the stream is closed.
        // The stream will be materialized in a Message Dispatcher or Service
        // Component
        return new SftpInputStream(client, fileInputStream, fileName, connector.isAutoDelete(), endpoint);
    }

    private InputStream archiveFileUsingTempDirs(String archive,
                                                 String archiveTmpReceivingDir,
                                                 String archiveTmpSendingDir,
                                                 InputStream is,
                                                 String fileNamePart,
                                                 File archiveFile) throws IOException
    {

        File archiveTmpReceivingFolder = FileUtils.newFile(archive + '/' + archiveTmpReceivingDir);
        File archiveTmpReceivingFile = FileUtils.newFile(archive + '/' + archiveTmpReceivingDir, fileNamePart);
        if (!archiveTmpReceivingFolder.exists())
        {
            if (logger.isInfoEnabled())
            {
                logger.info("Creates " + archiveTmpReceivingFolder.getAbsolutePath());
            }
            if (!archiveTmpReceivingFolder.mkdirs())
                throw new IOException("Failed to create archive-tmp-receiving-folder: "
                                      + archiveTmpReceivingFolder);
        }

        File archiveTmpSendingFolder = FileUtils.newFile(archive + '/' + archiveTmpSendingDir);
        File archiveTmpSendingFile = FileUtils.newFile(archive + '/' + archiveTmpSendingDir, fileNamePart);
        if (!archiveTmpSendingFolder.exists())
        {
            if (logger.isInfoEnabled())
            {
                logger.info("Creates " + archiveTmpSendingFolder.getAbsolutePath());
            }
            if (!archiveTmpSendingFolder.mkdirs())
                throw new IOException("Failed to create archive-tmp-sending-folder: "
                                      + archiveTmpSendingFolder);
        }

        if (logger.isInfoEnabled())
        {
            logger.info("Copy SftpInputStream to archiveTmpReceivingFile... "
                        + archiveTmpReceivingFile.getAbsolutePath());
        }
        sftpUtil.copyStreamToFile(is, archiveTmpReceivingFile);

        // TODO. ML FIX. Should be performed before the sftp:delete - operation, i.e.
        // in the SftpInputStream in the operation above...
        if (logger.isInfoEnabled())
        {
            logger.info("Move archiveTmpReceivingFile (" + archiveTmpReceivingFile
                        + ") to archiveTmpSendingFile (" + archiveTmpSendingFile + ")...");
        }
        FileUtils.moveFile(archiveTmpReceivingFile, archiveTmpSendingFile);

        if (logger.isDebugEnabled())
        {
            logger.debug("Return SftpFileArchiveInputStream for archiveTmpSendingFile ("
                         + archiveTmpSendingFile + ")...");
        }
        return new SftpFileArchiveInputStream(archiveTmpSendingFile, archiveFile);
    }

    private InputStream archiveFile(InputStream is, File archiveFile) throws IOException
    {
        File archiveFolder = FileUtils.newFile(archiveFile.getParentFile().getPath());
        if (!archiveFolder.exists())
        {
            if (logger.isInfoEnabled())
            {
                logger.info("Creates " + archiveFolder.getAbsolutePath());
            }
            if (!archiveFolder.mkdirs())
                throw new IOException("Failed to create archive-folder: " + archiveFolder);
        }

        if (logger.isInfoEnabled())
        {
            logger.info("Copy SftpInputStream to archiveFile... " + archiveFile.getAbsolutePath());
        }
        sftpUtil.copyStreamToFile(is, archiveFile);

        if (logger.isDebugEnabled())
        {
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
     *            a file is ready to be processed.
     * @return true if the file has changed
     * @throws Exception Error
     */
    private boolean hasChanged(String fileName, SftpClient client, long fileAge, long sizeCheckDelayMs)
        throws Exception
    {
        // Perform fileAge test if configured
        // Note that for this to work it is required that the system clock on the
        // mule server
        // is synchronized with the system clock on the sftp server
        if (fileAge > 0)
        {
            long lastModifiedTime = client.getLastModifiedTime(fileName);
            // TODO Can we get the current time from the other server?
            long now = System.currentTimeMillis();
            long diff = now - lastModifiedTime;
            // If the diff is negative it's a sign that the time on the test server
            // and the ftps-server is not synchronized
            if (diff < fileAge)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("The file has not aged enough yet, will return nothing for: " + fileName
                                 + ". The file must be " + (fileAge - diff) + "ms older, was " + diff);
                }
                return true;
            }
            if (logger.isDebugEnabled())
            {
                logger.debug("The file " + fileName + " has aged enough. Was " + diff);
            }
        }

        // Perform a size check with a short configurable latencey between the
        // size-calls
        // Take consecutive file size snapshots to determine if file is still being
        // written
        if (sizeCheckDelayMs > 0)
        {
            logger.info("Perform size check with a delay of: " + sizeCheckDelayMs + " ms.");
            long fileSize1 = client.getSize(fileName);
            Thread.sleep(sizeCheckDelayMs);
            long fileSize2 = client.getSize(fileName);

            if (fileSize1 == fileSize2)
            {
                logger.info("File is stable (not growing), ready for retrieval: " + fileName);
            }
            else
            {
                logger.info("File is growing, deferring retrieval: " + fileName);
                return true;
            }
        }

        // None of file-change tests faile so we can retrieve this file
        return false;
    }
}
