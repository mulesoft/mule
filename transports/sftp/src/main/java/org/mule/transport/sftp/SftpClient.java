/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.mule.transport.sftp.notification.SftpTransportNotification.SFTP_DELETE_ACTION;
import static org.mule.transport.sftp.notification.SftpTransportNotification.SFTP_GET_ACTION;
import static org.mule.transport.sftp.notification.SftpTransportNotification.SFTP_PUT_ACTION;
import static org.mule.transport.sftp.notification.SftpTransportNotification.SFTP_RENAME_ACTION;

import org.mule.api.MuleEvent;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.OutputHandler;
import org.mule.transport.sftp.notification.SftpNotifier;
import org.mule.util.StringUtils;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>SftpClient</code> Wrapper around jsch sftp library. Provides access to basic
 * sftp commands.
 */

public class SftpClient
{

    public static final String CHANNEL_SFTP = "sftp";
    public static final String STRICT_HOST_KEY_CHECKING = "StrictHostKeyChecking";
    public static final String PREFERRED_AUTHENTICATION_METHODS = "PreferredAuthentications";

    private Log logger = LogFactory.getLog(getClass());

    private ChannelSftp channelSftp;

    private JSch jsch;
    private SftpNotifier notifier;

    private Session session;

    private final String host;

    private int port = 22;

    private String home;

    // Keep track of the current working directory for improved logging.
    private String currentDirectory = "";

    private static final Object lock = new Object();

    private String preferredAuthenticationMethods;

    private int connectionTimeoutMillis = 0; // No timeout by default

    public SftpClient(String host)
    {
        this(host, null);
    }

    public SftpClient(String host, SftpNotifier notifier)
    {
        this.host = host;
        this.notifier = notifier;

        jsch = new JSch();
    }

    public void changeWorkingDirectory(String wd) throws IOException
    {
        currentDirectory = wd;

        try
        {
            wd = getAbsolutePath(wd);
            if (logger.isDebugEnabled())
            {
                logger.debug("Attempting to cwd to: " + wd);
            }
            channelSftp.cd(wd);
        }
        catch (SftpException e)
        {
            String message = "Error '" + e.getMessage() + "' occurred when trying to CDW to '" + wd + "'.";
            logger.error(message);
            throw new IOException(message);
        }
    }

    /**
     * Converts a relative path to an absolute path according to
     * http://tools.ietf.org/html/draft-ietf-secsh-scp-sftp-ssh-uri-04.
     *
     * @param path relative path
     * @return Absolute path
     */
    public String getAbsolutePath(String path)
    {
        if (path.startsWith("/~"))
        {
            return home + path.substring(2, path.length());
        }

        // Already absolute!
        return path;
    }

    public void login(String user, String password) throws IOException
    {
        try
        {
            Properties hash = new Properties();
            hash.put(STRICT_HOST_KEY_CHECKING, "no");
            if (!StringUtils.isEmpty(preferredAuthenticationMethods))
            {
                hash.put(PREFERRED_AUTHENTICATION_METHODS, preferredAuthenticationMethods);
            }

            session = jsch.getSession(user, host);
            session.setConfig(hash);
            session.setPort(port);
            session.setPassword(password);
            session.setTimeout(connectionTimeoutMillis);
            session.connect();

            Channel channel = session.openChannel(CHANNEL_SFTP);
            channel.connect();

            channelSftp = (ChannelSftp) channel;
            setHome(channelSftp.pwd());
        }
        catch (JSchException e)
        {
            logAndThrowLoginError(user, e);
        }
        catch (SftpException e)
        {
            logAndThrowLoginError(user, e);
        }
    }

    public void login(String user, String identityFile, String passphrase) throws IOException
    {
        // Lets first check that the identityFile exist
        if (!new File(identityFile).exists())
        {
            throw new IOException("IdentityFile '" + identityFile + "' not found");
        }

        try
        {
            if (passphrase == null || "".equals(passphrase))
            {
                jsch.addIdentity(new File(identityFile).getAbsolutePath());
            }
            else
            {
                jsch.addIdentity(new File(identityFile).getAbsolutePath(), passphrase);
            }

            Properties hash = new Properties();
            hash.put(STRICT_HOST_KEY_CHECKING, "no");
            if (!StringUtils.isEmpty(preferredAuthenticationMethods))
            {
                hash.put(PREFERRED_AUTHENTICATION_METHODS, preferredAuthenticationMethods);
            }

            session = jsch.getSession(user, host);
            session.setConfig(hash);
            session.setPort(port);
            session.setTimeout(connectionTimeoutMillis);
            session.connect();

            Channel channel = session.openChannel(CHANNEL_SFTP);
            channel.connect();

            channelSftp = (ChannelSftp) channel;
            setHome(channelSftp.pwd());
        }
        catch (JSchException e)
        {
            logAndThrowLoginError(user, e);
        }
        catch (SftpException e)
        {
            logAndThrowLoginError(user, e);
        }
    }

