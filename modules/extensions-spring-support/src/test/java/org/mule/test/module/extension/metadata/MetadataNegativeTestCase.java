/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.metadata;

import static java.lang.Integer.valueOf;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.api.metadata.resolving.FailureCode.COMPONENT_NOT_FOUND;
import static org.mule.runtime.api.metadata.resolving.FailureCode.CONNECTION_FAILURE;
import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_CONFIGURATION;
import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_METADATA_KEY;
import static org.mule.runtime.api.metadata.resolving.FailureCode.NO_DYNAMIC_METADATA_AVAILABLE;
import static org.mule.runtime.api.metadata.resolving.FailureCode.RESOURCE_UNAVAILABLE;
import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;
import static org.mule.runtime.api.metadata.resolving.MetadataComponent.COMPONENT;
import static org.mule.runtime.api.metadata.resolving.MetadataComponent.INPUT;
import static org.mule.runtime.api.metadata.resolving.MetadataComponent.KEYS;
import static org.mule.runtime.api.metadata.resolving.MetadataComponent.OUTPUT_ATTRIBUTES;
import static org.mule.runtime.module.extension.api.metadata.MultilevelMetadataKeyBuilder.newKey;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.AMERICA;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.SAN_FRANCISCO;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.USA;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

public class MetadataNegativeTestCase extends AbstractMetadataOperationTestCase {

  private static final String NOT_A_METADATA_PROVIDER = "is not a MetadataProvider or MetadataEntityProvider";
  private static final String NO_OBJECT_FOUND = "No object found at location %s";
  private static final String FAIL_WITH_RESOLVING_EXCEPTION = "failWithResolvingException";
  private static final String FAIL_WITH_RUNTIME_EXCEPTION = "failWithRuntimeException";
  private static final String NON_EXISTING_FLOW = "nonExistingFlow";
  private static final String NON_EXISTING_CONFIG = "nonexisting-config";
  private static final String LOGGER_FLOW = "loggerFlow";
  private static final String FLOW_WITHOUT_SOURCE = "flowWithoutSource";
  private static final String CONFIGURATION_CANNOT_BE_DYNAMIC = "Configuration used for Metadata fetch cannot be dynamic";
  private static final String NO_DYNAMIC_KEY_AVAILABLE = "Component [%s] is not a MetadataKeyProvider";
  private static final String DYNAMIC_CONFIG = "dynamic-config";
  private static final String NO_SUCH_COMPONENT_MODEL_EXCEPTION_CLASS_NAME =
      "org.mule.runtime.config.internal.dsl.model.NoSuchComponentModelException";
  private static final String INVALID_COMPONENT_EXCEPTION_CLASSNAME =
      "org.mule.runtime.core.internal.metadata.InvalidComponentIdException";

  public MetadataNegativeTestCase(ResolutionType resolutionType) {
    super(resolutionType);
  }

  @Override
  protected String getConfigFile() {
    return METADATA_TEST;
  }

  @Test
  public void getOperationMetadataWithResolvingException() throws Exception {
    location = builder().globalName(FAIL_WITH_RESOLVING_EXCEPTION).addProcessorsPart().addIndexPart(0).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata = getComponentDynamicMetadata(PERSON_METADATA_KEY);
    assertThat(metadata.isSuccess(), is(false));
    assertFailureResult(metadata, 2);
    List<MetadataFailure> failures = metadata.getFailures();
    assertMetadataFailure(failures.get(0), "", CONNECTION_FAILURE, "", OUTPUT_ATTRIBUTES, "");
    assertMetadataFailure(failures.get(1), "", CONNECTION_FAILURE, "", INPUT, "content");
  }

  @Test
  public void getKeysWithRuntimeException() throws Exception {
    location = builder().globalName(FAIL_WITH_RUNTIME_EXCEPTION).addProcessorsPart().addIndexPart(0).build();
    MetadataResult<MetadataKeysContainer> metadata = metadataService.getMetadataKeys(location);
    assertFailureResult(metadata, 1);
    assertMetadataFailure(metadata.getFailures().get(0), "", UNKNOWN, RuntimeException.class.getName(), KEYS);
  }

  @Test
  public void getOperationMetadataWithRuntimeException() throws Exception {
    location = builder().globalName(FAIL_WITH_RUNTIME_EXCEPTION).addProcessorsPart().addIndexPart(0).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata = getComponentDynamicMetadata(PERSON_METADATA_KEY);
    assertFailureResult(metadata, 2);
    assertMetadataFailure(metadata.getFailures().get(0), "", UNKNOWN, "", OUTPUT_ATTRIBUTES);
    assertMetadataFailure(metadata.getFailures().get(1), "", UNKNOWN, "", INPUT, "content");
  }

  @Test
  public void flowDoesNotExist() throws Exception {
    location = builder().globalName(NON_EXISTING_FLOW).addProcessorsPart().addIndexPart(0).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = getComponentDynamicMetadata(PERSON_METADATA_KEY);
    assertFailureResult(result, 1);
    assertMetadataFailure(result.getFailures().get(0),
                          format(NO_OBJECT_FOUND, location.toString()),
                          COMPONENT_NOT_FOUND,
                          NO_SUCH_COMPONENT_MODEL_EXCEPTION_CLASS_NAME,
                          COMPONENT,
                          "");
  }

