/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.ftp.command;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.fail;

import org.mule.extension.file.common.api.FileSystem;
import org.mule.runtime.core.util.FileUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.Test;

@SmallTest
public class FtpReconnectionTestCase extends AbstractMuleTestCase {

  private FtpDummyCommand command = new FtpDummyCommand();

  @Test
  public void testReconnectionFromFTPConnectionClosed() {
    try {
      command.getFile(null);
      fail("Should have handled an FTPConnectionClosedException");
    } catch (Exception e) {
      assertTrue(e.getCause().getCause() instanceof FTPConnectionClosedException);
    }
  }

  private class FtpDummyCommand extends ClassicFtpCommand {

    public FtpDummyCommand() {
      super(null, new FTPClient() {

        @Override
        public FTPFile mlistFile(String pathname) throws IOException {
          throw new FTPConnectionClosedException();
        }

        @Override
        public int getReplyCode() {
          return -1;
        }
      });
    }

    @Override
    protected Path getBasePath(FileSystem fileSystem) {
      return FileUtils.getTempDirectory().toPath();
    }
  }
}
