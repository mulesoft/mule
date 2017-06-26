/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.extension.http.api.request.validator.ResponseValidatorTypedException;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class ModuleWithGlobalElementTestCase extends AbstractModuleWithHttpTestCase {

  @Parameterized.Parameter
  public String configFile;

  @Parameterized.Parameter(1)
  public String[] paths;

  @Parameterized.Parameters(name = "{index}: Running tests for {0} ")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        //simple scenario
        {"flows/flows-using-module-global-elements.xml", new String[] {MODULE_GLOBAL_ELEMENT_XML}},
        //nested modules scenario
        {"flows/nested/flows-using-module-global-elements-proxy.xml",
            new String[] {MODULE_GLOBAL_ELEMENT_XML, MODULE_GLOBAL_ELEMENT_PROXY_XML}},
        //nested^2 modules scenario
        {"flows/nested/flows-using-module-global-elements-another-proxy.xml",
            new String[] {MODULE_GLOBAL_ELEMENT_XML, MODULE_GLOBAL_ELEMENT_PROXY_XML, MODULE_GLOBAL_ELEMENT_ANOTHER_PROXY_XML}},
        //using literals and expressions that will be resolved accordingly scenario
        {"flows/flows-using-module-global-elements-with-expressions.xml", new String[] {MODULE_GLOBAL_ELEMENT_XML}}
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
  public void testHttpDoLogin() throws Exception {
    assertFlowForUsername("testHttpDoLogin", "userLP");
  }

  @Test
  public void testHttpDontLoginThrowsException() throws Exception {
    try {
      flowRunner("testHttpDontLogin").run();
      fail("Should not have reach here");
    } catch (MessagingException me) {
      Throwable cause = me.getEvent().getError().get().getCause();
      assertThat(cause, instanceOf(ResponseValidatorTypedException.class));
      assertThat(cause.getMessage(), containsString("failed: unauthorized (401)"));
    }
  }

  @Test
  public void testHttpDoLoginGonnet() throws Exception {
    assertFlowForUsername("testHttpDoLoginGonnet", "userGonnet");
  }
}
