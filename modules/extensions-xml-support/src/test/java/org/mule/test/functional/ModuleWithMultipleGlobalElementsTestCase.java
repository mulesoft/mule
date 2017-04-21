/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.runtime.api.message.Message;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.List;

public class ModuleWithMultipleGlobalElementsTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

  private static final String SUB_DIRECTORY_NAME_A = "subDirectoryA";
  private static final String SUB_DIRECTORY_NAME_B = "subDirectoryB";

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public SystemProperty workingDir = new SystemProperty("workingDir", temporaryFolder.getRoot().getAbsolutePath());

  @BeforeClass
  public static void setUp() throws Exception {
    if (!temporaryFolder.getRoot().exists()) {
      temporaryFolder.getRoot().mkdir();
    }
    temporaryFolder.newFolder(SUB_DIRECTORY_NAME_A);
    temporaryFolder.newFolder(SUB_DIRECTORY_NAME_B);
  }

  @AfterClass
  public static void tearDown() {
    temporaryFolder.delete();
  }

  @Override
  protected String getModulePath() {
    return "modules/module-multiple-global-elements.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-multiple-global-elements.xml";
  }

  @Test
  public void listPatternA() throws Exception {
    assertFlowResult("list-pattern-a", SUB_DIRECTORY_NAME_A);
  }

  @Test
  public void listPatternB() throws Exception {
    assertFlowResult("list-pattern-b", SUB_DIRECTORY_NAME_B);
  }

  private void assertFlowResult(String flowName, String subDirectoryName) throws Exception {
    List<Message> messages = (List<Message>) flowRunner(flowName).run().getMessage().getPayload().getValue();
    assertThat(messages, is(notNullValue()));
    assertThat(messages, hasSize(1));
    FileAttributes attributes = (FileAttributes) messages.get(0).getAttributes().getValue();
    assertThat(attributes.getName(), is(subDirectoryName));
  }
}
