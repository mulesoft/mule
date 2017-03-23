/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.mule.extension.FtpTestHarness.HELLO_PATH;

import org.mule.extension.FtpTestHarness;
import org.mule.extension.file.common.api.FileWriteMode;
import org.mule.extension.file.common.api.stream.AbstractFileInputStream;
import org.mule.extension.ftp.internal.sftp.connection.SftpClient;
import org.mule.extension.ftp.internal.sftp.connection.SftpClientFactory;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.util.IOUtils;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;
import org.mule.test.runner.RunnerDelegateTo;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunnerDelegateTo(Parameterized.class)
@ArtifactClassLoaderRunnerConfig(exportPluginClasses = {SftpClientFactory.class, SftpClient.class})
public abstract class FtpConnectorTestCase extends MuleArtifactFunctionalTestCase {

  protected static final String NAMESPACE = "FTP";
  private final String name;

  @Rule
  public final FtpTestHarness testHarness;

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{"ftp", new ClassicFtpTestHarness()}, {"sftp", new SftpTestHarness()}});
  }

  public FtpConnectorTestCase(String name, FtpTestHarness testHarness) {
    this.name = name;
    this.testHarness = testHarness;
  }

  protected Event readHelloWorld() throws Exception {
    return getPath(HELLO_PATH);
  }

  protected Message readPath(String path) throws Exception {
    return readPath(path, true);
  }

  protected Message readPath(String path, boolean streaming) throws Exception {
    return getPath(path, streaming).getMessage();
  }

  protected void doWrite(String path, Object content, FileWriteMode mode, boolean createParent) throws Exception {
    doWrite("write", path, content, mode, createParent);
  }

  protected void doWrite(String flow, String path, Object content, FileWriteMode mode, boolean createParent) throws Exception {
    doWrite(flow, path, content, mode, createParent, null);
  }

  protected void doWrite(String flow, String path, Object content, FileWriteMode mode, boolean createParent, String encoding)
      throws Exception {
    flowRunner(flow).withVariable("path", path).withVariable("createParent", createParent).withVariable("mode", mode)
        .withVariable("encoding", encoding).withPayload(content).run();
  }

  private Event getPath(String path) throws Exception {
    return getPath(path, true);
  }

  private Event getPath(String path, boolean streaming) throws Exception {
    return flowRunner("read")
        .withVariable("path", path)
        .withVariable("streaming", streaming)
        .run();
  }

  protected String readPathAsString(String path) throws Exception {
    return toString(readPath(path).getPayload().getValue());
  }

  protected boolean isLocked(Message message) {
    return ((AbstractFileInputStream) message.getPayload().getValue()).isLocked();
  }

  protected String toString(Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof Message) {
      value = ((Message) value).getPayload().getValue();
    }

    if (value instanceof String) {
      return (String) value;
    }

    InputStream inputStream;
    if (value instanceof CursorStreamProvider) {
      inputStream = ((CursorStreamProvider) value).openCursor();
    } else if (value instanceof InputStream) {
      inputStream = (InputStream) value;
    } else {
      throw new IllegalArgumentException("Result was not of expected type");
    }

    try {
      return IOUtils.toString(inputStream);
    } finally {
      closeQuietly(inputStream);
    }
  }

}
