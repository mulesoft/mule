/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.extension.file.common.api.exceptions.FileError.ILLEGAL_PATH;
import org.mule.extension.FtpTestHarness;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.exceptions.IllegalPathException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.util.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;

public class FtpListTestCase extends FtpConnectorTestCase {

  private static final String TEST_FILE_PATTERN = "test-file-%d.html";
  private static final String SUB_DIRECTORY_NAME = "subDirectory";
  private static final String CONTENT = "foo";

  public FtpListTestCase(String name, FtpTestHarness testHarness) {
    super(name, testHarness);
  }

  @Override
  protected String getConfigFile() {
    return "ftp-list-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    createTestFiles();
  }

  @Test
  public void listNotRecursive() throws Exception {
    List<Message> messages = doList(".", false);

    assertThat(messages, hasSize(6));
    assertThat(assertListedFiles(messages), is(true));
  }

  @Test
  public void listRecursive() throws Exception {
    List<Message> messages = doList(".", true);

    assertThat(messages, hasSize(8));
    assertThat(assertListedFiles(messages), is(true));

    List<Message> subDirectories = messages.stream()
        .filter(message -> ((FileAttributes) message.getAttributes()).isDirectory())
        .collect(toList());

    assertThat(subDirectories, hasSize(1));
    assertThat(assertListedFiles(subDirectories), is(true));
  }

  @Test
  public void notDirectory() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, ILLEGAL_PATH.getType(), IllegalPathException.class,
                                            "Only directories can be listed");
    doList("list", format(TEST_FILE_PATTERN, 0), false);
  }

  @Test
  public void notExistingPath() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, ILLEGAL_PATH.getType(), IllegalPathException.class, "doesn't exists");
    doList("list", format("whatever", 0), false);
  }

  @Test
  public void listWithEmbeddedMatcher() throws Exception {
    List<Message> messages = doList("listWithEmbeddedPredicate", ".", false);

    assertThat(messages, hasSize(2));
    assertThat(assertListedFiles(messages), is(false));
  }

  @Test
  public void listWithGlobalMatcher() throws Exception {
    List<Message> messages = doList("listWithGlobalMatcher", ".", true);

    assertThat(messages, hasSize(1));

    FileAttributes file = (FileAttributes) messages.get(0).getAttributes();
    assertThat(file.isDirectory(), is(true));
    assertThat(file.getName(), equalTo(SUB_DIRECTORY_NAME));
  }

  @Test
  public void listWithoutPath() throws Exception {
    List<Message> messages = (List<Message>) flowRunner("listWithoutPath").run().getMessage().getPayload().getValue();

    assertThat(messages, hasSize(6));
    FileAttributes attributes = (FileAttributes) messages.get(0).getAttributes();
    assertThat(new File(attributes.getPath()).getParent(), is(equalTo(testHarness.getWorkingDirectory())));
  }

  private boolean assertListedFiles(List<Message> messages) throws Exception {
    boolean directoryWasFound = false;

    for (Message message : messages) {
      FileAttributes attributes = (FileAttributes) message.getAttributes();
      if (attributes.isDirectory()) {
        assertThat("two directories found", directoryWasFound, is(false));
        directoryWasFound = true;
        assertThat(attributes.getName(), equalTo(SUB_DIRECTORY_NAME));
      } else {
        assertThat(attributes.getName(), endsWith(".html"));
        assertThat(IOUtils.toString((InputStream) message.getPayload().getValue()), equalTo(CONTENT));
        assertThat(attributes.getSize(), is(new Long(CONTENT.length())));
      }
    }

    return directoryWasFound;
  }

  private List<Message> doList(String path, boolean recursive) throws Exception {
    return doList("list", path, recursive);
  }

  private List<Message> doList(String flowName, String path, boolean recursive) throws Exception {
    List<Message> messages =
        (List<Message>) flowRunner(flowName).withVariable("path", path).withVariable("recursive", recursive).run()
            .getMessage().getPayload().getValue();

    assertThat(messages, is(notNullValue()));
    return messages;
  }

  private void createTestFiles() throws Exception {
    createTestFiles(".", 0, 5);
    createSubDirectory();
  }

  private void createSubDirectory() throws Exception {
    testHarness.makeDir(SUB_DIRECTORY_NAME);
    createTestFiles(SUB_DIRECTORY_NAME, 5, 7);
  }

  private void createTestFiles(String parentFolder, int startIndex, int endIndex) throws Exception {
    for (int i = startIndex; i < endIndex; i++) {
      String name = String.format(TEST_FILE_PATTERN, i);
      testHarness.write(parentFolder, name, CONTENT);
    }
  }
}
