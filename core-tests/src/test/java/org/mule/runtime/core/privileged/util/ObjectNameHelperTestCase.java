/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.util;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class ObjectNameHelperTestCase extends AbstractMuleContextTestCase {

  public static final String UNIQUE_NAME_PREFIX = "unique-name-prefix";

  @Test
  public void uniqueNameGeneration() throws Exception {
    final ObjectNameHelper objectNameHelper = new ObjectNameHelper(muleContext);
    final String uniqueName = objectNameHelper.getUniqueName(UNIQUE_NAME_PREFIX);
    assertThat(uniqueName, startsWith(UNIQUE_NAME_PREFIX));
    final String secondUniqueName = objectNameHelper.getUniqueName(UNIQUE_NAME_PREFIX);
    assertThat(secondUniqueName, startsWith(UNIQUE_NAME_PREFIX));
    assertThat(uniqueName, not(is(secondUniqueName)));
    final String nextName = UNIQUE_NAME_PREFIX + "-2";
    ((MuleContextWithRegistries) muleContext).getRegistry().registerObject(nextName, "");
    final String thirdUniqueName = objectNameHelper.getUniqueName(UNIQUE_NAME_PREFIX);
    assertThat(thirdUniqueName, not(is(nextName)));
  }

}
