/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeThat;
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
import static org.mule.runtime.module.extension.internal.metadata.PartAwareMetadataKeyBuilder.newKey;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.AMERICA;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.SAN_FRANCISCO;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.USA;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.ComponentId;
import org.mule.runtime.api.metadata.ConfigurationId;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.ProcessorId;
import org.mule.runtime.api.metadata.SourceId;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.internal.metadata.InvalidComponentIdException;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

public class MetadataNegativeTestCase extends MetadataExtensionFunctionalTestCase {

  private static final String NOT_A_METADATA_PROVIDER = "is not a MetadataProvider or MetadataEntityProvider";
  private static final String PROCESSOR_DOES_NOT_EXIST = "Processor doesn't exist in the given index [%s]";
  private static final String SOURCE_DOES_NOT_EXIST = "Flow doesn't contain a message source";
  private static final String FLOW_DOES_NOT_EXIST = "Flow [%s] doesn't exist";
  private static final String FAIL_WITH_RESOLVING_EXCEPTION = "failWithResolvingException";
  private static final String FAIL_WITH_RUNTIME_EXCEPTION = "failWithRuntimeException";
  private static final String NON_EXISTING_FLOW = "nonExistingFlow";
  private static final String NON_EXISTING_CONFIG = "nonexisting-config";
  private static final String LOGGER_FLOW = "loggerFlow";
  private static final String FLOW_WITHOUT_SOURCE = "flowWithoutSource";
  private static final String CONFIGURATION_CANNOT_BE_DYNAMIC = "Configuration used for Metadata fetch cannot be dynamic";
  private static final String NO_DYNAMIC_KEY_AVAILABLE = "Component [%s] is not a MetadataKeyProvider";

  @Override
  protected String getConfigFile() {
    return METADATA_TEST;
  }

  @Test
  public void getOperationMetadataWithResolvingException() throws Exception {
    componentId = new ProcessorId(FAIL_WITH_RESOLVING_EXCEPTION, FIRST_PROCESSOR_INDEX);
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata = getComponentDynamicMetadata(PERSON_METADATA_KEY);
    assertThat(metadata.isSuccess(), is(false));
    assertFailureResult(metadata, 2);
    List<MetadataFailure> failures = metadata.getFailures();
    assertMetadataFailure(failures.get(0), "", CONNECTION_FAILURE, "", OUTPUT_ATTRIBUTES, "");
    assertMetadataFailure(failures.get(1), "", CONNECTION_FAILURE, "", INPUT, "content");
  }

  @Test
  public void getKeysWithRuntimeException() throws Exception {
    componentId = new ProcessorId(FAIL_WITH_RUNTIME_EXCEPTION, FIRST_PROCESSOR_INDEX);
    MetadataResult<MetadataKeysContainer> metadata = metadataService.getMetadataKeys(componentId);
    assertFailureResult(metadata, 1);
    assertMetadataFailure(metadata.getFailures().get(0), "", UNKNOWN, RuntimeException.class.getName(), KEYS);
  }

  @Test
  public void getOperationMetadataWithRuntimeException() throws Exception {
    componentId = new ProcessorId(FAIL_WITH_RUNTIME_EXCEPTION, FIRST_PROCESSOR_INDEX);
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadata = getComponentDynamicMetadata(PERSON_METADATA_KEY);
    assertFailureResult(metadata, 2);
    assertMetadataFailure(metadata.getFailures().get(0), "", UNKNOWN, "", OUTPUT_ATTRIBUTES);
    assertMetadataFailure(metadata.getFailures().get(1), "", UNKNOWN, "", INPUT, "content");
  }

  @Test
  public void flowDoesNotExist() throws Exception {
    componentId = new ProcessorId(NON_EXISTING_FLOW, FIRST_PROCESSOR_INDEX);
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = getComponentDynamicMetadata(PERSON_METADATA_KEY);
    assertFailureResult(result, 1);
    assertMetadataFailure(result.getFailures().get(0),
                          format(FLOW_DOES_NOT_EXIST, NON_EXISTING_FLOW),
                          COMPONENT_NOT_FOUND,
                          InvalidComponentIdException.class.getName(),
                          COMPONENT,
                          "");
  }

