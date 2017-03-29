/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.api;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.extension.file.common.api.AbstractFileAttributes;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.runtime.api.exception.MuleRuntimeException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Implementation of {@link FileAttributes} for files obtained from a local file system.
 *
 * @since 4.0
 */
public class LocalFileAttributes extends AbstractFileAttributes {

  private LocalDateTime lastModifiedTime;
  private LocalDateTime lastAccessTime;
  private LocalDateTime creationTime;
  private long size;
  private boolean regularFile;
  private boolean directory;
  private boolean symbolicLink;

  /**
   * {@inheritDoc}
   */
  public LocalFileAttributes(Path path) {
    super(path);
    initAttributes(path);
  }

  protected void initAttributes(Path path) {
    BasicFileAttributes attributes = getAttributes(path);
    this.lastModifiedTime = asDateTime(attributes.lastModifiedTime());
    this.lastAccessTime = asDateTime(attributes.lastAccessTime());
    this.creationTime = asDateTime(attributes.creationTime());
    this.size = attributes.size();
    this.regularFile = attributes.isRegularFile();
    this.directory = attributes.isDirectory();
    this.symbolicLink = Files.isSymbolicLink(Paths.get(getPath()));
  }

  /**
   * @return The last time the file was modified
   */
  public LocalDateTime getLastModifiedTime() {
    return lastModifiedTime;
  }

  /**
   * @return The last time the file was accessed
   */
  public LocalDateTime getLastAccessTime() {
    return lastAccessTime;
  }

  /**
   * @return the time at which the file was created
   */
  public LocalDateTime getCreationTime() {
    return creationTime;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getSize() {
    return size;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRegularFile() {
    return regularFile;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDirectory() {
    return directory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSymbolicLink() {
    return symbolicLink;
  }

  private BasicFileAttributes getAttributes(Path path) {
    try {
      return Files.readAttributes(path, BasicFileAttributes.class);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not read attributes for file " + path), e);
    }
  }

  private LocalDateTime asDateTime(FileTime fileTime) {
    return LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
  }
}
