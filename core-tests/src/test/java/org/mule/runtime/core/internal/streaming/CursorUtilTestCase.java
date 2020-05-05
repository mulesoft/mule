/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.internal.streaming.CursorUtils.unwrap;

import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class CursorUtilTestCase extends AbstractMuleTestCase {

  @Test
  public void unwrapNonDecoratedProvider() {
    CursorProvider provider = mock(CursorProvider.class);
    assertThat(unwrap(provider), is(sameInstance(provider)));
  }

  @Test
  public void unwrapOneLevelDecorator() {
    CursorProvider provider = mock(CursorProvider.class);
    CursorProviderDecorator decorator = mock(CursorProviderDecorator.class);
    when(decorator.getDelegate()).thenReturn(provider);

    assertThat(unwrap(decorator), is(sameInstance(provider)));
  }

  @Test
  public void unwrap10thLevelDecorator() {
    CursorProvider provider = mock(CursorProvider.class);
    CursorProviderDecorator decorator = mock(CursorProviderDecorator.class);
    when(decorator.getDelegate()).thenReturn(provider);

    for (int i = 0; i < 9; i++) {
      CursorProviderDecorator newDecorator = mock(CursorProviderDecorator.class);
      when(newDecorator.getDelegate()).thenReturn(decorator);
      decorator = newDecorator;
    }

    assertThat(unwrap(decorator), is(sameInstance(provider)));
  }
}
