/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import org.mule.extension.FtpTestHarness;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.extension.file.api.FileAttributes;
import org.mule.runtime.module.extension.file.api.TreeNode;

import java.nio.file.Paths;
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
    TreeNode node = doList(".", false);
    List<TreeNode> childs = node.getChilds();

    assertThat(childs, hasSize(6));
    assertThat(assertListedFiles(childs), is(true));
  }

  @Test
  public void listRecursive() throws Exception {
    TreeNode node = doList(".", true);
    List<TreeNode> childs = node.getChilds();

    assertThat(childs, hasSize(6));
    assertThat(assertListedFiles(childs), is(true));

    List<TreeNode> subDirectories = childs.stream().filter(child -> child.getAttributes().isDirectory()).collect(toList());

    assertThat(subDirectories, hasSize(1));
    TreeNode subDirectory = subDirectories.get(0);
    assertThat(subDirectory.getChilds(), hasSize(2));
    assertThat(assertListedFiles(subDirectory.getChilds()), is(false));
  }

  @Test
  public void notDirectory() throws Exception {
    testHarness.expectedException().expectCause(is(instanceOf(IllegalArgumentException.class)));
    doList(String.format(TEST_FILE_PATTERN, 0), false);
  }

  @Test
  public void notExistingPath() throws Exception {
    testHarness.expectedException().expectCause(is(instanceOf(IllegalArgumentException.class)));
    doList(String.format("whatever", 0), false);
  }

  @Test
  public void listWithEmbeddedMatcher() throws Exception {
    TreeNode node = doList("listWithEmbeddedPredicate", ".", false);
    List<TreeNode> childs = node.getChilds();

    assertThat(childs, hasSize(2));
    assertThat(assertListedFiles(childs), is(false));
  }

  @Test
  public void listWithGlobalMatcher() throws Exception {
    TreeNode node = doList("listWithGlobalMatcher", ".", true);
    List<TreeNode> childs = node.getChilds();

    assertThat(childs, hasSize(1));

    FileAttributes file = childs.get(0).getAttributes();
    assertThat(file.isDirectory(), is(true));
    assertThat(file.getName(), equalTo(SUB_DIRECTORY_NAME));
  }

  @Test
  public void listWithoutPath() throws Exception {
    TreeNode node = (TreeNode) flowRunner("listWithoutPath").run().getMessage().getPayload();

    assertThat(node.getAttributes().getPath(),
               is(equalTo(Paths.get(testHarness.getWorkingDirectory()).toAbsolutePath().toString())));
    assertThat(node.getChilds(), hasSize(6));
  }

  private boolean assertListedFiles(List<TreeNode> nodes) throws Exception {
    boolean directoryWasFound = false;

    for (TreeNode node : nodes) {
      FileAttributes attributes = node.getAttributes();
      if (attributes.isDirectory()) {
        assertThat("two directories found", directoryWasFound, is(false));
        directoryWasFound = true;
        assertThat(attributes.getName(), equalTo(SUB_DIRECTORY_NAME));
      } else {
        assertThat(attributes.getName(), endsWith(".html"));
        assertThat(IOUtils.toString(node.getContent()), equalTo(CONTENT));
        assertThat(attributes.getSize(), is(new Long(CONTENT.length())));
      }
    }

    return directoryWasFound;
  }

  private TreeNode doList(String path, boolean recursive) throws Exception {
    return doList("list", path, recursive);
  }

  private TreeNode doList(String flowName, String path, boolean recursive) throws Exception {
    TreeNode node = (TreeNode) flowRunner(flowName).withFlowVariable("path", path).withFlowVariable("recursive", recursive).run()
        .getMessage().getPayload();

    assertThat(node, is(notNullValue()));
    assertThat(node.getContent(), is(nullValue()));

    FileAttributes attributes = node.getAttributes();
    assertThat(attributes.isDirectory(), is(true));
    assertThat(attributes.getPath(), equalTo(Paths.get(testHarness.getWorkingDirectory()).resolve(path).toString()));
    assertThat(attributes.isDirectory(), is(true));

    return node;
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
