/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static java.lang.String.format;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.api.metadata.resolving.FailureCode.COMPONENT_NOT_FOUND;
import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_METADATA_KEY;
import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;
import static org.mule.runtime.api.metadata.resolving.MetadataComponent.COMPONENT;
import static org.mule.runtime.api.metadata.resolving.MetadataComponent.OUTPUT_PAYLOAD;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configLessConnectionLessOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configLessOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.multiLevelCompleteOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.multiLevelOPDeclarationPartialTypeKeys;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.multiLevelShowInDslGroupOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.requiresConfigurationOutputTypeKeyResolverOP;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.sourceDeclaration;
import org.mule.metadata.internal.utils.MetadataTypeWriter;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataTypesDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.app.declaration.api.OperationElementDeclaration;
import org.mule.runtime.app.declaration.api.SourceElementDeclaration;

import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.Test;

public class MetadataTypesTestCase extends DeclarationSessionTestCase {

  @Test
  public void sourceDynamicTypes() {
    SourceElementDeclaration sourceElementDeclaration = sourceDeclaration(CONFIG_NAME, null, "America", "USA", "SFO");
    MetadataResult<ComponentMetadataTypesDescriptor> containerTypeMetadataResult =
        session.resolveComponentMetadata(sourceElementDeclaration);
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
    MetadataResult<ComponentMetadataTypesDescriptor> containerTypeMetadataResult =
        session.resolveComponentMetadata(operationElementDeclaration);
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

  // TODO MULE-18680: Optional levels are required for multi-level keys!
  @Test
  public void operationDynamicTypesPartialKey() {
    OperationElementDeclaration operationElementDeclaration =
        multiLevelOPDeclarationPartialTypeKeys(CONFIG_NAME, "America", "USA");
    MetadataResult<ComponentMetadataTypesDescriptor> containerTypeMetadataResult =
        session.resolveComponentMetadata(operationElementDeclaration);
    assertThat(containerTypeMetadataResult.isSuccess(), is(false));
    assertThat(containerTypeMetadataResult.getFailures(), hasSize(1));
    assertThat(containerTypeMetadataResult.getFailures().get(0).getFailureCode(), is(INVALID_METADATA_KEY));
    assertThat(containerTypeMetadataResult.getFailures().get(0).getMessage(), containsString("Missing levels: [city]"));
  }

  // TODO MULE-18680 Optional levels are required for multi-level keys!
  @Test
  public void operationDynamicTypesNoKey() {
    OperationElementDeclaration operationElementDeclaration = multiLevelOPDeclarationPartialTypeKeys(CONFIG_NAME, null, null);
    MetadataResult<ComponentMetadataTypesDescriptor> containerTypeMetadataResult =
        session.resolveComponentMetadata(operationElementDeclaration);
    assertThat(containerTypeMetadataResult.isSuccess(), is(false));
    assertThat(containerTypeMetadataResult.getFailures(), hasSize(1));
    assertThat(containerTypeMetadataResult.getFailures().get(0).getFailureCode(), is(INVALID_METADATA_KEY));
    assertThat(containerTypeMetadataResult.getFailures().get(0).getMessage(),
               containsString("Missing levels: [continent, country, city]"));
  }

  @Test
  public void operationDynamicTypesSingleLevelKey() {
    OperationElementDeclaration operationElementDeclaration = configLessOPDeclaration(CONFIG_NAME, "item");
    MetadataResult<ComponentMetadataTypesDescriptor> metadataTypes =
        session.resolveComponentMetadata(operationElementDeclaration);
    assertThat(metadataTypes.isSuccess(), is(true));
    assertThat(metadataTypes.get().getOutputMetadata().isPresent(), is(true));
    assertThat(getTypeId(metadataTypes.get().getOutputMetadata().get()),
               is(of("org.mule.tooling.extensions.metadata.api.parameters.ItemOutput")));
  }

  @Test
  public void operationDynamicTypesSingleLevelKeyRequiredNotProvided() {
    OperationElementDeclaration operationElementDeclaration = configLessConnectionLessOPDeclaration(CONFIG_NAME);
    MetadataResult<ComponentMetadataTypesDescriptor> metadataTypes =
        session.resolveComponentMetadata(operationElementDeclaration);
    assertThat(metadataTypes.isSuccess(), is(false));
    assertThat(metadataTypes.getFailures(), hasSize(1));
    assertThat(metadataTypes.getFailures().get(0).getFailingComponent(), is(COMPONENT));
    assertThat(metadataTypes.getFailures().get(0).getFailureCode(), is(INVALID_METADATA_KEY));
    assertThat(metadataTypes.getFailures().get(0).getMessage(), containsString("Missing MetadataKey: metadataKey"));
  }

  @Test
  public void metadataKeyDefaultValueNotUsed() {
    OperationElementDeclaration operationElementDeclaration = configLessOPDeclaration(CONFIG_NAME);
    MetadataResult<ComponentMetadataTypesDescriptor> metadataTypes =
        session.resolveComponentMetadata(operationElementDeclaration);
    assertThat(metadataTypes.isSuccess(), is(false));
    assertThat(metadataTypes.getFailures(), hasSize(1));
    assertThat(metadataTypes.getFailures().get(0).getFailingComponent(), is(OUTPUT_PAYLOAD));
    assertThat(metadataTypes.getFailures().get(0).getFailureCode(), is(INVALID_METADATA_KEY));
    assertThat(metadataTypes.getFailures().get(0).getReason(), containsString("MetadataResolvingException: Unknown key:"));
  }

  @Test
  public void componentNotFoundOnDeclaration() {
    String invalidComponentName = "invalid";
    MetadataResult<ComponentMetadataTypesDescriptor> metadataTypes =
        session.resolveComponentMetadata(invalidComponentDeclaration(invalidComponentName));
    assertThat(metadataTypes.isSuccess(), is(false));
    assertThat(metadataTypes.getFailures(), IsCollectionWithSize.hasSize(1));
    assertThat(metadataTypes.getFailures().get(0).getFailureCode(), is(COMPONENT_NOT_FOUND));
    assertThat(metadataTypes.getFailures().get(0).getFailingComponent(), is(COMPONENT));
    assertThat(metadataTypes.getFailures().get(0).getMessage(),
               is(format("Could not find component: 'ToolingSupportTest:%s'", invalidComponentName)));
  }

  @Test
  public void extensionModelNotFound() {
    String invalidExtensionModel = "invalidExtensionName";
    MetadataResult<ComponentMetadataTypesDescriptor> metadataTypes =
        session.resolveComponentMetadata(invalidExtensionModel(invalidExtensionModel));
    assertThat(metadataTypes.isSuccess(), is(false));
    assertThat(metadataTypes.getFailures(), IsCollectionWithSize.hasSize(1));
    assertThat(metadataTypes.getFailures().get(0).getFailureCode(), is(COMPONENT_NOT_FOUND));
    assertThat(metadataTypes.getFailures().get(0).getFailingComponent(), is(COMPONENT));
    assertThat(metadataTypes.getFailures().get(0).getMessage(),
               is(format("ElementDeclaration is defined for extension: '%s' which is not part of the context: '[mule, ToolingSupportTest, module]'",
                         invalidExtensionModel)));
  }

  @Test
  public void configRefNotFound() {
    String missingConfigName = "missingConfigName";
    MetadataResult<ComponentMetadataTypesDescriptor> metadataTypes =
        session.resolveComponentMetadata(multiLevelShowInDslGroupOPDeclaration(missingConfigName, null, null));
    assertThat(metadataTypes.isSuccess(), is(false));
    assertThat(metadataTypes.getFailures(), IsCollectionWithSize.hasSize(1));
    assertThat(metadataTypes.getFailures().get(0).getFailureCode(), is(COMPONENT_NOT_FOUND));
    assertThat(metadataTypes.getFailures().get(0).getFailingComponent(), is(COMPONENT));
    assertThat(metadataTypes.getFailures().get(0).getMessage(),
               is(format("The resolver requires a configuration but the one referenced by the component declaration with name: '%s' is not present",
                         missingConfigName)));
  }

  @Test
  public void failOnOperationDoesNotHaveConfigButResolverRequiresConfiguration() {
    MetadataResult<ComponentMetadataTypesDescriptor> metadataTypes =
        session.resolveComponentMetadata(requiresConfigurationOutputTypeKeyResolverOP("someType"));
    assertThat(metadataTypes.isSuccess(), is(false));
    assertThat(metadataTypes.getFailures(), IsCollectionWithSize.hasSize(1));
    assertThat(metadataTypes.getFailures().get(0).getFailureCode(), is(UNKNOWN));
    assertThat(metadataTypes.getFailures().get(0).getFailingComponent(), is(OUTPUT_PAYLOAD));
    assertThat(metadataTypes.getFailures().get(0).getMessage(),
               is("Configuration is not present, a message from resolver"));
  }

}
