/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.streaming.DefaultStreamingManager;
import org.mule.runtime.core.api.streaming.NullCursorProviderFactory;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.api.streaming.object.CursorIteratorProviderFactory;
import org.mule.runtime.core.internal.streaming.bytes.factory.NullCursorStreamProviderFactory;
import org.mule.runtime.core.internal.streaming.object.factory.NullCursorIteratorProviderFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class DefaultStreamingManagerTestCase extends AbstractMuleContextTestCase {

  private DefaultStreamingManager streamingManager;

  @Before
  public void before() {
    streamingManager = (DefaultStreamingManager) muleContext.getStreamingManager();
  }

  @Test
  public void getPairForCursorStreamProviderFactory() {
    CursorStreamProviderFactory provider = mock(CursorStreamProviderFactory.class);
    Pair<CursorStreamProviderFactory, CursorIteratorProviderFactory> pair = streamingManager.getPairFor(provider);
    assertThat(pair.getFirst(), is(sameInstance(provider)));
  }

  @Test
  public void getPairForCursorIteratorProviderFactory() {
    CursorIteratorProviderFactory provider = mock(CursorIteratorProviderFactory.class);
    Pair<CursorStreamProviderFactory, CursorIteratorProviderFactory> pair = streamingManager.getPairFor(provider);
    assertThat(pair.getSecond(), is(sameInstance(provider)));
  }

  @Test
  public void getPairForNullCursorProviderFactory() {
    Pair<CursorStreamProviderFactory, CursorIteratorProviderFactory> pair =
        streamingManager.getPairFor(new NullCursorProviderFactory());
    assertThat(pair.getFirst(), is(instanceOf(NullCursorStreamProviderFactory.class)));
    assertThat(pair.getSecond(), is(instanceOf(NullCursorIteratorProviderFactory.class)));
  }
}
