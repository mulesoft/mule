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
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import java.util.Optional;

import org.junit.Test;

public class SelectMetadataInputTestCase extends AbstractDbIntegrationTestCase {

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/select/select-metadata-config.xml"};
  }

  @Test
  public void returnsNullSelectMetadataUnParameterizedQuery() throws Exception {
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata = getMetadata("selectMetadata", "select * from PLANET");

    assertThat(metadata.isSuccess(), is(true));
    assertThat(metadata.get().getModel().getAllParameterModels().stream()
        .filter(p -> p.getName().equals("inputParameters"))
        .findFirst().get().getType(),
               is(instanceOf(NullType.class)));
  }

  @Test
  public void returnsAnySelectInputMetadataFromNotSupportedParameterizedQuery() throws Exception {

    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata =
        getMetadata("selectMetadata",
                    "select * from PLANET where id = #[mel:payload.id] and name = #[mel:message.outboundProperties.updateCount]");

    assertThat(metadata.isSuccess(), is(true));
    assertThat(metadata.get().getModel().getAllParameterModels().stream()
        .filter(p -> p.getName().equals("inputParameters"))
        .findFirst().get().getType(),
               is(typeBuilder.anyType().build()));
  }

  @Test
  public void returnsSelectInputMetadataFromBeanParameterizedQuery() throws Exception {
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata = getMetadata("selectMetadata",
                                                                                       "select * from PLANET where id = :id and name = :name");

    assertThat(metadata.isSuccess(), is(true));
    ObjectType type = (ObjectType) metadata.get().getModel().getAllParameterModels().stream()
        .filter(p -> p.getName().equals("inputParameters"))
        .findFirst().get().getType();
    assertThat(type.getFields().size(), equalTo(2));

    Optional<ObjectFieldType> id = type.getFieldByName("id");
    assertThat(id.isPresent(), is(true));
    assertThat(id.get().getValue(), equalTo(testDatabase.getIdFieldMetaDataType()));

    Optional<ObjectFieldType> name = type.getFieldByName("name");
    assertThat(name.isPresent(), is(true));
    assertThat(name.get().getValue(), equalTo(testDatabase.getNameFieldMetaDataType()));
  }
}
