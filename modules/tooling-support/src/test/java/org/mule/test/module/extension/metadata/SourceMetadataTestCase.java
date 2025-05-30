/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.metadata;

import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.tck.junit4.matcher.MetadataKeyMatcher.metadataKeyWithId;
import static org.mule.tck.junit4.matcher.metadata.MetadataKeyResultSuccessMatcher.isSuccess;
import static org.mule.test.metadata.extension.MetadataConnection.CAR;
import static org.mule.test.metadata.extension.MetadataConnection.HOUSE;
import static org.mule.test.metadata.extension.MetadataConnection.PERSON;
import static org.mule.test.metadata.extension.resolver.TestInputOutputSourceResolverWithKeyResolver.STARTED_CONNECTION_PROVIDER_KEY_MASK;
import static org.mule.test.metadata.extension.resolver.TestInputOutputSourceResolverWithKeyResolver.STARTED_SOURCE_KEY_MASK;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.AMERICA;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.ARGENTINA;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.BUENOS_AIRES;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.EUROPE;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.LA_PLATA;
import static org.mule.test.module.extension.metadata.MetadataExtensionFunctionalTestCase.ResolutionType.EXPLICIT_RESOLUTION;

import static java.lang.String.format;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.OutputMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.tck.message.StringAttributes;
import org.mule.test.metadata.extension.MetadataConnectionProvider;
import org.mule.test.metadata.extension.MetadataSource;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

public class SourceMetadataTestCase extends MetadataExtensionFunctionalTestCase<SourceModel> {

  private static final String EXPECTED_STARTED_SOURCE_KEY_ID = format(STARTED_SOURCE_KEY_MASK, false);
  private static final String EXPECTED_STARTED_CONNECTION_PROVIDER_KEY_ID = format(STARTED_CONNECTION_PROVIDER_KEY_MASK, true);

  public SourceMetadataTestCase(ResolutionType resolutionType) {
    super(resolutionType);
    this.provider = resolutionType == EXPLICIT_RESOLUTION ? MetadataService::getSourceMetadata
        : (metadataService, componentId, key) -> metadataService.getSourceMetadata(componentId);
    this.location = builder().globalName(SOURCE_METADATA).addSourcePart().build();
  }

  @Before
  public void before() {
    MetadataConnectionProvider.STARTED = false;
    MetadataSource.STARTED = false;
  }

  @Override
  protected String getConfigFile() {
    return METADATA_TEST;
  }

  @Test
  public void getSourceMetadataKeys() {
    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService.getMetadataKeys(location);
    assertThat(metadataKeysResult, isSuccess());
    final Set<MetadataKey> metadataKeys = getKeysFromContainer(metadataKeysResult.get());
    assertThat(metadataKeys.size(), is(5));
    assertThat(metadataKeys, hasItems(metadataKeyWithId(PERSON), metadataKeyWithId(CAR),
                                      metadataKeyWithId(HOUSE), metadataKeyWithId(EXPECTED_STARTED_SOURCE_KEY_ID),
                                      metadataKeyWithId(EXPECTED_STARTED_CONNECTION_PROVIDER_KEY_ID)));
  }

  @Test
  public void getSourceMetadataKeysMultiLevelExplicitResolution() {
    Location location = builder().globalName(SOURCE_METADATA_WITH_PARTIAL_MULTILEVEL).addSourcePart().build();

    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService
        .getMetadataKeys(location, MetadataKeyBuilder.newKey(AMERICA).withPartName(CONTINENT).build());
    assertThat(metadataKeysResult, isSuccess());
    final Set<MetadataKey> continents = getKeysFromContainer(metadataKeysResult.get());
    assertThat(continents, hasSize(1));

    assertThat(continents, hasItem(metadataKeyWithId(AMERICA).withDisplayName(AMERICA).withPartName(CONTINENT)));
    assertThat(continents, not(hasItem(metadataKeyWithId(EUROPE).withDisplayName(EUROPE).withPartName(CONTINENT))));
  }

  @Test
  public void getSourceMetadataKeysMultiLevelShowInDsl() {
    Location location = builder().globalName(SOURCE_METADATA_WITH_PARTIAL_MULTILEVEL_SHOW_IN_DSL).addSourcePart().build();

    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService
        .getMetadataKeys(location);
    assertThat(metadataKeysResult, isSuccess());
    final Set<MetadataKey> continents = getKeysFromContainer(metadataKeysResult.get());
    assertThat(continents, hasSize(1));

    assertThat(continents, hasItem(metadataKeyWithId(AMERICA).withDisplayName(AMERICA).withPartName(CONTINENT)));
    assertThat(continents, not(hasItem(metadataKeyWithId(EUROPE).withDisplayName(EUROPE).withPartName(CONTINENT))));
  }

