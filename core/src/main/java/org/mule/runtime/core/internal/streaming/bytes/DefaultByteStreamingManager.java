/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.core.internal.streaming.bytes.DefaultByteStreamingManager.Status.DISPOSABLE;
import static org.mule.runtime.core.internal.streaming.bytes.DefaultByteStreamingManager.Status.NORMAL;
import static org.mule.runtime.core.internal.streaming.bytes.DefaultByteStreamingManager.Status.SURVIVOR;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.streaming.CursorStream;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.streaming.bytes.ByteStreamingStatistics;
import org.mule.runtime.core.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.streaming.bytes.FileStoreCursorStreamConfig;
import org.mule.runtime.core.streaming.bytes.InMemoryCursorStreamConfig;
import org.mule.runtime.core.internal.streaming.bytes.factory.FileStoreCursorStreamProviderFactory;
import org.mule.runtime.core.internal.streaming.bytes.factory.InMemoryCursorStreamProviderFactory;
import org.mule.runtime.core.internal.streaming.bytes.factory.NullCursorStreamProviderFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

/**
 * Default implementation of {@link ByteStreamingManagerAdapter}
 *
 * @since 4.0
 */
public class DefaultByteStreamingManager implements ByteStreamingManagerAdapter, Disposable {

  private static final Logger LOGGER = getLogger(DefaultByteStreamingManager.class);

  private final LoadingCache<String, EventStreamingState> registry =
      CacheBuilder.newBuilder()
          .removalListener((RemovalNotification<String, EventStreamingState> notification) -> notification.getValue().dispose())
          .build(new CacheLoader<String, EventStreamingState>() {

            @Override
            public EventStreamingState load(String key) throws Exception {
              return new EventStreamingState();
            }
          });

  private final DefaultByteStreamingStatistics statistics = new DefaultByteStreamingStatistics();
  private final ByteBufferManager bufferFactory;
  private final Scheduler executorService;
  private final MuleContext muleContext;

