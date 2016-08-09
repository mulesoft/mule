/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transport;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.apache.commons.lang.SerializationUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

@SmallTest
public class NullPayloadTestCase extends AbstractMuleTestCase {

  @Test
  public void testUniqueDeserialization() {
    byte[] serialized = SerializationUtils.serialize(null);
    assertNotNull(serialized);

    Object deserialized = SerializationUtils.deserialize(serialized);
    assertThat(deserialized, is(nullValue()));
  }

}
