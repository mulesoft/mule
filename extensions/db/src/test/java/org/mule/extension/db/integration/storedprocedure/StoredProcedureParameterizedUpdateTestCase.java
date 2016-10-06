/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.storedprocedure;

import static org.junit.Assume.assumeThat;
import org.mule.extension.db.integration.matcher.SupportsReturningStoredProcedureResultsWithoutParameters;

public class StoredProcedureParameterizedUpdateTestCase extends AbstractStoredProcedureParameterizedUpdateTestCase {

  @Override
  public void setupStoredProcedure() throws Exception {
    assumeThat(getDefaultDataSource(), new SupportsReturningStoredProcedureResultsWithoutParameters());
    super.setupStoredProcedure();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/storedprocedure/stored-procedure-parameterized-update-config.xml"};
  }

}