  @Test
  public void twoLevelPartialMultilevelSourceMetadataKeysExplicitResolution() throws Exception {
    Location location = builder().globalName(SOURCE_METADATA_WITH_PARTIAL_MULTILEVEL).addSourcePart().build();
    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService
        .getMetadataKeys(location, MetadataKeyBuilder
            .newKey(AMERICA)
            .withPartName(CONTINENT)
            .withChild(MetadataKeyBuilder.newKey(ARGENTINA)
                .withPartName(COUNTRY))
            .build());
    assertSuccessResult(metadataKeysResult);
    final Set<MetadataKey> continents = getKeysFromContainer(metadataKeysResult.get());
    assertThat(continents, hasSize(1));

    Set<MetadataKey> countries = continents.iterator().next().getChilds();
    assertThat(countries, hasSize(1));

    Set<MetadataKey> cities = countries.iterator().next().getChilds();
    assertThat(cities, hasSize(2));

    assertThat(cities, hasItem(metadataKeyWithId(BUENOS_AIRES).withPartName(CITY)));
    assertThat(cities, hasItem(metadataKeyWithId(LA_PLATA).withPartName(CITY)));
  }

  @Test
  public void getSourceDynamicOutputMetadata() throws Exception {
    final MetadataResult<ComponentMetadataDescriptor<SourceModel>> result = getComponentDynamicMetadata(PERSON_METADATA_KEY);
    assertThat(result, isSuccess());
    ComponentMetadataDescriptor<SourceModel> componentMetadata = result.get();
    assertExpectedOutput(componentMetadata.getModel(), personType, typeLoader.load(StringAttributes.class));
    assertThat(componentMetadata.getMetadataAttributes().getKey().get(), is(PERSON_METADATA_KEY));
  }

  @Test
  public void getSourceDynamicOutputMetadataExplicitResolution() throws Exception {
    MetadataResult<OutputMetadataDescriptor> outputMetadataResult =
        metadataService.getOutputMetadata(location, PERSON_METADATA_KEY);
    assertThat(outputMetadataResult.isSuccess(), is(true));
    OutputMetadataDescriptor outputMetadataDescriptor = outputMetadataResult.get();
    assertThat(outputMetadataDescriptor.getPayloadMetadata().isDynamic(), is(true));
    assertExpectedType(outputMetadataDescriptor.getPayloadMetadata().getType(), personType);
  }

  @Test
  public void getSourceDynamicOutputAttributesMetadataExplicitResolution() throws Exception {
    MetadataResult<OutputMetadataDescriptor> outputMetadataResult =
        metadataService.getOutputMetadata(location, PERSON_METADATA_KEY);
    assertThat(outputMetadataResult.isSuccess(), is(true));
    OutputMetadataDescriptor outputMetadataDescriptor = outputMetadataResult.get();
    assertExpectedType(outputMetadataDescriptor.getAttributesMetadata().getType(), types.get(StringAttributes.class.getName()));
  }

  @Test
  public void getSourceMultilevelDynamicOutputMetadataImplicitResolution() throws Exception {
    Location location = builder().globalName(SOURCE_METADATA_WITH_MULTILEVEL).addSourcePart().build();
    MetadataResult<ComponentMetadataDescriptor<SourceModel>> outputMetadataResult =
        metadataService.getSourceMetadata(location);
    assertThat(outputMetadataResult.isSuccess(), is(true));
    SourceModel sourceModel = outputMetadataResult.get().getModel();
    MetadataType outputType = sourceModel.getOutput().getType();
    assertThat(outputType, instanceOf(ObjectType.class));
    ObjectType outputObjectType = (ObjectType) outputType;
    assertThat(outputObjectType.getFieldByName(AMERICA).isPresent(), is(true));
    assertThat(outputObjectType.getFieldByName(ARGENTINA).isPresent(), is(true));
    assertThat(outputObjectType.getFieldByName(BUENOS_AIRES).isPresent(), is(true));
  }

  /**
   * Since the classloader for this tests is different from the one that actually initialize the components the STARTED/STOPPED
   * information is retrieved building a key with the source status in the correct environment.
   */
  @Test
  public void sourcesMustNotStartWhenResolvingMetadata() {
    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService.getMetadataKeys(location);
    final Set<MetadataKey> metadataKeys = getKeysFromContainer(metadataKeysResult.get());
    assertThat(metadataKeys, hasItems(metadataKeyWithId(EXPECTED_STARTED_SOURCE_KEY_ID)));
  }

  /**
   * Since the classloader for this tests is different from the one that actually initialize the components the STARTED/STOPPED
   * information is retrieved building a key with the source status in the correct environment.
   */
  @Test
  public void sourcesMustStartConnectionProvidersWhenResolvingMetadata() {
    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService.getMetadataKeys(location);
    final Set<MetadataKey> metadataKeys = getKeysFromContainer(metadataKeysResult.get());
    assertThat(metadataKeys, hasItems(metadataKeyWithId(EXPECTED_STARTED_CONNECTION_PROVIDER_KEY_ID)));
  }
}
