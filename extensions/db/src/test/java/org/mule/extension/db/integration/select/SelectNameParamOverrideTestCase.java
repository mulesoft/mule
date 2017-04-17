/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.select;

import static org.mule.extension.db.integration.TestRecordUtil.assertMessageContains;
import static org.mule.extension.db.integration.TestRecordUtil.getMarsRecord;
import static org.mule.extension.db.integration.TestRecordUtil.getVenusRecord;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.api.message.Message;

import org.junit.Test;

public class SelectNameParamOverrideTestCase extends AbstractDbIntegrationTestCase {

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/select/select-name-param-override-config.xml"};
  }

  @Test
  public void usesParamOverriddenByName() throws Exception {
    Message response = flowRunner("overriddenParamsByName").keepStreamsOpen().run().getMessage();
    assertMessageContains(response, getMarsRecord());
  }

  @Test
  public void usesInlineParamOverriddenByName() throws Exception {
    Message response = flowRunner("inlineOverriddenParamsByName").keepStreamsOpen().run().getMessage();
    assertMessageContains(response, getVenusRecord());
  }
}

