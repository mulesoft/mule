/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.serialization;

import static org.mule.runtime.core.internal.context.DefaultMuleContext.currentMuleContext;
import static org.mule.test.allure.AllureConstants.SerializationFeature.SERIALIZATION;
import static org.mule.test.allure.AllureConstants.SerializationFeature.SerializationStory.MESSAGE_SERIALIZATION;

import org.mule.runtime.api.serialization.SerializationException;
import org.mule.tck.core.internal.serialization.AbstractSerializerProtocolContractTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SERIALIZATION)
@Story(MESSAGE_SERIALIZATION)
public class JavaExternalSerializerProtocolProtocolTestCase extends AbstractSerializerProtocolContractTestCase {

  @Before
  public void setUp() {
    currentMuleContext.set(muleContext);
  }

  @After
  public void teardown() {
    currentMuleContext.set(null);
  }

  @Override
  protected void doSetUp() throws Exception {
    serializationProtocol = muleContext.getObjectSerializer().getExternalProtocol();
  }

  @Test(expected = SerializationException.class)
  public void notSerializable() throws Exception {
    serializationProtocol.serialize(new Object());
  }

}
