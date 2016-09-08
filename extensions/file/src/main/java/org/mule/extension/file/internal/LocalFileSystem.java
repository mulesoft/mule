/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal;

import static java.nio.file.StandardOpenOption.WRITE;
import org.mule.extension.file.api.LocalFileAttributes;
import org.mule.extension.file.internal.command.LocalCopyCommand;
import org.mule.extension.file.internal.command.LocalCreateDirectoryCommand;
import org.mule.extension.file.internal.command.LocalDeleteCommand;
import org.mule.extension.file.internal.command.LocalListCommand;
import org.mule.extension.file.internal.command.LocalMoveCommand;
import org.mule.extension.file.internal.command.LocalReadCommand;
import org.mule.extension.file.internal.command.LocalRenameCommand;
import org.mule.extension.file.internal.command.LocalWriteCommand;
import org.mule.extension.file.internal.lock.LocalPathLock;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.util.ArrayUtils;
import org.mule.extension.file.common.api.AbstractFileSystem;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.FileSystem;
import org.mule.extension.file.common.api.command.CopyCommand;
import org.mule.extension.file.common.api.command.CreateDirectoryCommand;
import org.mule.extension.file.common.api.command.DeleteCommand;
import org.mule.extension.file.common.api.command.ListCommand;
import org.mule.extension.file.common.api.command.MoveCommand;
import org.mule.extension.file.common.api.command.ReadCommand;
import org.mule.extension.file.common.api.command.RenameCommand;
import org.mule.extension.file.common.api.command.WriteCommand;
import org.mule.extension.file.common.api.lock.PathLock;

import java.nio.file.OpenOption;
import java.nio.file.Path;

/**
 * Implementation of {@link FileSystem} for file systems mounted on the host operating system.
 * <p>
 * Whenever the {@link FileSystem} contract refers to locking, this implementation will resolve through a {@link LocalPathLock},
 * which produces file system level locks which rely on the host operating system.
 * <p>
 * Also, for any method returning {@link FileAttributes} instances, a {@link LocalFileAttributes} will be used.
 *
 * @since 4.0
 */
public final class LocalFileSystem extends AbstractFileSystem {

  private final CopyCommand copyCommand;
  private final CreateDirectoryCommand createDirectoryCommand;
  private final DeleteCommand deleteCommand;
  private final ListCommand listCommand;
  private final MoveCommand moveCommand;
  private final ReadCommand readCommand;
  private final RenameCommand renameCommand;
  private final WriteCommand writeCommand;

  /**
   * Creates a new instance
   */
  public LocalFileSystem(MuleContext muleContext) {
    copyCommand = new LocalCopyCommand(this);
    createDirectoryCommand = new LocalCreateDirectoryCommand(this);
    deleteCommand = new LocalDeleteCommand(this);
    listCommand = new LocalListCommand(this);
    moveCommand = new LocalMoveCommand(this);
    readCommand = new LocalReadCommand(this);
    renameCommand = new LocalRenameCommand(this);
    writeCommand = new LocalWriteCommand(this, muleContext);
  }

  @Override
  protected CopyCommand getCopyCommand() {
    return copyCommand;
  }

  @Override
  public CreateDirectoryCommand getCreateDirectoryCommand() {
    return createDirectoryCommand;
  }

  @Override
  protected DeleteCommand getDeleteCommand() {
    return deleteCommand;
  }

  @Override
  protected ListCommand getListCommand() {
    return listCommand;
  }

  @Override
  protected MoveCommand getMoveCommand() {
    return moveCommand;
  }

  @Override
  protected ReadCommand getReadCommand() {
    return readCommand;
  }

  @Override
  protected RenameCommand getRenameCommand() {
    return renameCommand;
  }

  @Override
  protected WriteCommand getWriteCommand() {
    return writeCommand;
  }

  @Override
  protected PathLock createLock(Path path, Object... params) {
    return new LocalPathLock(path, ArrayUtils.isEmpty(params) ? new OpenOption[] {WRITE} : (OpenOption[]) params);
  }

  /**
   * No-op implementation.
   */
  @Override
  public void changeToBaseDir(FileConnectorConfig config) {}

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends FileAttributes> getAttributesType() {
    return LocalFileAttributes.class;
  }

}
