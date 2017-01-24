/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.delete;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import org.junit.Test;

public class DeleteMetadataTestCase extends AbstractDbIntegrationTestCase {

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/delete/delete-metadata-config.xml"};
  }

  @Test
  public void deleteOutputMetadata() throws Exception {
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata =
        getMetadata("deleteMetadata", "DELETE FROM PLANET WHERE name = 'Mars'");

    assertOutputPayload(metadata, typeLoader.load(int.class));
  }

  @Test
  public void bulkDeleteOutputMetadata() throws Exception {
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata =
        getMetadata("bulkDeleteMetadata", "DELETE FROM PLANET WHERE name = 'Mars'");

    assertOutputPayload(metadata, typeLoader.load(int[].class));
  }

  @Test
  public void bulkDeleteNoParametersInputMetadata() throws Exception {
    MetadataType parametersTypes =
        getParameterValuesMetadata("bulkDeleteMetadata", "DELETE FROM PLANET WHERE name = 'Mars'");

    assertThat(parametersTypes, is(instanceOf(NullType.class)));
  }

  @Test
  public void bulkDeleteParameterizedInputMetadata() throws Exception {
    MetadataType parametersTypes =
        getParameterValuesMetadata("bulkDeleteMetadata", "DELETE FROM PLANET WHERE name = :name");

    assertThat(parametersTypes, is(instanceOf(ArrayType.class)));
    assertThat(((ArrayType) parametersTypes).getType(), is(instanceOf(ObjectType.class)));
    MetadataType listGeneric = ((ArrayType) parametersTypes).getType();
    assertThat(((ObjectType) listGeneric).getFields().size(), equalTo(1));
    assertFieldOfType(((ObjectType) listGeneric), "name", testDatabase.getNameFieldMetaDataType());
  }

  @Test
  public void deleteNoParametersInputMetadata() throws Exception {
    MetadataType parametersTypes =
        getInputMetadata("deleteMetadata", "DELETE FROM PLANET WHERE name = 'Mars'");
    assertThat(parametersTypes, is(instanceOf(NullType.class)));
  }

  @Test
  public void deleteParameterizedInputMetadata() throws Exception {
    MetadataType parametersTypes =
        getInputMetadata("deleteMetadata", "DELETE FROM PLANET WHERE name = :name");

    assertThat(parametersTypes, is(instanceOf(ObjectType.class)));
    assertThat(((ObjectType) parametersTypes).getFields().size(), equalTo(1));
    assertFieldOfType(((ObjectType) parametersTypes), "name", testDatabase.getNameFieldMetaDataType());
  }

  @Test
  public void deleteWithExpressionInputMetadata() throws Exception {
    MetadataType parametersTypes =
        getInputMetadata("deleteMetadata", "DELETE FROM PLANET WHERE name = #[mel:payload]");
    assertThat(parametersTypes, is(typeBuilder.anyType().build()));
  }
}
