/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.type.catalog;

import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.STRING;
import static org.mule.runtime.core.api.type.catalog.SpecialTypesTypeLoader.VOID;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class ApplicationTypeLoaderTestCase {

  private ApplicationTypeLoader applicationTypeLoader;

  @Before
  public void setUp() {
    applicationTypeLoader = new ApplicationTypeLoader();
  }

  @Test
  public void hasPrimitiveTypeString() {
    assertThat(applicationTypeLoader.load(STRING).isPresent(), is(true));
  }

  @Test
  public void hasNotIncorrectType() {
    assertThat(applicationTypeLoader.load("incorrect").isPresent(), is(false));
  }

  @Test
  public void hasVoidType() {
    assertThat(applicationTypeLoader.load(VOID).isPresent(), is(true));
  }
}
