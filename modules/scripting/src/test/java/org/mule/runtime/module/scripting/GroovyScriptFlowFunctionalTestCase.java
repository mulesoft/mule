/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.scripting;

import org.junit.Test;

public class GroovyScriptFlowFunctionalTestCase extends GroovyScriptServiceFunctionalTestCase {

  @Test
  public void inlineScriptMutateProperty() throws Exception {
    flowRunner("inlineScriptMutateProperty").withPayload("").withOutboundProperty("foo", "bar").run();
  }

  @Test
  public void inlineScriptAddProperty() throws Exception {
    flowRunner("inlineScriptAddProperty").withPayload("").run();
  }

  @Test
  public void inlineScriptMutatePropertiesMap() throws Exception {
    flowRunner("inlineScriptMutatePropertiesMap").withPayload("").withOutboundProperty("foo", "bar").run();
  }

  @Test
  public void inlineScriptMutateVariable() throws Exception {
    flowRunner("inlineScriptMutateVariable").withPayload("").withFlowVariable("foo", "bar").run();
  }

  @Test
  public void inlineScriptAddVariable() throws Exception {
    flowRunner("inlineScriptAddVariable").withPayload("").run();
  }

  @Test
  public void inlineScriptMutateVariablesMap() throws Exception {
    flowRunner("inlineScriptMutateVariablesMap").withPayload("").withFlowVariable("foo", "bar").run();
  }

  @Test
  public void inlineScriptMutatePayload() throws Exception {
    flowRunner("inlineScriptMutatePayload").withPayload("").run();
  }

  @Test
  public void scriptExpressionVariables() throws Exception {
    flowRunner("scriptExpressionVariables").withPayload("").withFlowVariable("prop1", "Received")
        .withFlowVariable("prop2", "A-OK").run();
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"groovy-component-config-flow.xml", "groovy-component-config.xml"};
  }

  @Override
  protected String getConfigFile() {
    return null;
  }
}
