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
import static org.mule.tck.junit4.matcher.MetadataKeyMatcher.metadataKeyWithId;
import static org.mule.test.metadata.extension.MetadataConnection.CAR;
import static org.mule.test.metadata.extension.MetadataConnection.HOUSE;
import static org.mule.test.metadata.extension.MetadataConnection.PERSON;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.SourceId;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.ParameterMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.message.StringAttributes;

import com.google.common.reflect.TypeToken;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

public class SourceMetadataTestCase extends MetadataExtensionFunctionalTestCase {

  private static final String TYPE_PARAMETER_NAME = "type";

  @Override
  protected String getConfigFile() {
    return METADATA_TEST;
  }

  @Before
  public void setUp() {
    componentId = new SourceId(SOURCE_METADATA);
  }

  @Test
  public void getSourceMetadataKeys() {
    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataManager.getMetadataKeys(componentId);
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
    final ComponentMetadataDescriptor componentMetadata = getComponentDynamicMetadata(PERSON_METADATA_KEY);
    assertExpectedOutput(componentMetadata.getOutputMetadata(), personType, personType);
  }

  @Test
  public void sourceDoesNotSupportDynamicContentMetadata() throws Exception {
    final ComponentMetadataDescriptor componentMetadata = getComponentDynamicMetadata(PERSON_METADATA_KEY);
    assertThat(componentMetadata.getContentMetadata().isPresent(), is(false));
  }

  @Test
  public void getSourceStaticOutputMetadata() throws IOException {
    final ComponentMetadataDescriptor componentMetadata = getComponentStaticMetadata();
    assertExpectedOutput(componentMetadata.getOutputMetadata(), new TypeToken<Map<String, Object>>() {}.getType(),
                         StringAttributes.class);
  }

  @Test
  public void getSourceParametersStaticMetadata() throws IOException {
    final ComponentMetadataDescriptor componentMetadata = getComponentStaticMetadata();
    final List<MetadataResult<ParameterMetadataDescriptor>> parametersMetadata = componentMetadata.getParametersMetadata();

    assertThat(parametersMetadata.size(), is(1));
    final MetadataResult<ParameterMetadataDescriptor> typeMetadataDescriptor = parametersMetadata.get(0);
    assertExpectedType(typeMetadataDescriptor, TYPE_PARAMETER_NAME, String.class);
  }

  @Test
  public void sourceDoesNotSupportStaticContentMetadata() {
    final ComponentMetadataDescriptor componentMetadata = getComponentStaticMetadata();
    assertThat(componentMetadata.getContentMetadata().isPresent(), is(false));
  }
}
