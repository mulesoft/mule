/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.executescript;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.core.api.lifecycle.InitialisationException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class InvalidExecuteScriptTestCase extends AbstractDbIntegrationTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/executescript/execute-script-file-and-text-config.xml"};
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    super.doSetUpBeforeMuleContextCreation();
    expectedException.expect(InitialisationException.class);
    expectedException.expectMessage(containsString("parameters cannot be set at the same time"));
  }

  @Test
  public void exclusiveFileAndSql() throws Exception {
    fail("Test should have failed");
  }
}
