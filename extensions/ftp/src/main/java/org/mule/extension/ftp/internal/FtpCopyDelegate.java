/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal;

import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.module.extension.file.api.FileAttributes;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;

import java.nio.file.Path;

/**
 * A delegate object for copying files
 *
 * @since 4.0
 */
@FunctionalInterface
public interface FtpCopyDelegate {

  /**
   * Performs the copy operation
   *
   * @param config the config which is parameterizing this operation
   * @param source the attributes which describes the source file
   * @param targetPath the target path
   * @param overwrite whether to overwrite the target file if it already exists
   * @param event the {@link MuleEvent} on which context the operation is being executed
   */
  void doCopy(FileConnectorConfig config, FileAttributes source, Path targetPath, boolean overwrite, MuleEvent event);
}
