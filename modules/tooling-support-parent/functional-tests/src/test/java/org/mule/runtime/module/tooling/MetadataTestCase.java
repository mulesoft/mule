/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.metadata.internal.utils.MetadataTypeWriter;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.app.declaration.api.OperationElementDeclaration;
import org.mule.runtime.app.declaration.api.SourceElementDeclaration;
import org.mule.runtime.module.tooling.api.metadata.ComponentMetadataTypes;

import org.junit.Test;

public class MetadataTestCase extends DeclarationSessionTestCase {

  @Test
  public void sourceDynamicTypes() {
    SourceElementDeclaration sourceElementDeclaration = sourceDeclaration(CONFIG_NAME, null, "America", "USA", "SFO");
    MetadataResult<ComponentMetadataTypes> containerTypeMetadataResult = session.getMetadataTypes(sourceElementDeclaration);
    assertThat(containerTypeMetadataResult.isSuccess(), is(true));

    //input parameters
    assertThat(containerTypeMetadataResult.get().getInputMetadata().size(), is(1));
    assertThat(new MetadataTypeWriter().toString(containerTypeMetadataResult.get().getInputMetadata().get("onSuccessParameter")),
               equalTo("%type _:Java = @default(\"value\" : \"America|USA|SFO\") String"));

    //output
    assertThat(containerTypeMetadataResult.get().getOutputMetadata().isPresent(), is(true));
    assertThat(new MetadataTypeWriter().toString(containerTypeMetadataResult.get().getOutputMetadata().get()),
               equalTo("%type _:Java = @default(\"value\" : \"America|USA|SFO\") String"));

    //output attributes
    assertThat(containerTypeMetadataResult.get().getOutputAttributesMetadata().isPresent(), is(true));
    assertThat(new MetadataTypeWriter().toString(containerTypeMetadataResult.get().getOutputAttributesMetadata().get()),
               equalTo("%type _:Java = @typeId(\"value\" : \"org.mule.tooling.extensions.metadata.api.source.StringAttributes\") {\n"
                   +
                   "  \"value\"? : @default(\"value\" : \"America|USA|SFO\") String\n" +
                   "}"));

  }

  @Test
  public void operationDynamicTypes() {
    OperationElementDeclaration operationElementDeclaration =
        multiLevelCompleteOPDeclaration(CONFIG_NAME, "America", "USA", "SFO");
    MetadataResult<ComponentMetadataTypes> containerTypeMetadataResult = session.getMetadataTypes(operationElementDeclaration);
    assertThat(containerTypeMetadataResult.isSuccess(), is(true));

    //input parameters
    assertThat(containerTypeMetadataResult.get().getInputMetadata().size(), is(1));
    assertThat(new MetadataTypeWriter().toString(containerTypeMetadataResult.get().getInputMetadata().get("dynamicParam")),
               equalTo("%type _:Java = @default(\"value\" : \"America|USA|SFO\") String"));

    //output
    assertThat(containerTypeMetadataResult.get().getOutputMetadata().isPresent(), is(true));
    assertThat(new MetadataTypeWriter().toString(containerTypeMetadataResult.get().getOutputMetadata().get()),
               equalTo("%type _:Java = @default(\"value\" : \"America|USA|SFO\") String"));

    //output attributes
    assertThat(containerTypeMetadataResult.get().getOutputAttributesMetadata().isPresent(), is(true));
    assertThat(new MetadataTypeWriter().toString(containerTypeMetadataResult.get().getOutputAttributesMetadata().get()),
               equalTo("%type _:Java = @typeId(\"value\" : \"org.mule.tooling.extensions.metadata.api.source.StringAttributes\") {\n"
                   +
                   "  \"value\"? : @default(\"value\" : \"America|USA|SFO\") String\n" +
                   "}"));
  }

  @Test
  public void operationDynamicTypesPartialKey() {
    OperationElementDeclaration operationElementDeclaration = multiLevelOPDeclaration(CONFIG_NAME, "America", "USA");
    MetadataResult<ComponentMetadataTypes> containerTypeMetadataResult = session.getMetadataTypes(operationElementDeclaration);
    assertThat(containerTypeMetadataResult.isSuccess(), is(false));
  }

}
