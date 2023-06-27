/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
