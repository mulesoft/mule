/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import org.junit.Test;

/**
 * Guarantees that the macro expansion works for direct dependency on the module but also with the nested one
 */
public class MultipleModuleWithGlobalElementTestCase extends AbstractModuleWithHttpTestCase {

  @Override
  protected String[] getModulePaths() {
    return new String[] {MODULE_GLOBAL_ELEMENT_XML, MODULE_GLOBAL_ELEMENT_PROXY_XML, MODULE_GLOBAL_ELEMENT_ANOTHER_PROXY_XML};
  }

  @Override
  protected String getConfigFile() {
    return "flows/nested/flows-using-module-global-elements-another-proxy-and-module-global-elements.xml";
  }

  @Test
  public void testHttpDoLoginThroughNestedModules() throws Exception {
    assertFlowForUsername("testHttpDoLoginThroughNestedModules", "nestedUser");
  }

  @Test
  public void testHttpDoLoginThroughDirectModule() throws Exception {
    assertFlowForUsername("testHttpDoLoginThroughDirectModule", "directUser");
  }
}
