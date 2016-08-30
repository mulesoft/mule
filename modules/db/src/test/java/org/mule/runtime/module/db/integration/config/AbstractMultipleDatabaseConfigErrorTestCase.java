/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.config;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.module.db.integration.TestDbConfig;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public abstract class AbstractMultipleDatabaseConfigErrorTestCase extends AbstractDbIntegrationTestCase {

  public AbstractMultipleDatabaseConfigErrorTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  @Test(expected = MessagingException.class)
  public void returnsErrorWhenMultipleJdbcConfigDefined() throws Exception {
    flowRunner("testFlow").withPayload(TEST_MESSAGE).run();
  }
}
