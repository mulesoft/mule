/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import org.mule.extension.db.integration.model.DerbyTestDatabase;
import org.mule.extension.db.integration.model.MySqlTestDatabase;
import org.mule.extension.db.integration.model.OracleTestDatabase;

import java.util.ArrayList;
import java.util.List;

public class TestDbConfig {

  private static boolean USE_DERBY = true;
  private static boolean USE_MYSQL = false;
  private static boolean USE_ORACLE = false;

  public static List<Object[]> getResources() {
    List<Object[]> result = new ArrayList<>();

    result.addAll(getDerbyResource());
    result.addAll(getMySqlResource());
    result.addAll(getOracleResource());

    return result;
  }

  public static List<Object[]> getDerbyResource() {
    if (USE_DERBY) {
      final DerbyTestDatabase derbyTestDatabase = new DerbyTestDatabase();
      return singletonList(new Object[] {"integration/config/derby-datasource.xml", derbyTestDatabase,
          derbyTestDatabase.getDbType()});
    } else {
      return emptyList();
    }
  }

  public static List<Object[]> getMySqlResource() {
    if (USE_MYSQL) {
      final MySqlTestDatabase mySqlTestDatabase = new MySqlTestDatabase();
      return singletonList(new Object[] {"integration/config/mysql-db-config.xml", mySqlTestDatabase,
          mySqlTestDatabase.getDbType()});
    } else {
      return emptyList();
    }
  }

  public static List<Object[]> getOracleResource() {
    if (USE_ORACLE) {
      final OracleTestDatabase oracleTestDatabase = new OracleTestDatabase();
      return singletonList(new Object[] {"integration/config/oracle-db-config.xml", oracleTestDatabase,
          oracleTestDatabase.getDbType()});
    } else {
      return emptyList();
    }
  }
}
