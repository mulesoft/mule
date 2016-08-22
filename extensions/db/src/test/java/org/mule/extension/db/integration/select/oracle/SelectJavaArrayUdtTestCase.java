/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.select.oracle;

import static org.mule.extension.db.integration.TestDbConfig.getOracleResource;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecords;
import static org.mule.extension.db.integration.model.Contact.CONTACT1;
import static org.mule.extension.db.integration.model.Contact.CONTACT2;
import static org.mule.extension.db.integration.model.Region.NORTHWEST;
import static org.mule.extension.db.integration.model.Region.SOUTHWEST;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.extension.db.integration.model.Field;
import org.mule.extension.db.integration.model.OracleTestDatabase;
import org.mule.extension.db.integration.model.Record;
import org.mule.runtime.api.message.MuleMessage;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectJavaArrayUdtTestCase extends AbstractDbIntegrationTestCase {

  public SelectJavaArrayUdtTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    List<Object[]> params = new LinkedList<>();

    if (!getOracleResource().isEmpty()) {
      params.add(new Object[] {"integration/config/oracle-mapped-udt-db-config.xml", new OracleTestDatabase()});
    }

    return params;
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/select/oracle/select-udt-array-config.xml"};
  }

  @Test
  public void returnsDefaultArray() throws Exception {
    MuleMessage response = flowRunner("returnsDefaultArray").run().getMessage();

    assertRecords(response.getPayload(),
                  new Record(new Field("REGION_NAME", NORTHWEST.getName()), new Field("ZIPS", NORTHWEST.getZips())),
                  new Record(new Field("REGION_NAME", SOUTHWEST.getName()), new Field("ZIPS", SOUTHWEST.getZips())));
  }

  @Test
  public void returnsMappedObjectArray() throws Exception {
    MuleMessage response = flowRunner("returnsCustomArray").run().getMessage();

    assertRecords(response.getPayload(),
                  new Record(new Field("CONTACT_NAME", CONTACT1.getName()), new Field("DETAILS", CONTACT1.getDetails())),
                  new Record(new Field("CONTACT_NAME", CONTACT2.getName()), new Field("DETAILS", CONTACT2.getDetails())));
  }
}
