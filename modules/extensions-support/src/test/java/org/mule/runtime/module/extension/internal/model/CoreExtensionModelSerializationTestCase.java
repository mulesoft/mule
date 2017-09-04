/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;
import org.mule.runtime.extension.api.persistence.ExtensionModelJsonSerializer;

import org.junit.Test;

public class CoreExtensionModelSerializationTestCase {

  @Test
  public void serializeDeserialize() throws Exception {
    ExtensionModel extensionModel = MuleExtensionModelProvider.getExtensionModel();
    ExtensionModelJsonSerializer jsonSerializer = new ExtensionModelJsonSerializer(true);
    String serialized = jsonSerializer.serialize(extensionModel);
    ExtensionModel deserialized = jsonSerializer.deserialize(serialized);
    assertThat(extensionModel, equalTo(deserialized));
  }

}
