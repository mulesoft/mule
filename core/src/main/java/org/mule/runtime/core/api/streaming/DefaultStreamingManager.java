/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming;

import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_ENABLE_STREAMING_STATISTICS;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.instantiateClass;
import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;
import static org.mule.runtime.core.privileged.util.EventUtils.getRoot;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.bytes.ByteBufferManager;
import org.mule.runtime.core.api.streaming.bytes.ByteBufferManagerFactory;
import org.mule.runtime.core.api.streaming.bytes.ByteStreamingManager;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.api.streaming.object.CursorIteratorProviderFactory;
import org.mule.runtime.core.api.streaming.object.ObjectStreamingManager;
import org.mule.runtime.core.internal.event.DefaultEventContext;
import org.mule.runtime.core.internal.streaming.AtomicStreamingStatistics;
import org.mule.runtime.core.internal.streaming.CursorManager;
import org.mule.runtime.core.internal.streaming.ManagedCursorProvider;
import org.mule.runtime.core.internal.streaming.MutableStreamingStatistics;
import org.mule.runtime.core.internal.streaming.NullStreamingStatistics;
import org.mule.runtime.core.internal.streaming.StreamingGhostBuster;
import org.mule.runtime.core.internal.streaming.bytes.DefaultByteStreamingManager;
import org.mule.runtime.core.internal.streaming.bytes.factory.PoolingByteBufferManagerFactory;
import org.mule.runtime.core.internal.streaming.object.DefaultObjectStreamingManager;
import org.mule.runtime.core.internal.streaming.object.factory.NullCursorIteratorProviderFactory;
import org.mule.runtime.core.internal.util.CompositeClassLoader;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.io.Closeable;
import java.io.InputStream;

import javax.inject.Inject;

import org.slf4j.Logger;

@NoExtend
public class DefaultStreamingManager implements StreamingManager, Initialisable, Disposable {

  private static final Logger LOGGER = getLogger(DefaultStreamingManager.class);

  private static final String BUFFER_MANAGER_FACTORY_CLASS = getProperty(ByteBufferManagerFactory.class.getName(),
                                                                         PoolingByteBufferManagerFactory.class.getName());

  private ByteBufferManager bufferManager;
  private ByteStreamingManager byteStreamingManager;
  private ObjectStreamingManager objectStreamingManager;
  private CursorManager cursorManager;
  private MutableStreamingStatistics statistics;
  private boolean initialised = false;

  @Inject
  private MuleContext muleContext;

  @Inject
  private SchedulerService schedulerService;

  @Inject
  private StreamingGhostBuster ghostBuster;

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialise() throws InitialisationException {
    if (!initialised) {
      statistics = createStatistics();

      cursorManager = new CursorManager(statistics, ghostBuster);
      bufferManager = createByteBufferManager();
      byteStreamingManager = createByteStreamingManager();
      objectStreamingManager = createObjectStreamingManager();

      initialiseIfNeeded(byteStreamingManager, true, muleContext);
      initialiseIfNeeded(objectStreamingManager, true, muleContext);
      initialised = true;
    }
  }

  private ByteBufferManager createByteBufferManager() throws InitialisationException {
    CompositeClassLoader classLoader =
        new CompositeClassLoader(getClass().getClassLoader(), muleContext.getExecutionClassLoader());
    ByteBufferManagerFactory factory;
    try {
      factory = (ByteBufferManagerFactory) instantiateClass(BUFFER_MANAGER_FACTORY_CLASS, new Object[] {}, classLoader);
    } catch (Exception e) {
      throw new InitialisationException(createStaticMessage(format("Could not create %s of type %s",
                                                                   ByteBufferManagerFactory.class.getName(),
                                                                   BUFFER_MANAGER_FACTORY_CLASS)),
                                        e, this);
    }

    return factory.create();
  }

  private MutableStreamingStatistics createStatistics() {
    return parseBoolean(getProperty(MULE_ENABLE_STREAMING_STATISTICS))
        ? new AtomicStreamingStatistics()
        : new NullStreamingStatistics();
  }

  protected ByteStreamingManager createByteStreamingManager() {
    return new DefaultByteStreamingManager(bufferManager, this);
  }

  protected ObjectStreamingManager createObjectStreamingManager() {
    return new DefaultObjectStreamingManager(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() {
    disposeIfNeeded(byteStreamingManager, LOGGER);
    disposeIfNeeded(objectStreamingManager, LOGGER);
    disposeIfNeeded(bufferManager, LOGGER);
    disposeIfNeeded(cursorManager, LOGGER);

    initialised = false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ByteStreamingManager forBytes() {
    return byteStreamingManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ObjectStreamingManager forObjects() {
    return objectStreamingManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CursorProvider manage(CursorProvider provider, EventContext creatorEventContext) {
    if (provider instanceof ManagedCursorProvider) {
      return provider;
    }
    return cursorManager.manage(provider, (DefaultEventContext) creatorEventContext);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void manage(InputStream stream, EventContext creatorEventContext) {
    manage((Closeable) stream, creatorEventContext);
  }

  @Override
  public void manage(Closeable closeable, EventContext creatorEventContext) {
    if (closeable instanceof Cursor) {
      return;
    }

    ((BaseEventContext) creatorEventContext).onTerminated((response, throwable) -> closeQuietly(closeable));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CursorProvider manage(CursorProvider provider, CoreEvent creatorEvent) {
    return manage(provider, getRoot(creatorEvent.getContext()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void manage(InputStream stream, CoreEvent creatorEvent) {
    manage(stream, getRoot(creatorEvent.getContext()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Pair<CursorStreamProviderFactory, CursorIteratorProviderFactory> getPairFor(CursorProviderFactory provider) {
    CursorStreamProviderFactory cursorStreamProviderFactory;
    CursorIteratorProviderFactory cursorIteratorProviderFactory;

    if (provider instanceof CursorIteratorProviderFactory) {
      cursorIteratorProviderFactory = (CursorIteratorProviderFactory) provider;
      if (provider instanceof NullCursorIteratorProviderFactory
          && muleContext.getConfiguration().isInheritIterableRepeatability()) {
        cursorStreamProviderFactory = forBytes().getNullCursorProviderFactory();
      } else {
        cursorStreamProviderFactory = forBytes().getDefaultCursorProviderFactory();
      }
    } else if (provider instanceof CursorStreamProviderFactory) {
      cursorStreamProviderFactory = (CursorStreamProviderFactory) provider;
      cursorIteratorProviderFactory = forObjects().getDefaultCursorProviderFactory();
    } else {
      cursorStreamProviderFactory = forBytes().getDefaultCursorProviderFactory();
      cursorIteratorProviderFactory = forObjects().getDefaultCursorProviderFactory();
    }
    return new Pair<>(cursorStreamProviderFactory, cursorIteratorProviderFactory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StreamingStatistics getStreamingStatistics() {
    return statistics;
  }

  protected ByteBufferManager getBufferManager() {
    return bufferManager;
  }
}
