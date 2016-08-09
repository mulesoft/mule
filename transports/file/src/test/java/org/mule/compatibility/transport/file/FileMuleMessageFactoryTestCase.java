/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.compatibility.core.api.transport.MuleMessageFactory;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleMessage;

import java.io.File;

import org.junit.Test;

public class FileMuleMessageFactoryTestCase extends AbstractFileMuleMessageFactoryTestCase {

  @Override
  protected MuleMessageFactory doCreateMuleMessageFactory() {
    return new FileMuleMessageFactory();
  }

  @Override
  protected Object getValidTransportMessage() {
    return tempFile;
  }

  @Test
  public void testMessageProperties() throws Exception {
    MuleMessageFactory factory = createMuleMessageFactory();

    MuleMessage message = factory.create(getValidTransportMessage(), encoding);
    assertNotNull(message);
    assertMessageProperties(message);
  }

  @Test
  public void testCreateMessageFromStream() throws Exception {
    MuleMessageFactory factory = createMuleMessageFactory();

    ReceiverFileInputStream stream = new ReceiverFileInputStream(tempFile, false, null);
    MuleMessage message = factory.create(stream, encoding);
    assertNotNull(message);
    assertMessageProperties(message);
  }

  @Test
  public void testCloseSeveralTimes() throws Exception {
    MuleMessageFactory factory = createMuleMessageFactory();
    File moveTo = tempFolder.newFile("moveTo.tmp");
    moveTo.deleteOnExit();
    ReceiverFileInputStream mockStream = new ReceiverFileInputStream(tempFile, false, moveTo);
    MuleMessage message = factory.create(mockStream, encoding);
    assertNotNull(message);

    assertTrue(tempFile.exists());

    mockStream.close();
    assertTrue(!tempFile.exists());

    mockStream.close(); // Ensure second close works ok
  }

  @Test
  public void setsMimeType() throws Exception {
    MuleMessageFactory factory = createMuleMessageFactory();
    File file = tempFolder.newFile("test.txt");
    file.deleteOnExit();
    ReceiverFileInputStream mockStream = new ReceiverFileInputStream(file, false, null);

    MuleMessage message = factory.create(mockStream, encoding);
    assertThat(message.getDataType().getMediaType().getPrimaryType(), equalTo(MediaType.TEXT.getPrimaryType()));
    assertThat(message.getDataType().getMediaType().getSubType(), equalTo(MediaType.TEXT.getSubType()));
  }

  private void assertMessageProperties(MuleMessage message) {
    assertEquals(tempFile.getName(), message.getInboundProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME));
    assertEquals(tempFile.getParent(), message.getInboundProperty(FileConnector.PROPERTY_DIRECTORY));
    assertThat(message.getInboundProperty(FileConnector.PROPERTY_FILE_SIZE), is(0l));
  }
}


