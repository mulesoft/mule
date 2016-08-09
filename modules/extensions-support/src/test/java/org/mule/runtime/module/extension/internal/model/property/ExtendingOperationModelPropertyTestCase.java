/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.model.property;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import org.mule.runtime.extension.api.annotation.Extensible;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class ExtendingOperationModelPropertyTestCase extends AbstractMuleTestCase {

  @Test
  public void extensible() {
    ExtendingOperationModelProperty<TestExtensibleType> modelProperty =
        new ExtendingOperationModelProperty(TestExtensibleType.class);
    assertThat(modelProperty.getType(), is(sameInstance(TestExtensibleType.class)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void notExtensible() {
    new ExtendingOperationModelProperty(Object.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullImplementation() {
    new ExtendingOperationModelProperty(null);
  }

  @Extensible
  private class TestExtensibleType {

  }
}
