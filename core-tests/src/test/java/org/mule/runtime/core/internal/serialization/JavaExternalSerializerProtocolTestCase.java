/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.serialization;

import static org.mule.test.allure.AllureConstants.SerializationFeature.SERIALIZATION;
import static org.mule.test.allure.AllureConstants.SerializationFeature.SerializationStory.MESSAGE_SERIALIZATION;

import org.mule.runtime.api.serialization.SerializationException;
import org.mule.tck.core.internal.serialization.AbstractSerializerProtocolContractTestCase;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SERIALIZATION)
@Story(MESSAGE_SERIALIZATION)
public class JavaExternalSerializerProtocolTestCase extends AbstractSerializerProtocolContractTestCase {

  @Before
  public void setUp() throws Exception {
    serializationProtocol = new JavaObjectSerializer(this.getClass().getClassLoader()).getExternalProtocol();
  }

  @Test(expected = SerializationException.class)
  public void notSerializable() throws Exception {
    serializationProtocol.serialize(new Object());
  }

}
