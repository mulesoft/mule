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
import org.mule.extension.db.integration.TestDbConfig;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.ParameterMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class UpdateMetadataTestCase extends AbstractDbIntegrationTestCase {

  public UpdateMetadataTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

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
  @Ignore("TODO MULE-10641")
  public void updateNoParametersInputMetadata() throws Exception {
    ParameterMetadataDescriptor parameters =
        getInputMetadata("updateMetadata", "update Planet set position = 1 where name = 'Mars'");
    assertThat(parameters.getType(), is(typeBuilder.nullType().build()));
  }

  @Test
  @Ignore("TODO MULE-10641")
  public void updateParameterizedInputMetadata() throws Exception {
    ParameterMetadataDescriptor parameters =
        getInputMetadata("updateMetadata", "update PLANET set NAME= :name where NAME='Mars'");

    assertThat(parameters.getType(), is(instanceOf(ObjectType.class)));
    assertThat(((ObjectType) parameters.getType()).getFields().size(), equalTo(1));
    assertFieldOfType(((ObjectType) parameters.getType()), "name", testDatabase.getNameFieldMetaDataType());
  }

  @Test
  @Ignore("TODO MULE-10641")
  public void updateWithExpressionInputMetadata() throws Exception {
    ParameterMetadataDescriptor parameters =
        getInputMetadata("updateMetadata", "update PLANET set NAME='#[data]' where POSITION=#[type]");
    assertThat(parameters.getType(), is(typeBuilder.anyType().build()));
  }

}
