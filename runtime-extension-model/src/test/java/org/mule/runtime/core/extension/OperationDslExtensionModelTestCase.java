/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.extension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.getOperationDslExtensionModel;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.internal.extension.CustomBuildingDefinitionProviderModelProperty;

import org.junit.Test;

public class OperationDslExtensionModelTestCase {

  private ExtensionModel dslModel = getOperationDslExtensionModel();

  @Test
  public void assertUndefinedComponents() {
    assertThat(dslModel.getOperationModels(), hasSize(0));
    assertThat(dslModel.getConfigurationModels(), hasSize(0));
    assertThat(dslModel.getFunctionModels(), hasSize(0));
    assertThat(dslModel.getPrivilegedArtifacts(), hasSize(0));
    assertThat(dslModel.getImportedTypes(), hasSize(0));
    assertThat(dslModel.getSubTypes(), hasSize(0));

    // TODO: review this. Should be a construct?
    // assertThat(dslModel.getTypes(), hasSize(0));

    assertThat(dslModel.getPrivilegedArtifacts(), hasSize(0));
    assertThat(dslModel.getErrorModels(), hasSize(0));
    assertThat(dslModel.getSourceModels(), hasSize(0));
  }

  @Test
  public void assertHasCustomBuildingDefinitionProvider() {
    assertThat(dslModel.getModelProperty(CustomBuildingDefinitionProviderModelProperty.class).isPresent(), is(true));
  }

}
