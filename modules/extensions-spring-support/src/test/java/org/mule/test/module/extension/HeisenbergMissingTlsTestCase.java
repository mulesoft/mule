/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractOfType;
import org.mule.runtime.core.api.config.ConfigurationException;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class HeisenbergMissingTlsTestCase extends InvalidExtensionConfigTestCase {

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    expectedException.expectCause(new BaseMatcher<Throwable>() {

      @Override
      public boolean matches(Object item) {
        ConfigurationException configurationException = extractOfType((Throwable) item, ConfigurationException.class).get();
        assertThat(configurationException.getMessage(), equalTo("Element <heisenberg:secure-connection> in line 17 of file "
            + "heisenberg-missing-tls-connection-config.xml is missing "
            + "required parameter 'tls-context'"));

        return true;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("not the expected exception");
      }
    });
  }

  @Override
  protected String getConfigFile() {
    return "heisenberg-missing-tls-connection-config.xml";
  }
}
