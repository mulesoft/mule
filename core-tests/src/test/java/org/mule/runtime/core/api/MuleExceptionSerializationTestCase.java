/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.exception.MuleException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.ObjectStreamClass;

import org.junit.Test;

@SmallTest
public class MuleExceptionSerializationTestCase extends AbstractMuleTestCase {

  @Test
  public void hasCorrectSerialVersionUID() {
    assertThat(ObjectStreamClass.lookup(MuleException.class).getSerialVersionUID(), is(4553533142751195715L));
  }

}
