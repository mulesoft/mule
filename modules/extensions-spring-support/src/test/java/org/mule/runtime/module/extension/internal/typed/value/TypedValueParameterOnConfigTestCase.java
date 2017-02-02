/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.typed.value;

import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import org.junit.After;
import org.junit.Test;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;

import java.nio.charset.Charset;

public class TypedValueParameterOnConfigTestCase extends AbstractTypedValueTestCase {

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {TypedValueExtension.class};
  }

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

    assertTypedValue(extension.stringTypedValue, "string", APPLICATION_JSON, UTF8);
    assertTypedValue(extension.differedDoor.getAddress(), "address", MediaType.ANY, UTF8);
  }

  @Test
  public void typedValueOnStaticConfig() throws Exception {
    TypedValueExtension extension =
        (TypedValueExtension) flowRunner("typedValueOnStaticConfig").run().getMessage().getPayload().getValue();

    assertTypedValue(extension.stringTypedValue, "string", MediaType.ANY, null);
    assertTypedValue(extension.differedDoor.getAddress(), "address", MediaType.ANY, null);
  }
}
