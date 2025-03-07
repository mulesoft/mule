/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.registry;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TRANSACTION_MANAGER;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.registerObject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import org.mule.tck.junit4.AbstractMuleContextTestCase;

import javax.inject.Inject;

import org.junit.Test;

import jakarta.transaction.TransactionManager;

public class TransactionManagerInjectTestCase extends AbstractMuleContextTestCase {

  @Test
  public void injectTransactionManager() throws Exception {
    TransactionManager manager = mock(TransactionManager.class);
    registerObject(muleContext, OBJECT_TRANSACTION_MANAGER, manager);
    TransactionClient txClient = new TransactionClient();
    muleContext.getInjector().inject(txClient);

    assertThat(txClient.getTxMgr(), is(manager));
  }

  public static class TransactionClient {

    private TransactionManager txMgr;

    public TransactionManager getTxMgr() {
      return txMgr;
    }

    @Inject
    public void setTxMgr(TransactionManager txMgr) {
      this.txMgr = txMgr;
    }
  }

}
