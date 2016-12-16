/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.update;

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
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.ParameterMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("DB Extension")
@Stories("Update Statement")
public class UpdateMetadataTestCase extends AbstractDbIntegrationTestCase {

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/update/update-metadata-config.xml"};
  }

  @Test
  public void updateOutputMetadata() throws Exception {
    MetadataResult<ComponentMetadataDescriptor> metadata =
        getMetadata("updateMetadata", "update PLANET set NAME='Mercury' where POSITION=4");

    assertOutputPayload(metadata, typeLoader.load(StatementResult.class));
  }

  @Test
  public void bulkUpdateOutputMetadata() throws Exception {
    MetadataResult<ComponentMetadataDescriptor> metadata =
        getMetadata("bulkUpdateMetadata", "update PLANET set NAME='Mercury' where NAME= :name");

    assertOutputPayload(metadata, typeLoader.load(int[].class));
  }

  @Test
  public void bulkUpdateNoParametersInputMetadata() throws Exception {
    ParameterMetadataDescriptor parameters =
        getParameterValuesMetadata("bulkUpdateMetadata", "update Planet set position = 1 where name = 'Mars'");
    assertThat(parameters.getType(), is(instanceOf(NullType.class)));
  }

  @Test
  public void bulkUpdateParameterizedInputMetadata() throws Exception {
    ParameterMetadataDescriptor parameters =
        getParameterValuesMetadata("bulkUpdateMetadata", "update PLANET set NAME='Mercury' where NAME= :name");

    assertThat(parameters.getType(), is(instanceOf(ArrayType.class)));
    assertThat(((ArrayType) parameters.getType()).getType(), is(instanceOf(ObjectType.class)));
    MetadataType listGeneric = ((ArrayType) parameters.getType()).getType();
    assertThat(((ObjectType) listGeneric).getFields().size(), equalTo(1));
    assertFieldOfType(((ObjectType) listGeneric), "name", testDatabase.getNameFieldMetaDataType());
  }

  @Test
  public void updateNoParametersInputMetadata() throws Exception {
    ParameterMetadataDescriptor parameters =
        getInputMetadata("updateMetadata", "update Planet set position = 1 where name = 'Mars'");
    assertThat(parameters.getType(), is(instanceOf(NullType.class)));
  }

  @Test
  public void updateParameterizedInputMetadata() throws Exception {
    ParameterMetadataDescriptor parameters =
        getInputMetadata("updateMetadata", "update PLANET set NAME= :name where NAME='Mars'");

    assertThat(parameters.getType(), is(instanceOf(ObjectType.class)));
    assertThat(((ObjectType) parameters.getType()).getFields().size(), equalTo(1));
    assertFieldOfType(((ObjectType) parameters.getType()), "name", testDatabase.getNameFieldMetaDataType());
  }

  @Test
  public void updateWithExpressionInputMetadata() throws Exception {
    ParameterMetadataDescriptor parameters =
        getInputMetadata("updateMetadata", "update PLANET set NAME='#[data]' where POSITION=#[type]");
    assertThat(parameters.getType(), is(typeBuilder.anyType().build()));
  }

}
