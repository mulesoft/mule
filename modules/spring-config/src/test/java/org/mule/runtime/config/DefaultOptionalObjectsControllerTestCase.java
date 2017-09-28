/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import org.mule.runtime.config.internal.DefaultOptionalObjectsController;
import org.mule.runtime.config.internal.OptionalObjectsController;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class DefaultOptionalObjectsControllerTestCase extends AbstractMuleTestCase {

  private static final String OPTIONAL_KEY = "optional";
  private static final String DISCARDED_KEY = "discarded";

  private OptionalObjectsController optionalObjectsController;

  @Before
  public void before() {
    optionalObjectsController = new DefaultOptionalObjectsController();
    optionalObjectsController.registerOptionalKey(OPTIONAL_KEY);
    optionalObjectsController.discardOptionalObject(DISCARDED_KEY);
  }

  @Test
  public void isOptional() {
    assertThat(optionalObjectsController.isOptional(OPTIONAL_KEY), is(true));
    assertThat(optionalObjectsController.isOptional(DISCARDED_KEY), is(false));
  }

  @Test
  public void isDiscarded() {
    assertThat(optionalObjectsController.isDiscarded(OPTIONAL_KEY), is(false));
    assertThat(optionalObjectsController.isDiscarded(DISCARDED_KEY), is(true));
  }

  @Test
  public void getAllDiscardedKeys() {
    Collection<String> allKeys = optionalObjectsController.getAllOptionalKeys();
    assertThat(allKeys, contains(OPTIONAL_KEY));
  }

  @Test
  public void getDiscardedObjectPlaceholder() {
    assertThat(optionalObjectsController.getDiscardedObjectPlaceholder(), is(not(nullValue())));
  }
}
