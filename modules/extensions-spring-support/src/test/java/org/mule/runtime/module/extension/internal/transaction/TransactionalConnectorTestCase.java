/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.transaction;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.test.transactional.TestTransactionalConnection;
import org.mule.test.transactional.TransactionalExtension;

import org.junit.Test;

public class TransactionalConnectorTestCase extends ExtensionFunctionalTestCase {

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {TransactionalExtension.class};
  }

  @Override
  protected String getConfigFile() {
    return "transaction-config.xml";
  }

  @Test
  public void commited() throws Exception {
    flowRunner("commitLocalTx").withPayload("").run();
  }

  @Test
  public void rolledBack() throws Exception {
    flowRunner("rollbackLocalTx").withPayload("").run();
  }

  @Test
  public void executeTransactionless() throws Exception {
    TestTransactionalConnection connection = flowRunner("executeTransactionless").withPayload("").run().getMessage().getPayload();
    assertThat(connection.isTransactionBegun(), is(false));
    assertThat(connection.isTransactionCommited(), is(false));
    assertThat(connection.isTransactionRolledback(), is(false));
  }
}
