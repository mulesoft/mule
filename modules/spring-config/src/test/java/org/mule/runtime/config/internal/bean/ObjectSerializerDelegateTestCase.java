/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.bean;

import static org.mule.test.allure.AllureConstants.ObjectSerializer.DEFAULT_OBJECT_SERIALIZER;
import static org.mule.test.allure.AllureConstants.SerializationFeature.SERIALIZATION;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.serialization.SerializationProtocol;
import org.mule.runtime.core.api.MuleContext;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SERIALIZATION)
@Story(DEFAULT_OBJECT_SERIALIZER)
public class ObjectSerializerDelegateTestCase extends AbstractMuleTestCase {

  @Test
  public void delegatesProperly() {
    final var objectSerializer = mock(ObjectSerializer.class);
    final var internalProtocol = mock(SerializationProtocol.class);
    when(objectSerializer.getInternalProtocol()).thenReturn(internalProtocol);
    final var externalProtocol = mock(SerializationProtocol.class);
    when(objectSerializer.getExternalProtocol()).thenReturn(externalProtocol);

    final var muleContext = mock(MuleContext.class);
    when(muleContext.getObjectSerializer()).thenReturn(objectSerializer);

    final var objectSerializerDelegate = new DefaultObjectSerializerDelegate();
    objectSerializerDelegate.setMuleContext(muleContext);
    assertThat(objectSerializerDelegate.getInternalProtocol(), sameInstance(internalProtocol));
    assertThat(objectSerializerDelegate.getExternalProtocol(), sameInstance(externalProtocol));
  }
}
