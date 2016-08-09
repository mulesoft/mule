/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_CLASSLOADER;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultDescribingContextTestCase extends AbstractMuleTestCase {

  private static final String KEY = "key";
  private static final String VALUE = "value";
  private ExtensionDeclarer descriptor;

  private DescribingContext context;

  @Before
  public void before() {
    descriptor = new ExtensionDeclarer();
    context = new DefaultDescribingContext(descriptor, getClass().getClassLoader());
    context.addParameter(KEY, VALUE);
  }

  @Test
  public void getDeclarationDescriptor() {
    assertThat(descriptor, is(sameInstance(context.getExtensionDeclarer())));
  }

  @Test
  public void getParameter() {
    assertThat(context.getParameter(KEY, String.class), is(sameInstance((Object) VALUE)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void wrongParameterType() {
    context.getParameter(KEY, Long.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullParameterKey() {
    context.addParameter(null, VALUE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullParameterValur() {
    context.addParameter(KEY, null);
  }

  @Test
  public void getClassLoader() {
    assertThat(context.getParameter(EXTENSION_CLASSLOADER, ClassLoader.class), is(sameInstance(getClass().getClassLoader())));
  }
}
