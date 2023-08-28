/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.typed.value;

import org.mule.runtime.api.metadata.MediaType;
import org.mule.test.typed.value.extension.extension.TypedValueExtension;
import org.mule.test.typed.value.extension.extension.TypedValueSource;

import org.junit.After;
import org.junit.Test;

public class TypedValueParameterOnConfigTestCase extends AbstractTypedValueTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"typed-value-on-config-config.xml"};
  }

  @After
  public void cleanUp() {
    TypedValueSource.onSuccessValue = null;
  }

  @Test
  public void typedValueOnDynamicConfig() throws Exception {
    TypedValueExtension extension =
        (TypedValueExtension) flowRunner("typedValueOnDynamicConfig").run().getMessage().getPayload().getValue();

    assertTypedValue(extension.getStringTypedValue(), "JsonStringElement", MediaType.ANY, null);
    assertTypedValue(extension.getDifferedDoor().getAddress(), "address", MediaType.ANY, null);
  }

  @Test
  public void typedValueOnStaticConfig() throws Exception {
    TypedValueExtension extension =
        (TypedValueExtension) flowRunner("typedValueOnStaticConfig").run().getMessage().getPayload().getValue();

    assertTypedValue(extension.getStringTypedValue(), "string", MediaType.ANY, null);
    assertTypedValue(extension.getDifferedDoor().getAddress(), "address", MediaType.ANY, null);
  }
}
