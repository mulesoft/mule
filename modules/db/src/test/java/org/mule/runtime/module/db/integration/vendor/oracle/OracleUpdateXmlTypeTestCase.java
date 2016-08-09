/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.vendor.oracle;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.integration.TestDbConfig;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.module.db.integration.model.Alien;
import org.mule.runtime.module.db.internal.domain.type.oracle.OracleXmlType;

import java.sql.Connection;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class OracleUpdateXmlTypeTestCase extends AbstractOracleXmlTypeTestCase {

  public OracleUpdateXmlTypeTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getOracleResource();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/vendor/oracle/oracle-update-xml-type-config.xml"};
  }

  @Test
  public void updateXmlTyeColumn() throws Exception {
    DataSource defaultDataSource = getDefaultDataSource();
    Connection connection = defaultDataSource.getConnection();

    Object xmlType;
    try {
      xmlType = OracleXmlType.createXmlType(connection, Alien.ET.getXml());

    } finally {
      if (connection != null) {
        connection.close();
      }
    }

    final MuleEvent responseEvent = flowRunner("updateWithXmlTypeParam").withPayload(xmlType).run();

    final MuleMessage response = responseEvent.getMessage();
    assertThat(response.getPayload(), equalTo(2));

    assertUpdatedAlienDscription();
  }
}
