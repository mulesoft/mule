/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.db.integration.vendor.oracle;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.db.integration.DbTestUtil.selectData;
import static org.mule.runtime.module.db.integration.TestRecordUtil.assertRecords;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.integration.TestDbConfig;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.module.db.integration.model.Alien;
import org.mule.runtime.module.db.integration.model.Field;
import org.mule.runtime.module.db.integration.model.Record;
import org.mule.runtime.module.db.internal.domain.type.oracle.OracleXmlType;
import org.mule.runtime.core.util.IOUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class OracleInsertXmlTypeTestCase extends AbstractOracleXmlTypeTestCase {

  public OracleInsertXmlTypeTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getOracleResource();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/vendor/oracle/oracle-insert-xml-type-config.xml"};
  }

  @Test
  public void insertXmlTypeFromXmlType() throws Exception {
    assertAlienWasInserted(doTest(new XmlContentBuilder() {

      @Override
      public Object build(Connection connection) throws Exception {
        return OracleXmlType.createXmlType(connection, Alien.ET.getXml());
      }
    }));
  }

  @Test
  public void insertLargeXmlTypeFromInputStream() throws Exception {
    assertAlienWasInserted(doTest(new XmlContentBuilder() {

      @Override
      public Object build(Connection connection) throws Exception {
        return IOUtils.getResourceAsStream("integration/vendor/oracle/oracle-insert-xml-type-large-sample.xml", this.getClass());
      }
    }));
  }

  @Test
  public void insertXmlTypeFromString() throws Exception {
    assertAlienWasInserted(doTest(new XmlContentBuilder() {

      @Override
      public Object build(Connection connection) throws Exception {
        return Alien.ET.getXml();
      }
    }));
  }

  @Test
  public void insertXmlTypeFromWrongType() throws Exception {
    assertNoAliens(doTest(new XmlContentBuilder() {

      @Override
      public Object build(Connection connection) throws Exception {
        return new Integer(1);
      }
    }));
  }

  private void assertNoAliens(MuleEvent event) throws SQLException {
    assertThat(event.getError(), is(notNullValue()));

    List<Map<String, String>> result = selectData("SELECT name FROM Alien", getDefaultDataSource());
    assertRecords(result);
  }

  private void assertAlienWasInserted(MuleEvent event) throws SQLException {
    assertThat(event.getMessage().getPayload(), is(equalTo(1)));

    List<Map<String, String>> result = selectData("SELECT name FROM Alien", getDefaultDataSource());
    assertRecords(result, new Record(new Field("NAME", Alien.ET.getName())));
  }

  private interface XmlContentBuilder {

    Object build(Connection connection) throws Exception;
  }

  private MuleEvent doTest(XmlContentBuilder builder) throws Exception {
    DataSource defaultDataSource = getDefaultDataSource();
    Connection connection = defaultDataSource.getConnection();

    try {
      testDatabase.executeUpdate(connection, "DELETE FROM ALIEN");

      final MuleEvent responseEvent =
          flowRunner("insertXmlType").withPayload(TEST_MESSAGE).withInboundProperty("name", Alien.ET.getName()).run();

      return responseEvent;
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }
}
