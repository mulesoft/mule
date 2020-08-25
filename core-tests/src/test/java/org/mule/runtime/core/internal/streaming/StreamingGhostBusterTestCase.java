/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static java.lang.System.gc;
import static java.lang.Thread.sleep;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.internal.streaming.IdentifiableCursorProviderDecorator.of;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;

import io.qameta.allure.Feature;
import org.junit.Test;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.internal.streaming.bytes.ManagedCursorStreamProvider;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.lang.ref.WeakReference;

@Feature(STREAMING)
public class StreamingGhostBusterTestCase  extends AbstractMuleContextTestCase {

    private StreamingGhostBuster ghostBuster;

    @Override
    protected void doSetUp() throws Exception {
        ghostBuster = new StreamingGhostBuster();
        initialiseIfNeeded(ghostBuster, true, muleContext);
        startIfNeeded(ghostBuster);
    }

    @Override
    protected void doTearDown() throws MuleException {
        ghostBuster.stop();
        ghostBuster.dispose();
    }

    @Test
    public void releaseResourcesWhenReferenceIsCollected() {
        MutableStreamingStatistics statistics = mock(MutableStreamingStatistics.class);
        CursorStreamProvider provider = mock(CursorStreamProvider.class);
        ManagedCursorStreamProvider managedCursorProvider = new ManagedCursorStreamProvider(of(provider), statistics);

        WeakReference<ManagedCursorProvider> reference = ghostBuster.track(managedCursorProvider);

        // Force GC collection
        managedCursorProvider = null;
        gc();
        assertThat(reference.get(), is(nullValue()));

        try {
            sleep(1000);
        } catch (InterruptedException e) {
            // ignore
        }
        verify(provider).releaseResources();
    }

}