  @Test
  public void processorDoesNotExist() throws Exception {
    String notValidIndex = "10";
    location = builder().globalName(CONTENT_AND_OUTPUT_METADATA_WITH_KEY_ID).addProcessorsPart()
        .addIndexPart(valueOf(notValidIndex)).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = getComponentDynamicMetadata(PERSON_METADATA_KEY);
    assertFailureResult(result, 1);
    assertMetadataFailure(result.getFailures().get(0),
                          format(NO_OBJECT_FOUND, location),
                          COMPONENT_NOT_FOUND,
                          NO_SUCH_COMPONENT_MODEL_EXCEPTION_CLASS_NAME,
                          COMPONENT);
  }

  @Test
  public void failToGetMetadataFromNonExistingSource() throws IOException {
    location = builder().globalName(FLOW_WITHOUT_SOURCE).addSourcePart().build();
    final MetadataResult<MetadataKeysContainer> result = metadataService.getMetadataKeys(location);
    assertFailureResult(result, 1);
    assertMetadataFailure(result.getFailures().get(0),
                          format(NO_OBJECT_FOUND, location),
                          COMPONENT_NOT_FOUND,
                          NO_SUCH_COMPONENT_MODEL_EXCEPTION_CLASS_NAME, COMPONENT);
  }

  @Test
  public void processorIsNotEntityMetadataProvider() throws Exception {
    location = builder().globalName(LOGGER_FLOW).addProcessorsPart().addIndexPart(0).build();
    MetadataResult<TypeMetadataDescriptor> result = metadataService.getEntityMetadata(location, PERSON_METADATA_KEY);
    assertFailureResult(result, 1);
    assertMetadataFailure(result.getFailures().get(0),
                          NOT_A_METADATA_PROVIDER, NO_DYNAMIC_METADATA_AVAILABLE,
                          INVALID_COMPONENT_EXCEPTION_CLASSNAME, COMPONENT);
  }

  @Test
  public void failToGetMetadataFromNonExistingConfig() throws IOException {
    location = builder().globalName(NON_EXISTING_CONFIG).build();
    final MetadataResult<MetadataKeysContainer> result =
        metadataService.getMetadataKeys(location);
    assertFailureResult(result, 1);
    assertMetadataFailure(result.getFailures().get(0),
                          format(NO_OBJECT_FOUND, NON_EXISTING_CONFIG),
                          COMPONENT_NOT_FOUND,
                          NO_SUCH_COMPONENT_MODEL_EXCEPTION_CLASS_NAME, COMPONENT);
  }

  @Test
  public void failToGetMetadataFromDynamicConfig() throws IOException {
    location = builder().globalName(DYNAMIC_CONFIG).build();
    final MetadataResult<MetadataKeysContainer> result =
        metadataService.getMetadataKeys(location);

    assertMetadataFailure(result.getFailures().get(0),
                          format(NO_DYNAMIC_KEY_AVAILABLE, location),
                          NO_DYNAMIC_METADATA_AVAILABLE,
                          INVALID_COMPONENT_EXCEPTION_CLASSNAME, COMPONENT);
  }

  @Test
  public void processorIsNotMetadataProvider() throws Exception {
    location = builder().globalName(LOGGER_FLOW).addProcessorsPart().addIndexPart(0).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = getComponentDynamicMetadata(PERSON_METADATA_KEY);
    assertMetadataFailure(result.getFailures().get(0),
                          NOT_A_METADATA_PROVIDER,
                          NO_DYNAMIC_METADATA_AVAILABLE,
                          INVALID_COMPONENT_EXCEPTION_CLASSNAME, COMPONENT);
  }

  @Test
  public void fetchMissingElementFromCache() throws Exception {
    location = builder().globalName(CONTENT_ONLY_CACHE_RESOLVER).addProcessorsPart().addIndexPart(0).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = getComponentDynamicMetadata(NULL_METADATA_KEY);
    assertMetadataFailure(result.getFailures().get(0),
                          "",
                          RESOURCE_UNAVAILABLE,
                          "",
                          INPUT,
                          "content");
  }

  @Test
  public void failWithDynamicConfigurationWhenRetrievingMetadata() throws IOException {
    location = builder().globalName(RESOLVER_WITH_DYNAMIC_CONFIG).addProcessorsPart().addIndexPart(0).build();
    MetadataKey key = newKey(AMERICA, CONTINENT).withChild(newKey(USA, COUNTRY).withChild(newKey(SAN_FRANCISCO, CITY))).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = getComponentDynamicMetadata(key);
    assertFailureResult(result, 1);
    assertMetadataFailure(result.getFailures().get(0),
                          CONFIGURATION_CANNOT_BE_DYNAMIC,
                          INVALID_CONFIGURATION,
                          MetadataResolvingException.class.getName(),
                          COMPONENT);
  }

  @Test
  public void failToGetMetadataWithMissingMetadataKeyLevels() throws Exception {
    assumeThat(resolutionType, is(ResolutionType.EXPLICIT_RESOLUTION));
    location = builder().globalName(INCOMPLETE_MULTILEVEL_KEY_RESOLVER).addProcessorsPart().addIndexPart(0).build();
    final MetadataKey metadataKey = newKey(AMERICA, CONTINENT).withChild(newKey(USA, COUNTRY)).build();
    final MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = getComponentDynamicMetadata(metadataKey);
    assertMetadataFailure(result.getFailures().get(0), "Missing levels: [city]", INVALID_METADATA_KEY, "", COMPONENT);
  }

  @Test
  public void operationCantResolverVoidAsOutputTypeFromList() throws Exception {
    location = Location.builder().globalName("voidListAsOutput").addProcessorsPart().addIndexPart(0).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> operationMetadata =
        metadataService.getOperationMetadata(location);
    assertThat(operationMetadata.isSuccess(), is(false));
  }
}
