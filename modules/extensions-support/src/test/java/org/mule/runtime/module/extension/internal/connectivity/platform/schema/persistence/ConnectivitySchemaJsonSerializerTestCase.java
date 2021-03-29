/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connectivity.platform.schema.persistence;

import static org.mule.runtime.module.extension.internal.connectivity.platform.schema.ConnectivitySchemaTestUtils.getNetsuiteTokenAuthenticationSchema;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class ConnectivitySchemaJsonSerializerTestCase extends AbstractMuleTestCase {

  private ConnectivitySchemaJsonSerializer serializer = new ConnectivitySchemaJsonSerializer(true);

  @Test
  public void serialize() {
    String json = serializer.serialize(getNetsuiteTokenAuthenticationSchema());
    System.out.println(json);
  }
}
