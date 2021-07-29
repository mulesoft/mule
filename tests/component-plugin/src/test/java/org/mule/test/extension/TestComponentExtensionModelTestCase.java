/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import org.mule.functional.internal.extension.TestComponentRuntimeExtensionModelProvider;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class TestComponentExtensionModelTestCase extends AbstractMuleTestCase {

  private ExtensionModel extensionModel = new TestComponentRuntimeExtensionModelProvider().createExtensionModel();

  @Test
  public void testExtensionModel() {
    assertThat(extensionModel.getName(), equalTo("Test Component Plugin"));
    assertThat(extensionModel.getConfigurationModels(), hasSize(1));
    assertThat(extensionModel.getConfigurationModels().get(0).getSourceModels(), hasSize(1));
    assertThat(extensionModel.getConstructModels(), hasSize(3));
    assertThat(extensionModel.getOperationModels(), hasSize(14));
  }
}
