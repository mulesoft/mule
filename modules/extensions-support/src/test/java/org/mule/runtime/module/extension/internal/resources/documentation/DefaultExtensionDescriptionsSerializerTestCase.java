/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.documentation;

import static org.mule.runtime.module.extension.api.resources.documentation.ExtensionDescriptionsSerializer.SERIALIZER;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.mule.runtime.module.extension.api.resources.documentation.ExtensionDescriptionSerializerException;

import org.junit.jupiter.api.Test;

public class DefaultExtensionDescriptionsSerializerTestCase {

  @Test
  void failingDeserialization() {
    assertThrows(ExtensionDescriptionSerializerException.class, () -> SERIALIZER.deserialize("not an xml"));
  }

  @Test
  void failingSeserialization() {
    assertThrows(ExtensionDescriptionSerializerException.class, () -> SERIALIZER.serialize(null));
  }
}
