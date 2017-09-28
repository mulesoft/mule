/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModuleModel;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Arrays;
import java.util.Collection;

/**
 * Test case to guarantee that the {@link MacroExpansionModuleModel} does its work properly. The Mule application relies on a set
 * of modules that will be feed in several orders, proving that the topological order while macro expanding do work accordingly.
 * <p/>
 * The current application graph dependencies with plugins to macro expand is as follow:
 * <pre>
 *   +---------------------------------------------+
 *   |                                             |
 *   |                                           +-v-+
 *   | +-----------------------+ +---------------> Z |
 *   | |                       | |               +---+
 *   | |                      +v-++     +---+
 *   | |               +------> B +-----> A |
 *   | |               |      +---+     +---+
 *   | |             +-+-+
 *   | | +-----------> C +----------------+
 *   | | |           +---+                |
 *   +-+-+-+                              |
 *   |     |                   +---+    +-v-+
 *   | APP +-------------------> X +----> W <--+
 *   |     |                   +-^-+    +--++  |
 *   +---+-+                     |         |   |
 *       |           +---+       |         +---+
 *       +-----------> Y +-------+
 *                   +---+
 * </pre>
 */
@RunnerDelegateTo(Parameterized.class)
public class ModulesUsingGraphTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

  private static final String MODULES_GRAPH_FOLDER = "modules/graph/module-";
  private static final String SUFFIX_XML_FILE = ".xml";

  @Parameterized.Parameter
  public String permutation;

  @Parameterized.Parameter(1)
  public String[] paths;

  @Parameterized.Parameters(name = "{index}: Running permutation modules {0} ")
  public static Collection<Object[]> data() {
    // some permutations of the presented graph, it's important to make them shuffle enough to be sure the namespaces are order
    // correctly.
    return asList(getParameters("w", "x", "y", "z", "a", "b", "c"),
                  getParameters("a", "b", "c", "w", "x", "y", "z"),
                  getParameters("y", "z", "c", "b", "a", "w", "x"),
                  getParameters("w", "z", "a", "b", "x", "c", "y"),
                  getParameters("b", "w", "x", "z", "c", "a", "y"));
  }

  private static Object[] getParameters(String... modules) {
    return new Object[] {StringUtils.join(modules, "-"),
        Arrays.asList(modules).stream()
            .map(moduleName -> MODULES_GRAPH_FOLDER + moduleName + SUFFIX_XML_FILE)
            .toArray(String[]::new)
    };
  }

  @Override
  protected String[] getModulePaths() {
    return paths;
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-graph-modules.xml";
  }

  @Test
  public void testUsingModuleB_Op1() throws Exception {
    assertCalls("testUsingModuleB_Op1", "b-op1 a-op1 z-op1");
  }

  @Test
  public void testUsingModuleC_Op1() throws Exception {
    assertCalls("testUsingModuleC_Op1", "c-op1 b-op1 a-op1 z-op1");
  }

  @Test
  public void testUsingModuleC_Op2() throws Exception {
    assertCalls("testUsingModuleC_Op2", "c-op2 a-op1 z-op1");
  }

  @Test
  public void testUsingModuleC_Op3() throws Exception {
    assertCalls("testUsingModuleC_Op3", "c-op3 w-op1 w-internal-op");
  }

  @Test
  public void testUsingModuleX_Op1() throws Exception {
    assertCalls("testUsingModuleX_Op1", "x-op1 w-op1 w-internal-op");
  }

  @Test
  public void testUsingModuleY_Op1() throws Exception {
    assertCalls("testUsingModuleY_Op1", "y-op1 x-op1 w-op1 w-internal-op");
  }

  @Test
  public void testUsingModuleZ_Op1() throws Exception {
    assertCalls("testUsingModuleZ_Op1", "z-op1");
  }

  private void assertCalls(String flow, String expected) throws Exception {
    final Message consumedMessage = flowRunner(flow).run().getMessage();
    assertThat(consumedMessage, hasPayload(equalTo(expected)));
  }
}
