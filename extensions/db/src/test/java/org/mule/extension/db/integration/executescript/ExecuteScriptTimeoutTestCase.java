/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.executescript;

import org.mule.extension.db.integration.AbstractQueryTimeoutTestCase;
import org.mule.extension.db.integration.model.AbstractTestDatabase;

public class ExecuteScriptTimeoutTestCase extends AbstractQueryTimeoutTestCase {

  public ExecuteScriptTimeoutTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/executescript/execute-script-timeout-config.xml"};
  }
}
