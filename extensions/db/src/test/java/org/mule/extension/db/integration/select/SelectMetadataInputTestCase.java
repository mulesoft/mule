/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.select;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.TestDbConfig;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectMetadataInputTestCase extends AbstractDbIntegrationTestCase {

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/select/select-metadata-config.xml"};
  }

  @Test
  public void returnsNullSelectMetadataUnParameterizedQuery() throws Exception {
    MetadataResult<ComponentMetadataDescriptor> metadata = getMetadata("selectMetadata", "select * from PLANET");

    assertThat(metadata.isSuccess(), is(true));
    assertThat(metadata.get().getInputMetadata().get().getParameterMetadata("inputParameters").get().getType(),
               is(typeBuilder.nullType().build()));
  }

  @Test
  public void returnsAnySelectInputMetadataFromNotSupportedParameterizedQuery() throws Exception {

    MetadataResult<ComponentMetadataDescriptor> metadata =
        getMetadata("selectMetadata",
                    "select * from PLANET where id = #[payload.id] and name = #[message.outboundProperties.updateCount]");

    assertThat(metadata.isSuccess(), is(true));
    assertThat(metadata.get().getInputMetadata().get().getParameterMetadata("inputParameters").get().getType(),
               is(typeBuilder.anyType().build()));
  }

  @Test
  public void returnsSelectInputMetadataFromBeanParameterizedQuery() throws Exception {
    MetadataResult<ComponentMetadataDescriptor> metadata = getMetadata("selectMetadata",
                                                                       "select * from PLANET where id = :id and name = :name");

    assertThat(metadata.isSuccess(), is(true));
    ObjectType type =
        (ObjectType) metadata.get().getInputMetadata().get().getParameterMetadata("inputParameters").get().getType();
    assertThat(type.getFields().size(), equalTo(2));

    Optional<ObjectFieldType> id = type.getFieldByName("id");
    assertThat(id.isPresent(), is(true));
    assertThat(id.get().getValue(), equalTo(testDatabase.getIdFieldMetaDataType()));

    Optional<ObjectFieldType> name = type.getFieldByName("name");
    assertThat(name.isPresent(), is(true));
    assertThat(name.get().getValue(), equalTo(testDatabase.getNameFieldMetaDataType()));
  }

}
