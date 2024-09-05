/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.type;

import static org.mule.runtime.api.util.classloader.MuleImplementationLoaderUtils.isResolveMuleImplementationLoadersDynamically;

import static org.apache.commons.lang3.SerializationUtils.deserialize;
import static org.apache.commons.lang3.SerializationUtils.serialize;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mockStatic;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataType.DynamicDelegateDataType;
import org.mule.runtime.api.util.classloader.MuleImplementationLoaderUtils;
import org.mule.runtime.core.privileged.metadata.DefaultDataTypeBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;
import java.util.function.Supplier;

import io.qameta.allure.Issue;
import org.junit.Test;
import org.mockito.MockedStatic;

@SmallTest
@Issue("W-16584126")
public class DynamicDelegateDataTypeSerializationTestCase extends AbstractMuleTestCase {

  @Test
  public void serializeDynamicDelegateDataType() {
    DataType delegate = new DefaultDataTypeBuilder().build();
    DynamicDelegateDataType original = new DynamicDelegateDataType((Supplier<DataType> & Serializable) () -> delegate);
    byte[] serialized = serialize(original);
    DynamicDelegateDataType deserialized = deserialize(serialized);
    assertThat(deserialized, is(original));

    try (final MockedStatic<MuleImplementationLoaderUtils> mockedLoaderUtils = mockStatic(MuleImplementationLoaderUtils.class)) {
      mockedLoaderUtils.when(MuleImplementationLoaderUtils::isResolveMuleImplementationLoadersDynamically).thenReturn(true);
      mockedLoaderUtils.when(MuleImplementationLoaderUtils::getMuleImplementationsLoader)
          .thenReturn(this.getClass().getClassLoader());
      assertThat(isResolveMuleImplementationLoadersDynamically(), is(true));

      assertThat(deserialized.getDelegate(), is(original.getDelegate()));
    }
  }

}
