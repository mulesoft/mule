/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static java.util.Arrays.asList;
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
import org.junit.runners.Parameterized;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;
import java.util.List;

@RunnerDelegateTo(Parameterized.class)
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

  @Parameterized.Parameter
  public String configFile;

  @Parameterized.Parameter(1)
  public String[] paths;

  @Parameterized.Parameters(name = "{index}: Running tests for {0} ")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        // scenario without TNS
        {"flows/flows-using-module-multiple-global-elements.xml", new String[] {"modules/module-multiple-global-elements.xml"}},
        // scenario with TNS and "internal" operations
        {"flows/flows-using-module-calling-operations-within-module-with-global-elements.xml",
            new String[] {"modules/module-calling-operations-within-module-with-global-elements.xml"}}
    });
  }

  @Override
  protected String[] getModulePaths() {
    return paths;
  }

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  @Test
  public void testConnectionConfigPatternA() throws Exception {
    doTestConnection("configPatternA");
  }

  @Test
  public void testConnectionConfigPatternB() throws Exception {
    doTestConnection("configPatternB");
  }

  private void doTestConnection(String globalElement) throws MuleException {
    ConfigurationInstance config = muleContext.getExtensionManager().getConfiguration(globalElement, testEvent());
    assertThat(config, is(notNullValue()));
    assertThat(config.getConnectionProvider().isPresent(), is(true));
    final ConnectionProvider connectionProvider = config.getConnectionProvider().get();
    final Object connect = connectionProvider.connect();
    final ConnectionValidationResult connectionValidationResult = connectionProvider.validate(connect);
    assertThat(connectionValidationResult.isValid(), is(true));
    connectionProvider.disconnect(connect);
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
