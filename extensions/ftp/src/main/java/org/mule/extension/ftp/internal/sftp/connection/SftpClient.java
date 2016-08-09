/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.sftp.connection;

import static java.lang.String.format;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.extension.ftp.api.sftp.SftpFileAttributes;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.module.extension.file.api.FileWriteMode;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around jsch sftp library which provides access to basic sftp commands.
 *
 * @since 4.0
 */
public class SftpClient {

  private static Logger LOGGER = LoggerFactory.getLogger(SftpClient.class);

  public static final String CHANNEL_SFTP = "sftp";
  public static final String STRICT_HOST_KEY_CHECKING = "StrictHostKeyChecking";
  public static final String PREFERRED_AUTHENTICATION_METHODS = "PreferredAuthentications";


  private ChannelSftp sftp;
  private JSch jsch;
  private Session session;
  private final String host;
  private int port = 22;
  private String password;
  private String identityFile;
  private String passphrase;
  private String knownHostsFile;
  private String preferredAuthenticationMethods;
  private long connectionTimeoutMillis = 0; // No timeout by default

  /**
   * Creates a new instance which connects to a server on a given {@code host} and {@code port}
   *
   * @param host the host address
   * @param port the remote connection port
   * @param jSchSupplier a {@link Supplier} for obtaining a {@link JSch} client
   */
  public SftpClient(String host, int port, Supplier<JSch> jSchSupplier) {
    this.host = host;
    this.port = port;

    jsch = jSchSupplier.get();
  }

  /**
   * @return the current working directory
   */
  public String getWorkingDirectory() {
    try {
      return sftp.pwd();
    } catch (SftpException e) {
      throw exception("Could not obtain current working directory", e);
    }
  }

  /**
   * Changes the current working directory to {@code wd}
   *
   * @param path the new working directory path
   */
  public void changeWorkingDirectory(String path) {
    LOGGER.debug("Attempting to cwd to: {}", path);

    try {
      sftp.cd(path);
    } catch (SftpException e) {
      throw exception("Exception occurred while trying to change working directory to " + path, e);
    }
  }

  /**
   * Gets the attributes for the file in the given {code path}
   *
   * @param path the file's path
   * @return a {@link SftpFileAttributes} or {@code null} if the file doesn't exists.
   */
  public SftpFileAttributes getAttributes(Path path) {
    try {
      return new SftpFileAttributes(path, sftp.stat(path.toString()));
    } catch (SftpException e) {
      if (e.getMessage().contains(FileNotFoundException.class.getName())) {
        return null;
      }
      throw exception("Could not obtain attributes for path " + path, e);
    }
  }

  /**
   * Performs a login operation for the given {@code user} using the connection options and additional credentials optionally set
   * on this client
   *
   * @param user the authentication user
   */
  public void login(String user) throws IOException {
    try {
      configureSession(user);
      if (!StringUtils.isEmpty(password)) {
        session.setPassword(password);
      }

      if (!StringUtils.isEmpty(identityFile)) {
        setupIdentity();
      }

      connect();
    } catch (Exception e) {
      throw loginException(user, e);
    }
  }

  private void setupIdentity() throws JSchException {
    if (passphrase == null || "".equals(passphrase)) {
      jsch.addIdentity(identityFile);
    } else {
      jsch.addIdentity(identityFile, passphrase);
    }
  }

  private void checkExists(String path) {
    if (!new File(path).exists()) {
      throw new IllegalArgumentException(format("File '%s' not found", path));
    }
  }

  private void connect() throws JSchException {
    session.connect();
    Channel channel = session.openChannel(CHANNEL_SFTP);
    channel.connect();

    sftp = (ChannelSftp) channel;
  }

  private void configureSession(String user) throws JSchException {
    Properties hash = new Properties();
    configureHostChecking(hash);
    if (!StringUtils.isEmpty(preferredAuthenticationMethods)) {
      hash.put(PREFERRED_AUTHENTICATION_METHODS, preferredAuthenticationMethods);
    }

    session = jsch.getSession(user, host);
    session.setConfig(hash);
    session.setPort(port);
    session.setTimeout(Long.valueOf(connectionTimeoutMillis).intValue());
  }

  private void configureHostChecking(Properties hash) throws JSchException {
    if (knownHostsFile == null) {
      hash.put(STRICT_HOST_KEY_CHECKING, "no");
    } else {
      checkExists(knownHostsFile);
      hash.put(STRICT_HOST_KEY_CHECKING, "ask");
      jsch.setKnownHosts(knownHostsFile);
    }
  }

  /**
   * Renames the file at {@code sourcePath} to {@code target}
   *
   * @param sourcePath the path to the renamed file
   * @param target the new path
   */
  public void rename(String sourcePath, String target) throws IOException {
    try {
      sftp.rename(sourcePath, target);
    } catch (SftpException e) {
      throw exception(format("Could not rename path '%s' to '%s'", sourcePath, target), e);
    }
  }

