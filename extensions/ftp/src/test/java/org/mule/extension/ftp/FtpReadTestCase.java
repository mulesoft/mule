/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.FtpTestHarness.BINARY_FILE_NAME;
import static org.mule.extension.FtpTestHarness.HELLO_PATH;
import static org.mule.extension.FtpTestHarness.HELLO_WORLD;
import static org.mule.extension.file.common.api.exceptions.FileError.ILLEGAL_PATH;
import static org.mule.runtime.api.metadata.MediaType.JSON;
import org.mule.extension.FtpTestHarness;
import org.mule.extension.file.common.api.exceptions.IllegalPathException;
import org.mule.extension.file.common.api.stream.AbstractFileInputStream;
import org.mule.extension.ftp.api.FtpFileAttributes;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.util.IOUtils;

import java.io.InputStream;

import org.junit.Test;

public class FtpReadTestCase extends FtpConnectorTestCase {

  public FtpReadTestCase(String name, FtpTestHarness testHarness) {
    super(name, testHarness);
  }

  @Override
  protected String getConfigFile() {
    return "ftp-read-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    testHarness.createHelloWorldFile();
  }

  @Test
  public void read() throws Exception {
    Message message = readHelloWorld().getMessage();

    assertThat(message.getPayload().getDataType().getMediaType().getPrimaryType(), is(JSON.getPrimaryType()));
    assertThat(message.getPayload().getDataType().getMediaType().getSubType(), is(JSON.getSubType()));

    AbstractFileInputStream payload = (AbstractFileInputStream) message.getPayload().getValue();
    assertThat(payload.isLocked(), is(false));
    assertThat(getPayloadAsString(message), is(HELLO_WORLD));
  }

  @Test
  public void readBinary() throws Exception {
    testHarness.createBinaryFile();

    Message response = readPath(BINARY_FILE_NAME);

    assertThat(response.getPayload().getDataType().getMediaType().getPrimaryType(), is(MediaType.BINARY.getPrimaryType()));
    assertThat(response.getPayload().getDataType().getMediaType().getSubType(), is(MediaType.BINARY.getSubType()));

    AbstractFileInputStream payload = (AbstractFileInputStream) response.getPayload().getValue();
    assertThat(payload.isLocked(), is(false));

    byte[] readContent = new byte[new Long(HELLO_WORLD.length()).intValue()];
    IOUtils.read(payload, readContent);
    assertThat(new String(readContent), is(HELLO_WORLD));
  }

  @Test
  public void readWithForcedMimeType() throws Exception {
    Event event = flowRunner("readWithForcedMimeType").withVariable("path", HELLO_PATH).run();
    assertThat(event.getMessage().getPayload().getDataType().getMediaType().getPrimaryType(), equalTo("test"));
    assertThat(event.getMessage().getPayload().getDataType().getMediaType().getSubType(), equalTo("test"));
  }

  @Test
  public void readUnexisting() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, ILLEGAL_PATH.getType(), IllegalPathException.class, "doesn't exists");
    readPath("files/not-there.txt");
  }

  @Test
  public void readDirectory() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, ILLEGAL_PATH.getType(), IllegalPathException.class,
                                            "since it's a directory");
    readPath("files");
  }

  @Test
  public void readLockReleasedOnContentConsumed() throws Exception {
    Message message = readWithLock();
    getPayloadAsString(message);

    assertThat(isLocked(message), is(false));
  }

  @Test
  public void readLockReleasedOnEarlyClose() throws Exception {
    Message message = readWithLock();
    ((InputStream) message.getPayload().getValue()).close();

    assertThat(isLocked(message), is(false));
  }

  @Test
  public void getProperties() throws Exception {
    FtpFileAttributes fileAttributes = (FtpFileAttributes) readHelloWorld().getMessage().getAttributes();
    testHarness.assertAttributes(HELLO_PATH, fileAttributes);
  }

  private Message readWithLock() throws Exception {
    Message message = flowRunner("readWithLock").run().getMessage();

    assertThat(isLocked(message), is(true));
    return message;
  }
}
