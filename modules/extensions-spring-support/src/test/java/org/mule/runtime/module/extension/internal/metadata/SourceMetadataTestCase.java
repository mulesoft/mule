/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.tck.junit4.matcher.MetadataKeyMatcher.metadataKeyWithId;
import static org.mule.test.metadata.extension.MetadataConnection.CAR;
import static org.mule.test.metadata.extension.MetadataConnection.HOUSE;
import static org.mule.test.metadata.extension.MetadataConnection.PERSON;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.tck.message.StringAttributes;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

public class SourceMetadataTestCase extends MetadataExtensionFunctionalTestCase {

  private static final MetadataComponentDescriptorProvider<SourceModel> explicitMetadataResolver =
      MetadataService::getSourceMetadata;


  @Override
  protected String getConfigFile() {
    return METADATA_TEST;
  }

  @Before
  public void setUp() {
    location = builder().globalName(SOURCE_METADATA).addPart("source").build();
    provider = explicitMetadataResolver;
  }

  @Test
  public void getSourceMetadataKeys() {
    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService.getMetadataKeys(location);
    assertThat(metadataKeysResult.isSuccess(), is(true));
    final Set<MetadataKey> metadataKeys = getKeysFromContainer(metadataKeysResult.get());
    assertThat(metadataKeys.size(), is(3));
    assertThat(metadataKeys, hasItems(metadataKeyWithId(PERSON), metadataKeyWithId(CAR), metadataKeyWithId(HOUSE)));
  }

  @Test
  public void injectComposedMetadataKeyIdInstanceInSource() throws Exception {
    ((Flow) getFlowConstruct(SOURCE_METADATA_WITH_MULTILEVEL)).start();
  }

  @Test
  public void injectSimpleMetadataKeyIdInstanceInSource() throws Exception {
    ((Flow) getFlowConstruct(SOURCE_METADATA)).start();
  }

  @Test
  public void getSourceDynamicOutputMetadata() throws Exception {
    final ComponentMetadataDescriptor<SourceModel> componentMetadata = getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);
    assertExpectedOutput(componentMetadata.getModel(), personType, typeLoader.load(StringAttributes.class));
  }
}