  @Test
  public void processorDoesNotExist() throws Exception {
    String notValidIndex = "10";
    componentId = new ProcessorId(CONTENT_AND_OUTPUT_METADATA_WITH_KEY_ID, notValidIndex);
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = getComponentDynamicMetadata(PERSON_METADATA_KEY);
    assertFailureResult(result, 1);
    assertMetadataFailure(result.getFailures().get(0),
                          format(PROCESSOR_DOES_NOT_EXIST, notValidIndex),
                          COMPONENT_NOT_FOUND,
                          InvalidComponentIdException.class.getName(),
                          COMPONENT);
  }

  @Test
  public void failToGetMetadataFromNonExistingSource() throws IOException {
    final SourceId notExistingSource = new SourceId(FLOW_WITHOUT_SOURCE);
    final MetadataResult<MetadataKeysContainer> result = metadataService.getMetadataKeys(notExistingSource);
    assertFailureResult(result, 1);
    assertMetadataFailure(result.getFailures().get(0),
                          SOURCE_DOES_NOT_EXIST,
                          COMPONENT_NOT_FOUND,
                          InvalidComponentIdException.class.getName(), COMPONENT);
  }

  @Test
  public void processorIsNotEntityMetadataProvider() throws Exception {
    componentId = new ProcessorId(LOGGER_FLOW, FIRST_PROCESSOR_INDEX);
    MetadataResult<TypeMetadataDescriptor> result = metadataService.getEntityMetadata(componentId, PERSON_METADATA_KEY);
    assertFailureResult(result, 1);
    assertMetadataFailure(result.getFailures().get(0),
                          NOT_A_METADATA_PROVIDER, NO_DYNAMIC_METADATA_AVAILABLE,
                          InvalidComponentIdException.class.getName(), COMPONENT);
  }

  @Test
  public void failToGetMetadataFromNonExistingConfig() throws IOException {
    final MetadataResult<MetadataKeysContainer> result =
        metadataService.getMetadataKeys(new ConfigurationId(NON_EXISTING_CONFIG));
    assertFailureResult(result, 1);
    assertMetadataFailure(result.getFailures().get(0),
                          format("Configuration named [%s] doesn't exist", NON_EXISTING_CONFIG),
                          COMPONENT_NOT_FOUND,
                          InvalidComponentIdException.class.getName(), COMPONENT);
  }

  @Test
  public void failToGetMetadataFromDynamicConfig() throws IOException {
    ComponentId componentId = new ConfigurationId("dynamic-config");
    final MetadataResult<MetadataKeysContainer> result =
        metadataService.getMetadataKeys(componentId);

    assertMetadataFailure(result.getFailures().get(0),
                          format(NO_DYNAMIC_KEY_AVAILABLE, componentId),
                          NO_DYNAMIC_METADATA_AVAILABLE,
                          InvalidComponentIdException.class.getName(), COMPONENT);
  }

  @Test
  public void processorIsNotMetadataProvider() throws Exception {
    componentId = new ProcessorId(LOGGER_FLOW, FIRST_PROCESSOR_INDEX);
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = getComponentDynamicMetadata(PERSON_METADATA_KEY);
    assertMetadataFailure(result.getFailures().get(0),
                          NOT_A_METADATA_PROVIDER,
                          NO_DYNAMIC_METADATA_AVAILABLE,
                          InvalidComponentIdException.class.getName(), COMPONENT);
  }

  @Test
  public void fetchMissingElementFromCache() throws Exception {
    componentId = new ProcessorId(CONTENT_ONLY_CACHE_RESOLVER, FIRST_PROCESSOR_INDEX);
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
    componentId = new ProcessorId(RESOLVER_WITH_DYNAMIC_CONFIG, FIRST_PROCESSOR_INDEX);
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
    componentId = new ProcessorId(INCOMPLETE_MULTILEVEL_KEY_RESOLVER, FIRST_PROCESSOR_INDEX);
    final MetadataKey metadataKey = newKey(AMERICA, CONTINENT).withChild(newKey(USA, COUNTRY)).build();
    final MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = getComponentDynamicMetadata(metadataKey);
    assertMetadataFailure(result.getFailures().get(0), "Missing levels: [city]", INVALID_METADATA_KEY, "", COMPONENT);
  }
}
