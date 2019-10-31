/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.metadata;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.tck.junit4.matcher.MetadataKeyMatcher.metadataKeyWithId;
import static org.mule.test.metadata.extension.MetadataConnection.CAR;
import static org.mule.test.metadata.extension.MetadataConnection.HOUSE;
import static org.mule.test.metadata.extension.MetadataConnection.PERSON;
import static org.mule.test.metadata.extension.resolver.TestInputOutputSourceResolverWithKeyResolver.STARTED_CONNECTION_PROVIDER_KEY_MASK;
import static org.mule.test.metadata.extension.resolver.TestInputOutputSourceResolverWithKeyResolver.STARTED_SOURCE_KEY_MASK;
import static org.mule.test.module.extension.metadata.MetadataExtensionFunctionalTestCase.ResolutionType.EXPLICIT_RESOLUTION;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
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
    assertThat(metadataKeysResult.isSuccess(), is(true));
    final Set<MetadataKey> metadataKeys = getKeysFromContainer(metadataKeysResult.get());
    assertThat(metadataKeys.size(), is(5));
    assertThat(metadataKeys, hasItems(metadataKeyWithId(PERSON), metadataKeyWithId(CAR),
                                      metadataKeyWithId(HOUSE), metadataKeyWithId(EXPECTED_STARTED_SOURCE_KEY_ID),
                                      metadataKeyWithId(EXPECTED_STARTED_CONNECTION_PROVIDER_KEY_ID)));
  }

  @Test
  public void getSourceDynamicOutputMetadata() throws Exception {
    final MetadataResult<ComponentMetadataDescriptor<SourceModel>> result = getComponentDynamicMetadata(PERSON_METADATA_KEY);
    assertThat(result.isSuccess(), is(true));
    ComponentMetadataDescriptor<SourceModel> componentMetadata = result.get();
    assertExpectedOutput(componentMetadata.getModel(), personType, typeLoader.load(StringAttributes.class));
    assertThat(componentMetadata.getMetadataAttributes().getKey().get(), is(PERSON_METADATA_KEY));
  }

  /**
   * Since the classloader for this tests is different from the one that actually initialize the components
   * the STARTED/STOPPED information is retrieved building a key with the source status in the correct environment.
   */
  @Test
  public void sourcesMustNotStartWhenResolvingMetadata() {
    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService.getMetadataKeys(location);
    final Set<MetadataKey> metadataKeys = getKeysFromContainer(metadataKeysResult.get());
    assertThat(metadataKeys, hasItems(metadataKeyWithId(EXPECTED_STARTED_SOURCE_KEY_ID)));
  }

  /**
   * Since the classloader for this tests is different from the one that actually initialize the components
   * the STARTED/STOPPED information is retrieved building a key with the source status in the correct environment.
   */
  @Test
  public void sourcesMustStartConnectionProvidersWhenResolvingMetadata() {
    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService.getMetadataKeys(location);
    final Set<MetadataKey> metadataKeys = getKeysFromContainer(metadataKeysResult.get());
    assertThat(metadataKeys, hasItems(metadataKeyWithId(EXPECTED_STARTED_CONNECTION_PROVIDER_KEY_ID)));
  }
}
