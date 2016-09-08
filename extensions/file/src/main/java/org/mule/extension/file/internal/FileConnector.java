/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal;

import static java.lang.String.format;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.extension.file.api.DeletedFileAttributes;
import org.mule.extension.file.api.EventedFileAttributes;
import org.mule.extension.file.api.FileEventType;
import org.mule.extension.file.api.ListenerFileAttributes;
import org.mule.extension.file.api.LocalFileAttributes;
import org.mule.extension.file.api.LocalFilePredicateBuilder;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.connector.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.FilePredicateBuilder;
import org.mule.extension.file.common.api.StandardFileSystemOperations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File connector used to manipulate file systems mounted on the host operation system.
 * <p>
 * This class serves as both extension definition and configuration. Operations are based on the standard
 * {@link StandardFileSystemOperations}
 *
 * @since 4.0
 */
@Extension(name = "File", description = "Connector to manipulate files on a locally mounted file system")
@Operations({StandardFileSystemOperations.class})
@SubTypeMapping(baseType = FilePredicateBuilder.class, subTypes = LocalFilePredicateBuilder.class)
@ConnectionProviders(LocalFileConnectionProvider.class)
@Sources(DirectoryListener.class)
@Export(classes = {LocalFileAttributes.class, FileEventType.class, ListenerFileAttributes.class, EventedFileAttributes.class,
    DeletedFileAttributes.class})
public class FileConnector extends FileConnectorConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileConnector.class);

  /**
   * The directory to be considered as the root of every relative path used with this connector. If not provided, it will default
   * to the value of the {@code user.home} system property. If that system property is not set, then the connector will fail to
   * initialise.
   */
  @Parameter
  @Optional
  @DisplayName("Working Directory")
  @Summary("Directory to be considered as the root of every relative path used with this connector")
  private String workingDir;

  @Override
  protected void doInitialise() throws InitialisationException {
    validateWorkingDir();
  }

  private void validateWorkingDir() throws InitialisationException {
    if (workingDir == null) {
      workingDir = System.getProperty("user.home");
      if (workingDir == null) {
        throw new InitialisationException(createStaticMessage("Could not obtain user's home directory. Please provide a explicit value for the workingDir parameter"),
                                          this);
      }

      LOGGER.warn("File connector '{}' does not specify the workingDir property. Defaulting to '{}'", getConfigName(),
                  workingDir);
    }
    Path workingDirPath = Paths.get(workingDir);
    if (Files.notExists(workingDirPath)) {
      throw new InitialisationException(createStaticMessage(format("Provided workingDir '%s' does not exists",
                                                                   workingDirPath.toAbsolutePath())),
                                        this);
    }
    if (!Files.isDirectory(workingDirPath)) {
      throw new InitialisationException(createStaticMessage(format("Provided workingDir '%s' is not a directory",
                                                                   workingDirPath.toAbsolutePath())),
                                        this);
    }
  }

  /**
   * {@inheritDoc}
   */
  public String getWorkingDir() {
    return workingDir;
  }
}