  public DefaultByteStreamingManager(ByteBufferManager bufferFactory, Scheduler executorService, MuleContext muleContext) {
    this.bufferFactory = bufferFactory;
    this.executorService = executorService;
    this.muleContext = muleContext;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() {
    executorService.stop(muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CursorStreamProviderFactory getInMemoryCursorStreamProviderFactory(InMemoryCursorStreamConfig config) {
    return new InMemoryCursorStreamProviderFactory(this, config, bufferFactory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CursorStreamProviderFactory getFileStoreCursorStreamProviderFactory(FileStoreCursorStreamConfig config) {
    return new FileStoreCursorStreamProviderFactory(this, config, bufferFactory, executorService);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CursorStreamProviderFactory getNullCursorStreamProviderFactory() {
    return new NullCursorStreamProviderFactory(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CursorStreamProviderFactory getDefaultCursorStreamProviderFactory() {
    return new InMemoryCursorStreamProviderFactory(this, InMemoryCursorStreamConfig.getDefault(), bufferFactory);
  }

  @Override
  public ByteStreamingStatistics getByteStreamingStatistics() {
    return statistics;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onOpen(CursorStreamProviderAdapter provider) {
    registerEventContext(getRoot(provider.getCreatorEvent().getContext()));
    registry.getUnchecked(getEventId(provider)).addProvider(provider);
    statistics.incrementOpenProviders();
  }

  /*
   * Duplicate registration will occur if cursors are opened in multiple child flows or processing branches. This means terminate
   * will fire multiple times sequentially during completion of the parent EventContext. After the first terminate all other
   * invocation will literally be no-ops. This is preferred to introducing contention here given multiple thread may be opening
   * cursors concurrently.
   */
  private void registerEventContext(EventContext eventContext) {
    from(eventContext.getCompletionPublisher()).doFinally(signal -> terminated(eventContext)).subscribe();
  }

  private EventContext getRoot(EventContext eventContext) {
    return eventContext.getParentContext()
        .map(this::getRoot)
        .orElse(eventContext);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onOpen(CursorStreamAdapter cursor) {
    final CursorStreamProviderAdapter provider = cursor.getProvider();
    registry.getUnchecked(getEventId(provider)).addCursor(provider, cursor);
    statistics.incrementOpenCursors();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onClose(CursorStreamAdapter cursor) {
    final String eventId = getEventId(cursor.getProvider());
    EventStreamingState state = registry.getIfPresent(eventId);

    if (state != null && state.removeCursor(cursor.getProvider(), cursor) == DISPOSABLE) {
      state.dispose();
      registry.invalidate(eventId);
    }
  }

  private void terminated(EventContext rootContext) {
    if (!rootContext.isStreaming()) {
      return;
    }
    EventStreamingState state = registry.getIfPresent(rootContext.getId());
    if (state != null) {
      if (state.terminate() == DISPOSABLE) {
        state.dispose();
        registry.invalidate(rootContext.getId());
      }
    }
  }

  private String getEventId(CursorStreamProviderAdapter provider) {
    return getEventId(provider.getCreatorEvent().getContext());
  }

  private String getEventId(EventContext eventContext) {
    return eventContext.getParentContext()
        .map(this::getEventId)
        .orElse(eventContext.getId());
  }

  enum Status {
    NORMAL, SURVIVOR, DISPOSABLE
  }

  private class EventStreamingState {

    private Status status = NORMAL;
    private boolean disposed = false;

    private final LoadingCache<CursorStreamProviderAdapter, List<CursorStreamAdapter>> cursors = CacheBuilder.newBuilder()
        .build(new CacheLoader<CursorStreamProviderAdapter, List<CursorStreamAdapter>>() {

          @Override
          public List<CursorStreamAdapter> load(CursorStreamProviderAdapter key) throws Exception {
            return new LinkedList<>();
          }
        });

    private synchronized void addProvider(CursorStreamProviderAdapter adapter) {
      cursors.getUnchecked(adapter);
    }

    private Status terminate() {
      if (cursors.size() == 0) {
        status = DISPOSABLE;
      } else {
        boolean allCursorsClosed = true;
        for (Map.Entry<CursorStreamProviderAdapter, List<CursorStreamAdapter>> entry : cursors.asMap().entrySet()) {
          closeProvider(entry.getKey());
          final List<CursorStreamAdapter> cursors = entry.getValue();
          if (!cursors.isEmpty()) {
            allCursorsClosed = allCursorsClosed && cursors.stream().allMatch(CursorStream::isClosed);
          }
        }
        status = allCursorsClosed ? DISPOSABLE : SURVIVOR;
      }
      return status;
    }

    private void addCursor(CursorStreamProviderAdapter provider, CursorStreamAdapter cursor) {
      cursors.getUnchecked(provider).add(cursor);
    }

    private Status removeCursor(CursorStreamProviderAdapter provider, CursorStreamAdapter cursor) {
      List<CursorStreamAdapter> openCursors = cursors.getUnchecked(provider);
      if (openCursors.remove(cursor)) {
        statistics.decrementOpenCursors();
      }

      if (openCursors.isEmpty()) {
        if (provider.isClosed() || status == SURVIVOR) {
          dispose();
          status = DISPOSABLE;
          cursors.invalidate(provider);
        }
      }

      return status;
    }

    private void dispose() {
      if (disposed) {
        return;
      }

      cursors.asMap().forEach((provider, cursors) -> {
        try {
          closeProvider(provider);
          closeAll(cursors);
        } finally {
          provider.releaseResources();
        }
      });

      disposed = true;
    }

    private void closeAll(List<CursorStreamAdapter> cursors) {
      cursors.forEach(cursor -> {
        try {
          cursor.close();
          statistics.decrementOpenCursors();
        } catch (Exception e) {
          LOGGER.warn("Exception was found trying to close cursor. Execution will continue", e);
        }
      });
    }

    private void closeProvider(CursorStreamProviderAdapter provider) {
      if (!provider.isClosed()) {
        provider.close();
        statistics.decrementOpenProviders();
      }
    }
  }
}
