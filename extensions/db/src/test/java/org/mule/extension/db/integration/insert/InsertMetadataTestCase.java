/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.insert;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.extension.db.api.StatementResult;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.TestDbConfig;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.ParameterMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class InsertMetadataTestCase extends AbstractDbIntegrationTestCase {


  public InsertMetadataTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/insert/insert-metadata-config.xml"};
  }

  @Test
  public void insertOutputMetadata() throws Exception {
    MetadataResult<ComponentMetadataDescriptor> metadata =
        getMetadata("insertMetadata", "INSERT INTO PLANET(POSITION, NAME) VALUES (777, 'Mercury')");

    assertOutputPayload(metadata, typeLoader.load(StatementResult.class));
  }

  @Test
  public void bulkUpdateOutputMetadata() throws Exception {
    MetadataResult<ComponentMetadataDescriptor> metadata =
        getMetadata("bulkInsertMetadata", "INSERT INTO PLANET(POSITION, NAME) VALUES (777, :name)");

    assertOutputPayload(metadata, typeLoader.load(int[].class));
  }

  @Test
  public void insertNoParametersInputMetadata() throws Exception {
    ParameterMetadataDescriptor parameters =
        getInputMetadata("insertMetadata", "INSERT INTO PLANET(POSITION, NAME) VALUES (777, 'Mercury')");
    assertThat(parameters.getType(), is(typeBuilder.nullType().build()));
  }

  @Test
  public void insertParameterizedInputMetadata() throws Exception {
    ParameterMetadataDescriptor parameters =
        getInputMetadata("insertMetadata", "INSERT INTO PLANET(POSITION, NAME) VALUES (777, :name)");

    assertThat(parameters.getType(), is(instanceOf(ObjectType.class)));
    assertThat(((ObjectType) parameters.getType()).getFields().size(), equalTo(1));
    assertFieldOfType(((ObjectType) parameters.getType()), "name", testDatabase.getNameFieldMetaDataType());
  }

  @Test
  public void insertWithExpressionInputMetadata() throws Exception {
    ParameterMetadataDescriptor parameters =
        getInputMetadata("insertMetadata", "INSERT INTO PLANET(POSITION, NAME) VALUES (777, #[payload])");
    assertThat(parameters.getType(), is(typeBuilder.anyType().build()));
  }

}
