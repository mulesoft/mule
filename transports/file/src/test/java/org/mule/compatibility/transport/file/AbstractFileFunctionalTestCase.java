/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.compatibility.core.api.config.MuleEndpointProperties;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.transport.file.FileConnector;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.core.util.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.net.MalformedURLException;

/**
 * We are careful here to access the file system in a generic way. This means setting directories dynamically.
 */
public abstract class AbstractFileFunctionalTestCase extends FunctionalTestCase {

  public static final String TEST_MESSAGE = "Test file contents";
  public static final String TARGET_FILE = "TARGET_FILE";

  protected File tmpDir;

  @Override
  protected String getConfigFile() {
    return "file-functional-test-flow.xml";
  }

  protected String fileToUrl(File file) throws MalformedURLException {
    return file.getAbsoluteFile().toURI().toURL().toString();
  }

  // annoying but necessary wait apparently due to OS caching?
  protected void waitForFileSystem() throws Exception {
    synchronized (this) {
      wait(1000);
    }
  }

  protected File initForRequest() throws Exception {
    createTempDirectory();
    File target = createAndPopulateTempFile("mule-file-test-", ".txt");

    // define the readFromDirectory on the connector
    FileConnector connector = (FileConnector) muleContext.getRegistry().lookupObject("receiveConnector");
    connector.setReadFromDirectory(tmpDir.getAbsolutePath());
    logger.debug("Directory is " + connector.getReadFromDirectory());

    waitForFileSystem();
    return target;
  }

  private void createTempDirectory() throws Exception {
    tmpDir = File.createTempFile("mule-file-test-", "-dir");
    tmpDir.delete();
    tmpDir.mkdir();
  }

  protected File createAndPopulateTempFile(String prefix, String suffix) throws Exception {
    File target = File.createTempFile(prefix, suffix, tmpDir);
    logger.info("Created temporary file: " + target.getAbsolutePath());

    Writer out = new FileWriter(target);
    out.write(TEST_MESSAGE);
    out.close();

    target.deleteOnExit();
    return target;
  }

  protected void checkReceivedMessage(MuleMessage message) throws Exception {
    assertNotNull(message);
    assertNotNull(message.getPayload());
    assertTrue(message.getPayload() instanceof InputStream);

    InputStream fis = (InputStream) message.getPayload();
    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    IOUtils.copy(fis, byteOut);
    fis.close();
    String result = new String(byteOut.toByteArray());
    assertEquals(TEST_MESSAGE, result);
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();
    FileUtils.deleteTree(tmpDir);
  }

  public EndpointFactory getEndpointFactory() {
    return (EndpointFactory) muleContext.getRegistry().lookupObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY);
  }
}
