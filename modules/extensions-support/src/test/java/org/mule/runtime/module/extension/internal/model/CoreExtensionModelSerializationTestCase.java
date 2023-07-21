/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider;
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
