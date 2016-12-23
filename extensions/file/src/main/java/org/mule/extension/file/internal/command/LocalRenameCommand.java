/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal.command;

import static java.lang.String.format;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import org.mule.extension.file.common.api.command.RenameCommand;
import org.mule.extension.file.common.api.exceptions.FileAlreadyExistsException;
import org.mule.extension.file.internal.LocalFileSystem;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A {@link LocalFileCommand} which implements the {@link RenameCommand} contract
 *
 * @since 4.0
 */
public final class LocalRenameCommand extends LocalFileCommand implements RenameCommand {

  /**
   * {@inheritDoc}
   */
  public LocalRenameCommand(LocalFileSystem fileSystem) {
    super(fileSystem);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void rename(String filePath, String newName, boolean overwrite) {
    Path source = resolveExistingPath(filePath);
    Path target = source.getParent().resolve(newName);

    if (Files.exists(target)) {
      if (!overwrite) {
        throw new FileAlreadyExistsException(format("'%s' cannot be renamed because '%s' already exists", source, target));
      }

      try {
        fileSystem.delete(target.toString());
      } catch (Exception e) {
        throw exception(format("Exception was found deleting '%s' as part of renaming '%s'", target, source), e);
      }
    }

    try {
      Files.move(source, target, ATOMIC_MOVE, REPLACE_EXISTING);
    } catch (Exception e) {
      throw exception(format("Exception was found renaming '%s' to '%s'", source, newName), e);
    }
  }
}
