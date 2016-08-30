/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.xa;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.TransactionConfigEnum.ACTION_ALWAYS_BEGIN;
import static org.mule.runtime.module.db.integration.DbTestUtil.selectData;
import static org.mule.runtime.module.db.integration.TestRecordUtil.assertRecords;
import static org.mule.runtime.module.db.integration.model.Planet.MARS;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.transaction.XaTransactionFactory;
import org.mule.runtime.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.module.db.integration.model.DerbyTestDatabase;
import org.mule.runtime.module.db.integration.model.Field;
import org.mule.runtime.module.db.integration.model.Record;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized;

public abstract class AbstractXaTransactionalTestCase extends AbstractDbIntegrationTestCase {

  public AbstractXaTransactionalTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return Collections.singletonList(new Object[] {"integration/config/derby-xa-db-config.xml", new DerbyTestDatabase()});
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {getTransactionManagerResource(), "integration/xa/xa-transactional-config.xml"};
  }

  protected abstract String getTransactionManagerResource();

  @Test
  public void commitsChanges() throws Exception {
    MuleMessage response =
        flowRunner("jdbcCommit").transactionally(ACTION_ALWAYS_BEGIN, new XaTransactionFactory()).run().getMessage();

    assertThat(response.getPayload(), equalTo(1));

    List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
    assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
  }

  @Test
  public void rollbacksChanges() throws Exception {
    MessagingException e =
        flowRunner("jdbcRollback").transactionally(ACTION_ALWAYS_BEGIN, new XaTransactionFactory()).runExpectingException();

    assertThat(e.getCause(), instanceOf(IllegalStateException.class));
    List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
    assertRecords(result, new Record(new Field("NAME", MARS.getName()), new Field("POSITION", 4)));
  }

  @Test
  public void commitsChangesWhenMpIsNotTransactionalOnRollback() throws Exception {
    MessagingException e = flowRunner("rollbackWithNonTransactionalMP")
        .transactionally(ACTION_ALWAYS_BEGIN, new XaTransactionFactory()).runExpectingException();

    assertThat(e.getCause(), instanceOf(IllegalStateException.class));
    List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
    assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
  }

  @Test
  public void commitsChangesWhenMpIsNotTransactionalOnCommit() throws Exception {
    MessagingException e = flowRunner("commitWithNonTransactionalMP")
        .transactionally(ACTION_ALWAYS_BEGIN, new XaTransactionFactory()).runExpectingException();

    assertThat(e.getCause(), instanceOf(IllegalStateException.class));
    List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
    assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
  }

}