  /**
   * Deletes the file at the given {@code path}
   *
   * @param path the path to the file to be deleted
   */
  public void deleteFile(String path) {

    try {
      sftp.rm(path);
    } catch (SftpException e) {
      throw exception("Could not delete file " + path, e);
    }
  }

  /**
   * Closes the active session and severs the connection (if any of those were active)
   */
  public void disconnect() {
    if (sftp != null && sftp.isConnected()) {
      sftp.exit();
      sftp.disconnect();
    }

    if (session != null && session.isConnected()) {
      session.disconnect();
    }
  }

  /**
   * @return whether this client is currently connected and logged into the remote server
   */
  public boolean isConnected() {
    return sftp != null && sftp.isConnected() && !sftp.isClosed() && session != null && session.isConnected();
  }

  /**
   * Lists the contents of the directory at the given {@code path}
   *
   * @param path the path to list
   * @return a immutable {@link List} of {@Link SftpFileAttributes}. Might be empty but will never be {@code null}
   */
  public List<SftpFileAttributes> list(String path) {
    List<ChannelSftp.LsEntry> entries;
    try {
      entries = sftp.ls(path);
    } catch (SftpException e) {
      throw exception("Found exception trying to list path " + path, e);
    }

    if (isEmpty(entries)) {
      return ImmutableList.of();
    }

    return entries.stream().map(entry -> new SftpFileAttributes(Paths.get(path).resolve(entry.getFilename()), entry.getAttrs()))
        .collect(new ImmutableListCollector<>());
  }

  /**
   * An {@link InputStream} with the contents of the file at the given {@code path}
   *
   * @param path the path to the file to read
   * @return an {@link InputStream}
   */
  public InputStream getFileContent(String path) {
    try {
      return sftp.get(path);
    } catch (SftpException e) {
      throw exception("Exception was found trying to retrieve the contents of file " + path, e);
    }
  }

  /**
   * Writes the contents of the {@code stream} into the file at the given {@code path}
   *
   * @param path the path to write into
   * @param stream the content to be written
   * @param mode the write mode
   * @throws Exception if anything goes wrong
   */
  public void write(String path, InputStream stream, FileWriteMode mode) throws Exception {
    sftp.put(stream, path, toInt(mode));
  }

  /**
   * Opens an {@link OutputStream} which allows writing into the file pointed by {@code path}
   *
   * @param path the path to write into
   * @param mode the write mode
   * @return an {@link OutputStream}
   */
  public OutputStream getOutputStream(String path, FileWriteMode mode) throws Exception {
    return sftp.put(path, toInt(mode));
  }

  private int toInt(FileWriteMode mode) {
    return mode == FileWriteMode.APPEND ? ChannelSftp.APPEND : ChannelSftp.OVERWRITE;
  }

  /**
   * Creates a directory
   *
   * @param directoryName The directory name
   * @throws IOException If an error occurs
   */
  public void mkdir(String directoryName) {
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Trying to create directory " + directoryName);
      }
      sftp.mkdir(directoryName);
    } catch (SftpException e) {
      throw exception("Could not create the directory " + directoryName, e);
    }
  }

  /**
   * Deletes the directory at {@code path}.
   * <p>
   * The directory is expected to be empty
   *
   * @param path the path of the directory to be deleted
   */
  public void deleteDirectory(String path) {
    try {
      sftp.rmdir(path);
    } catch (SftpException e) {
      throw exception("Could not delete directory " + path, e);
    }
  }

  public String getHost() {
    return host;
  }

  public void setPreferredAuthenticationMethods(String preferredAuthenticationMethods) {
    this.preferredAuthenticationMethods = preferredAuthenticationMethods;
  }

  protected RuntimeException exception(String message, Exception cause) {
    return new MuleRuntimeException(createStaticMessage(message), cause);
  }

  private RuntimeException loginException(String user, Exception e) {
    return exception(format("Error during login to %s@%s", user, host), e);
  }

  public void setKnownHostsFile(String knownHostsFile) {
    this.knownHostsFile = !StringUtils.isEmpty(knownHostsFile) ? new File(knownHostsFile).getAbsolutePath() : knownHostsFile;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setIdentity(String identityFilePath, String passphrase) {
    if (!StringUtils.isEmpty(identityFilePath)) {
      this.identityFile = new File(identityFilePath).getAbsolutePath();
      checkExists(identityFilePath);
    }
    this.passphrase = passphrase;
  }

  public int getPort() {
    return port;
  }

  public void setConnectionTimeoutMillis(long connectionTimeoutMillis) {
    this.connectionTimeoutMillis = connectionTimeoutMillis;
  }
}
