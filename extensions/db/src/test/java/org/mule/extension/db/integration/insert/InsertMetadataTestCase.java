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
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import org.junit.Test;

public class InsertMetadataTestCase extends AbstractDbIntegrationTestCase {

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/insert/insert-metadata-config.xml"};
  }

  @Test
  public void insertOutputMetadata() throws Exception {
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata =
        getMetadata("insertMetadata", "INSERT INTO PLANET(POSITION, NAME) VALUES (777, 'Mercury')");

    assertOutputPayload(metadata, typeLoader.load(StatementResult.class));
  }

  @Test
  public void bulkInsertOutputMetadata() throws Exception {
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata =
        getMetadata("bulkInsertMetadata", "INSERT INTO PLANET(POSITION, NAME) VALUES (777, :name)");

    assertOutputPayload(metadata, typeLoader.load(int[].class));
  }

  @Test
  public void bulkInsertNoParametersInputMetadata() throws Exception {
    MetadataType parameters =
        getParameterValuesMetadata("bulkInsertMetadata", "INSERT INTO PLANET(POSITION, NAME) VALUES (777, 'Mercury')");
    assertThat(parameters, is(instanceOf(NullType.class)));
  }

  @Test
  public void bulkInsertParameterizedInputMetadata() throws Exception {
    MetadataType parameters =
        getParameterValuesMetadata("bulkInsertMetadata", "INSERT INTO PLANET(POSITION, NAME) VALUES (777, :name)");

    assertThat(parameters, is(instanceOf(ArrayType.class)));
    assertThat(((ArrayType) parameters).getType(), is(instanceOf(ObjectType.class)));
    MetadataType listGeneric = ((ArrayType) parameters).getType();
    assertThat(((ObjectType) listGeneric).getFields().size(), equalTo(1));
    assertFieldOfType(((ObjectType) listGeneric), "name", testDatabase.getNameFieldMetaDataType());
  }

  @Test
  public void insertNoParametersInputMetadata() throws Exception {
    MetadataType parameters =
        getInputMetadata("insertMetadata", "INSERT INTO PLANET(POSITION, NAME) VALUES (777, 'Mercury')");
    assertThat(parameters, is(instanceOf(NullType.class)));
  }

  @Test
  public void insertParameterizedInputMetadata() throws Exception {
    MetadataType parameters =
        getInputMetadata("insertMetadata", "INSERT INTO PLANET(POSITION, NAME) VALUES (777, :name)");

    assertThat(parameters, is(instanceOf(ObjectType.class)));
    assertThat(((ObjectType) parameters).getFields().size(), equalTo(1));
    assertFieldOfType(((ObjectType) parameters), "name", testDatabase.getNameFieldMetaDataType());
  }

  @Test
  public void insertWithExpressionInputMetadata() throws Exception {
    MetadataType parameters =
        getInputMetadata("insertMetadata", "INSERT INTO PLANET(POSITION, NAME) VALUES (777, #[mel:payload])");
    assertThat(parameters, is(typeBuilder.anyType().build()));
  }

}