    private void logAndThrowLoginError(String user, Exception e) throws IOException
    {
        logger.error("Error during login to " + user + "@" + host, e);
        throw new IOException("Error during login to " + user + "@" + host + ": " + e.getMessage());
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public void setConnectionTimeoutMillis(int connectionTimeoutMillis)
    {
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }

    public void rename(String filename, String dest) throws IOException
    {
        // Notify sftp rename file action
        if (notifier != null)
        {
            notifier.notify(SFTP_RENAME_ACTION, "from: " + currentDirectory + "/" + filename + " - to: "
                                                + dest);
        }

        String absolutePath = getAbsolutePath(dest);
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Will try to rename " + currentDirectory + "/" + filename + " to "
                             + absolutePath);
            }
            channelSftp.rename(filename, absolutePath);
        }
        catch (SftpException e)
        {
            throw new IOException(e.getMessage());
            // throw new IOException("Error occured when renaming " +
            // currentDirectory + "/" + filename + " to " + absolutePath +
            // ". Error Message=" + e.getMessage());
        }
    }

    public void deleteFile(String fileName) throws IOException
    {
        // Notify sftp delete file action
    	notifyAction(SFTP_DELETE_ACTION, fileName);

        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Will try to delete " + fileName);
            }
            channelSftp.rm(fileName);
        }
        catch (SftpException e)
        {
            throw new IOException(e);
        }
    }

    public void disconnect()
    {
        if (channelSftp != null)
        {
            channelSftp.disconnect();
        }
        if ((session != null) && session.isConnected())
        {
            session.disconnect();
        }
    }

    public boolean isConnected()
    {
        return (channelSftp != null) && channelSftp.isConnected() && !channelSftp.isClosed()
               && (session != null) && session.isConnected();
    }

    public String[] listFiles() throws IOException
    {
        return listFiles(".");
    }

    public String[] listFiles(String path) throws IOException
    {
        return listDirectory(path, true, false);
    }

    public String[] listDirectories() throws IOException
    {
        return listDirectory(".", false, true);
    }

    public String[] listDirectories(String path) throws IOException
    {
        return listDirectory(path, false, true);
    }

    private String[] listDirectory(String path, boolean includeFiles, boolean includeDirectories)
            throws IOException
    {
        try
        {
            Vector<LsEntry> entries = channelSftp.ls(path);
            if (entries != null)
            {
                List<String> ret = new ArrayList<String>();
                for (LsEntry entry : entries)
                {
                    if (includeFiles && !entry.getAttrs().isDir())
                    {
                        ret.add(entry.getFilename());
                    }
                    if (includeDirectories && entry.getAttrs().isDir())
                    {
                        if (!entry.getFilename().equals(".") && !entry.getFilename().equals(".."))
                        {
                            ret.add(entry.getFilename());
                        }
                    }
                }
                return ret.toArray(new String[ret.size()]);
            }
        }
        catch (SftpException e)
        {
            throw new IOException(e.getMessage(), e);
        }
        return null;
    }

    // public boolean logout()
    // {
    // return true;
    // }

    public InputStream retrieveFile(String fileName) throws IOException
    {
        // Notify sftp get file action
        long size = getSize(fileName);
        if (notifier != null)
        {
            notifier.notify(SFTP_GET_ACTION, currentDirectory + "/" + fileName, size);
        }

        try
        {
            return channelSftp.get(fileName);
        }
        catch (SftpException e)
        {
            throw new IOException(e.getMessage() + ".  Filename is " + fileName);
        }
    }

    // public OutputStream storeFileStream(String fileName) throws IOException
    // {
    // try
    // {
    // return channelSftp.put(fileName);
    // } catch (SftpException e)
    // {
    // throw new IOException(e.getMessage());
    // }
    // }

    public void storeFile(String fileName, InputStream stream) throws IOException
    {
        storeFile(fileName, stream, WriteMode.OVERWRITE);
    }

    public void storeFile(String fileName, InputStream stream, WriteMode mode) throws IOException
    {
        try
        {

            // Notify sftp put file action
        	notifyAction(SFTP_PUT_ACTION, fileName);

            if (logger.isDebugEnabled())
            {
                logger.debug("Sending to SFTP service: Stream = " + stream + " , filename = " + fileName);
            }

            channelSftp.put(stream, fileName, mode.intValue());
        }
        catch (SftpException e)
        {
            throw new IOException(e.getMessage());
        }
    }

    public void storeFile(String fileName, MuleEvent event, OutputHandler outputHandler) throws IOException
    {
    	storeFile(fileName, event, outputHandler, WriteMode.OVERWRITE);
    }

    public void storeFile(String fileName, MuleEvent event, OutputHandler outputHandler, WriteMode mode) throws IOException
    {
        OutputStream os = null;
        try
        {

            // Notify sftp put file action
            notifyAction(SFTP_PUT_ACTION, fileName);

            if (logger.isDebugEnabled())
            {
                logger.debug("Sending to SFTP service: OutputHandler = " + outputHandler + " , filename = " + fileName);
            }

            os = channelSftp.put(fileName, mode.intValue());
            outputHandler.write(event, os);
        }
        catch (SftpException e)
        {
            throw new IOException(e.getMessage());
        }
        finally
        {
            if (os != null)
            {
                os.close();
            }
        }
    }
    
    public void storeFile(String fileNameLocal, String fileNameRemote) throws IOException
    {
        storeFile(fileNameLocal, fileNameRemote, WriteMode.OVERWRITE);
    }

    public void storeFile(String fileNameLocal, String fileNameRemote, WriteMode mode) throws IOException
    {
        try
        {
            channelSftp.put(fileNameLocal, fileNameRemote, mode.intValue());
        }
        catch (SftpException e)
        {
            throw new IOException(e.getMessage());
        }
    }

    private void notifyAction(int action, String fileName)
    {
        if (notifier != null)
        {
            notifier.notify(action, currentDirectory + "/" + fileName);
        }
    }
    
    public long getSize(String filename) throws IOException
    {
        try
        {
            return channelSftp.stat(filename).getSize();
        }
        catch (SftpException e)
        {
            throw new IOException(e.getMessage() + " (" + currentDirectory + "/" + filename + ")");
        }
    }

    /**
     * @param filename File name
     * @return Number of seconds since the file was written to
     * @throws IOException If an error occurs
     */
    public long getLastModifiedTime(String filename) throws IOException
    {
        try
        {
            SftpATTRS attrs = channelSftp.stat("./" + filename);
            return attrs.getMTime() * 1000L;
        }
        catch (SftpException e)
        {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Creates a directory
     *
     * @param directoryName The directory name
     * @throws IOException If an error occurs
     */
    public void mkdir(String directoryName) throws IOException
    {
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Will try to create directory " + directoryName);
            }
            channelSftp.mkdir(directoryName);
        }
        catch (SftpException e)
        {
            // Don't throw e.getmessage since we only get "2: No such file"..
            throw new IOException("Could not create the directory '" + directoryName + "', caused by: "
                                  + e.getMessage());
            // throw new IOException("Could not create the directory '" +
            // directoryName + "' in '" + currentDirectory + "', caused by: " +
            // e.getMessage());
        }
    }

    public void deleteDirectory(String path) throws IOException
    {
        path = getAbsolutePath(path);
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Will try to delete directory " + path);
            }
            channelSftp.rmdir(path);
        }
        catch (SftpException e)
        {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Setter for 'home'
     *
     * @param home The path to home
     */
    void setHome(String home)
    {
        this.home = home;
    }

    /**
     * @return the ChannelSftp - useful for some tests
     */
    public ChannelSftp getChannelSftp()
    {
        return channelSftp;
    }

    /**
     * Creates the directory if it not already exists. TODO: check if the SftpUtil &
     * SftpClient methods can be merged Note, this method is synchronized because it
     * in rare cases can be called from two threads at the same time and thus cause
     * an error.
     *
     * @param endpoint
     * @param newDir
     * @throws IOException
     */
    public void createSftpDirIfNotExists(ImmutableEndpoint endpoint, String newDir) throws IOException
    {
        String newDirAbs = endpoint.getEndpointURI().getPath() + "/" + newDir;

        String currDir = currentDirectory;

        if (logger.isDebugEnabled())
        {
            logger.debug("CHANGE DIR FROM " + currentDirectory + " TO " + newDirAbs);
        }

        // We need to have a synchronized block if two++ threads tries to
        // create the same directory at the same time
        synchronized (lock)
        {
            // Try to change directory to the new dir, if it fails - create it
            try
            {
                // This method will throw an exception if the directory does not
                // exist.
                changeWorkingDirectory(newDirAbs);
            }
            catch (IOException e)
            {
                logger.info("Got an exception when trying to change the working directory to the new dir. "
                            + "Will try to create the directory " + newDirAbs);
                changeWorkingDirectory(endpoint.getEndpointURI().getPath());
                mkdir(newDir);

                // Now it should exist!
                changeWorkingDirectory(newDirAbs);
            }
            finally
            {
                changeWorkingDirectory(currDir);
                if (logger.isDebugEnabled())
                {
                    logger.debug("DIR IS NOW BACK TO " + currentDirectory);
                }
            }
        }
    }

    public String duplicateHandling(String destDir, String filename, String duplicateHandling)
            throws IOException
    {
        if (duplicateHandling.equals(SftpConnector.PROPERTY_DUPLICATE_HANDLING_ASS_SEQ_NO))
        {
            filename = createUniqueName(destDir, filename);
        }
        else if (duplicateHandling.equals(SftpConnector.PROPERTY_DUPLICATE_HANDLING_THROW_EXCEPTION))
        {
            if (fileAlreadyExists(destDir, filename))
            {
                throw new IOException("File already exists: " + filename);
            }
        }
        return filename;
    }

    private boolean fileAlreadyExists(String destDir, String filename) throws IOException
    {
        logger.warn("listing files for: " + destDir + "/" + filename);
        String[] files = listFiles(destDir);
        for (String file : files)
        {
            if (file.equals(filename))
            {
                return true;
            }
        }
        return false;
    }

    private String createUniqueName(String dir, String path) throws IOException
    {
        int fileIdx = 1;

        String filename;
        String fileType;
        int fileTypeIdx = path.lastIndexOf('.');
        if (fileTypeIdx == -1)
        {
            // No file type/extension found
            filename = path;
            fileType = "";
        }
        else
        {
            fileType = path.substring(fileTypeIdx); // Let the fileType include the
            // leading '.'
            filename = path.substring(0, fileTypeIdx);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Create a unique name for: " + path + " (" + dir + " - " + filename + " - "
                         + fileType + ")");
        }

        String uniqueFilename = filename;
        String[] existingFiles = listFiles(getAbsolutePath(dir));

        while (existsFile(existingFiles, uniqueFilename, fileType))
        {
            uniqueFilename = filename + '_' + fileIdx++;
        }

        uniqueFilename = uniqueFilename + fileType;
        if (!path.equals(uniqueFilename) && logger.isInfoEnabled())
        {
            logger.info("A file with the original filename (" + dir + "/" + path
                        + ") already exists, new name: " + uniqueFilename);
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Unique name returned: " + uniqueFilename);
        }
        return uniqueFilename;
    }

    private boolean existsFile(String[] files, String filename, String fileType)
    {
        boolean existsFile = false;
        filename += fileType;
        for (String file : files)
        {
            if (file.equals(filename))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Found existing file: " + file);
                }
                existsFile = true;
            }
        }
        return existsFile;
    }

    public void chmod(String path, int permissions) throws SftpException
    {
        path = getAbsolutePath(path);
        if (logger.isDebugEnabled())
        {
            logger.debug("Will try to chmod directory '" + path + "' to permission " + permissions);
        }
        channelSftp.chmod(permissions, path);
    }

    public void setNotifier(SftpNotifier notifier)
    {
        this.notifier = notifier;
    }

    public String getHost()
    {
        return host;
    }

    public void recursivelyDeleteDirectory(String dir) throws IOException
    {
        this.changeWorkingDirectory(dir);
        String[] directories = this.listDirectories();
        String[] files = this.listFiles();
        for (int i = 0; i < directories.length; i++)
        {
            recursivelyDeleteDirectory(directories[i]);
        }
        for (int i = 0; i < files.length; i++)
        {
            deleteFile(files[i]);
        }
        this.changeWorkingDirectory("..");
        this.deleteDirectory(dir);
    }

    public void setPreferredAuthenticationMethods(String preferredAuthenticationMethods)
    {
        this.preferredAuthenticationMethods = preferredAuthenticationMethods;
    }

    public enum WriteMode
    {
        APPEND
                {
                    @Override
                    public int intValue()
                    {
                        return ChannelSftp.APPEND;
                    }
                },
        OVERWRITE
                {
                    @Override
                    public int intValue()
                    {
                        return ChannelSftp.OVERWRITE;
                    }
                };

        public abstract int intValue();
    }
}
