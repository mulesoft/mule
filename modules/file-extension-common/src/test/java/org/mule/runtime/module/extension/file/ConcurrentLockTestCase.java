/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.file;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.runtime.module.extension.file.api.AbstractFileSystem;
import org.mule.runtime.module.extension.file.api.FileAttributes;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;
import org.mule.runtime.module.extension.file.api.command.CopyCommand;
import org.mule.runtime.module.extension.file.api.command.CreateDirectoryCommand;
import org.mule.runtime.module.extension.file.api.command.DeleteCommand;
import org.mule.runtime.module.extension.file.api.command.ListCommand;
import org.mule.runtime.module.extension.file.api.command.MoveCommand;
import org.mule.runtime.module.extension.file.api.command.ReadCommand;
import org.mule.runtime.module.extension.file.api.command.RenameCommand;
import org.mule.runtime.module.extension.file.api.command.WriteCommand;
import org.mule.runtime.module.extension.file.api.lock.NullPathLock;
import org.mule.runtime.module.extension.file.api.lock.PathLock;
import org.mule.tck.size.SmallTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ConcurrentLockTestCase {

  private static final Path PATH = Paths.get("lock");
  private static final int TIMEOUT = 5;
  private static final TimeUnit TIMEOUT_UNIT = SECONDS;

  private AbstractFileSystem fileSystem = new TestFileSystem();
  private Latch mainThreadLatch = new Latch();
  private Latch secondaryThreadLatch = new Latch();
  private CountDownLatch assertionLatch = new CountDownLatch(2);
  private AtomicInteger failed = new AtomicInteger(0);
  private AtomicInteger successful = new AtomicInteger(0);

  @Test
  public void concurrentLock() throws Exception {
    new Thread(() -> {
      try {
        mainThreadLatch.release();
        secondaryThreadLatch.await(TIMEOUT, TIMEOUT_UNIT);
        tryLock();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }).start();

    mainThreadLatch.await(TIMEOUT, TIMEOUT_UNIT);
    secondaryThreadLatch.release();
    tryLock();

    assertionLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertThat(successful.get(), is(1));
    assertThat(failed.get(), is(1));
  }

  private void tryLock() {
    try {
      if (fileSystem.lock(PATH).tryLock()) {
        successful.incrementAndGet();
      } else {
        failed.incrementAndGet();
      }
    } catch (Exception e) {
      failed.incrementAndGet();
    }
    assertionLatch.countDown();
  }


  private class TestFileSystem extends AbstractFileSystem {

    private boolean locked = false;

    @Override
    protected ListCommand getListCommand() {
      return null;
    }

    @Override
    protected ReadCommand getReadCommand() {
      return null;
    }

    @Override
    protected WriteCommand getWriteCommand() {
      return null;
    }

    @Override
    protected CopyCommand getCopyCommand() {
      return null;
    }

    @Override
    protected MoveCommand getMoveCommand() {
      return null;
    }

    @Override
    protected DeleteCommand getDeleteCommand() {
      return null;
    }

    @Override
    protected RenameCommand getRenameCommand() {
      return null;
    }

    @Override
    protected CreateDirectoryCommand getCreateDirectoryCommand() {
      return null;
    }

    @Override
    protected PathLock createLock(Path path, Object... params) {
      if (locked) {
        PathLock lock = mock(PathLock.class);
        when(lock.tryLock()).thenReturn(false);
        return lock;
      } else {
        locked = true;
        return new NullPathLock();
      }
    }

    @Override
    public void changeToBaseDir(FileConnectorConfig config) {}

    @Override
    public Class<? extends FileAttributes> getAttributesType() {
      return FileAttributes.class;
    }
  }
}
