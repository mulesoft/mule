/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metadata.internal.cache;

import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SDK_TOOLING_SUPPORT;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.MetadataTypeResolutionStory.METADATA_CACHE;

import static java.util.Optional.empty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.OutputModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.metadata.api.cache.ConfigurationMetadataCacheIdGenerator;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SDK_TOOLING_SUPPORT)
@Story(METADATA_CACHE)
public class ComponentParameterizationMetadataCacheIdGeneratorFactoryTestCase extends AbstractMuleTestCase {

  private ConfigurationMetadataCacheIdGenerator configurationMetadataCacheIdGenerator;
  private ComponentParameterizationBasedMetadataCacheIdGenerator metadataCacheIdGenerator;

  private OutputModel staticTypeOutputModel;
  private OperationModel operationModel;

  @Before
  public void setUp() {
    configurationMetadataCacheIdGenerator = mock(ConfigurationMetadataCacheIdGenerator.class);
    metadataCacheIdGenerator =
        new ComponentParameterizationBasedMetadataCacheIdGenerator(configurationMetadataCacheIdGenerator);

    staticTypeOutputModel = mock(OutputModel.class);
    when(staticTypeOutputModel.hasDynamicType()).thenReturn(false);
    when(staticTypeOutputModel.getType()).thenReturn(mock(MetadataType.class));

    operationModel = mock(OperationModel.class);
    when(operationModel.getName()).thenReturn("mockOperation");
  }

  @Test
  public void idForComponentMetadataEmptyForStaticMetadata() {
    when(operationModel.getOutput()).thenReturn(staticTypeOutputModel);
    when(operationModel.getOutputAttributes()).thenReturn(staticTypeOutputModel);

    assertThat(metadataCacheIdGenerator.getIdForComponentMetadata(ComponentParameterization.builder(operationModel)
        .build()),
               is(empty()));
  }

}
