/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.extension.FtpTestHarness;
import org.mule.extension.file.common.api.TreeNode;

import org.junit.Test;

public class FtpDefaultWorkingDirTestCase extends FtpConnectorTestCase {

  public FtpDefaultWorkingDirTestCase(String name, FtpTestHarness testHarness) {
    super(name, testHarness);
  }

  @Override
  protected String getConfigFile() {
    return "ftp-list-config.xml";
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    System.clearProperty(WORKING_DIRECTORY_SYSTEM_PROPERTY_KEY);
    super.doSetUpBeforeMuleContextCreation();
  }

  @Test
  public void list() throws Exception {
    TreeNode node = (TreeNode) flowRunner("list")
        .withVariable("path", "../")
        .withVariable("recursive", false)
        .run()
        .getMessage().getPayload().getValue();

    assertThat(node.getChilds().isEmpty(), is(false));
  }

}
