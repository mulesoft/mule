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
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.ParameterMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import org.junit.Test;

public class DeleteMetadataTestCase extends AbstractDbIntegrationTestCase {

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/delete/delete-metadata-config.xml"};
  }

  @Test
  public void deleteOutputMetadata() throws Exception {
    MetadataResult<ComponentMetadataDescriptor> metadata =
        getMetadata("deleteMetadata", "DELETE FROM PLANET WHERE name = 'Mars'");

    assertOutputPayload(metadata, typeLoader.load(int.class));
  }

  @Test
  public void bulkDeleteOutputMetadata() throws Exception {
    MetadataResult<ComponentMetadataDescriptor> metadata =
        getMetadata("bulkDeleteMetadata", "DELETE FROM PLANET WHERE name = 'Mars'");

    assertOutputPayload(metadata, typeLoader.load(int[].class));
  }

  @Test
  public void bulkDeleteNoParametersInputMetadata() throws Exception {
    ParameterMetadataDescriptor parameters =
        getParameterValuesMetadata("bulkDeleteMetadata", "DELETE FROM PLANET WHERE name = 'Mars'");

    assertThat(parameters.getType(), is(instanceOf(NullType.class)));
  }

  @Test
  public void bulkDeleteParameterizedInputMetadata() throws Exception {
    ParameterMetadataDescriptor parameters =
        getParameterValuesMetadata("bulkDeleteMetadata", "DELETE FROM PLANET WHERE name = :name");

    assertThat(parameters.getType(), is(instanceOf(ArrayType.class)));
    assertThat(((ArrayType) parameters.getType()).getType(), is(instanceOf(ObjectType.class)));
    MetadataType listGeneric = ((ArrayType) parameters.getType()).getType();
    assertThat(((ObjectType) listGeneric).getFields().size(), equalTo(1));
    assertFieldOfType(((ObjectType) listGeneric), "name", testDatabase.getNameFieldMetaDataType());
  }

  @Test
  public void deleteNoParametersInputMetadata() throws Exception {
    ParameterMetadataDescriptor parameters =
        getInputMetadata("deleteMetadata", "DELETE FROM PLANET WHERE name = 'Mars'");
    assertThat(parameters.getType(), is(instanceOf(NullType.class)));
  }

  @Test
  public void deleteParameterizedInputMetadata() throws Exception {
    ParameterMetadataDescriptor parameters =
        getInputMetadata("deleteMetadata", "DELETE FROM PLANET WHERE name = :name");

    assertThat(parameters.getType(), is(instanceOf(ObjectType.class)));
    assertThat(((ObjectType) parameters.getType()).getFields().size(), equalTo(1));
    assertFieldOfType(((ObjectType) parameters.getType()), "name", testDatabase.getNameFieldMetaDataType());
  }

  @Test
  public void deleteWithExpressionInputMetadata() throws Exception {
    ParameterMetadataDescriptor parameters =
        getInputMetadata("deleteMetadata", "DELETE FROM PLANET WHERE name = #[mel:payload]");
    assertThat(parameters.getType(), is(typeBuilder.anyType().build()));
  }
}
