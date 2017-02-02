/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.transaction;

import org.junit.Test;

public class TransactionalTestCase extends AbstractTxDbIntegrationTestCase {

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/transaction/transactional-config.xml"};
  }

  @Test
  public void commitTxUpdateOutsideABlock() throws Exception {
    executeTransaction("commitTxUpdateOutsideABlock");
    validateDbState(MERCURY);
  }

  @Test
  public void commitAnOptionalTxUpdateIsANonTxBlock() throws Exception {
    executeTransaction("commitAnOptionalTxUpdateIsANonTxBlock");
    validateDbState(MERCURY);
  }

  @Test
  public void commitNonTxUpdateInATxBlock() throws Exception {
    executeTransaction("commitNonTxUpdateInATxBlock");
    validateDbState(MERCURY);
  }

  @Test
  public void rollbackTxUpdateInATxBlock() throws Exception {
    executeTransaction("rollbackTxUpdateInATxBlock");
    validateDbState(MARS);
  }

  @Test
  public void rollbackOptionalTxUpdateInATxBlock() throws Exception {
    executeTransaction("rollbackOptionalTxUpdateInATxBlock");
    validateDbState(MARS);
  }
}
