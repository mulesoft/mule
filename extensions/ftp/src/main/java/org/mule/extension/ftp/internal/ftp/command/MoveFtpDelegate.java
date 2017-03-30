/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.ftp.command;

import static java.lang.String.format;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.ftp.internal.FtpCopyDelegate;
import org.mule.extension.ftp.internal.ftp.connection.FtpFileSystem;
import org.mule.runtime.extension.api.exception.ModuleException;

import java.nio.file.Path;

public class MoveFtpDelegate implements FtpCopyDelegate {

  private FtpCommand command;
  private FtpFileSystem fileSystem;

  public MoveFtpDelegate(FtpCommand command, FtpFileSystem fileSystem) {
    this.command = command;
    this.fileSystem = fileSystem;
  }

  @Override
  public void doCopy(FileConnectorConfig config, FileAttributes source, Path targetPath, boolean overwrite) {
    try {
      if (command.exists(targetPath)) {
        if (overwrite) {
          fileSystem.delete(targetPath.toString());
        } else {
          command.alreadyExistsException(targetPath);
        }
      }

      command.rename(source.getPath(), targetPath.toString(), overwrite);
    } catch (ModuleException e) {
      throw e;
    } catch (Exception e) {
      throw command.exception(format("Found exception copying file '%s' to '%s'", source, targetPath), e);
    }
  }
}
