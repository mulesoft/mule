/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.operation;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Test;

public class OperationWithFieldParameterTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "operation-field-parameter.xml";
  }

  @Test
  public void useFieldParameter() throws Exception {
    String value = flowRunner("spreadTheWord").run().getMessage().getPayload().getValue().toString();
    assertThat(value, equalTo("meat is badmeat is badmeat is badmeat is badmeat is bad"));
  }

  @Test
  public void fieldParameterAvailableAtInitialise() throws Exception {
    String value = flowRunner("negativeEloquence").run().getMessage().getPayload().getValue().toString();
    assertThat(value, equalTo("meat is bad"));
  }

  @Test
  public void fieldParameterWithDefaultValue() throws Exception {
    String value = flowRunner("defaultEloquence").run().getMessage().getPayload().getValue().toString();
    assertThat(value, equalTo("meat is badmeat is bad"));
  }
}
