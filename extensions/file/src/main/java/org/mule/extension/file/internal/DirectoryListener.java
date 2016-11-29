/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal;

import static com.sun.nio.file.SensitivityWatchEventModifier.HIGH;
import static java.lang.String.format;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.walkFileTree;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toSet;
import static org.mule.extension.file.api.FileEventType.CREATE;
import static org.mule.extension.file.api.FileEventType.DELETE;
import static org.mule.extension.file.api.FileEventType.UPDATE;
import static org.mule.extension.file.common.api.FileDisplayConstants.MATCH_WITH;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.concurrent.ThreadNameHelper.getPrefix;
import org.mule.extension.file.api.DeletedFileAttributes;
import org.mule.extension.file.api.FileEventType;
import org.mule.extension.file.api.ListenerFileAttributes;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FilePredicateBuilder;
import org.mule.extension.file.common.api.FileSystem;
import org.mule.extension.file.common.api.lock.NullPathLock;
import org.mule.extension.file.common.api.matcher.NullFilePayloadPredicate;
import org.mule.extension.file.internal.command.DirectoryListenerCommand;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.lifecycle.PrimaryNodeLifecycleNotificationListener;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for near real-time events that happens on files contained inside a directory or on the directory itself. The events are
 * not acquired by polling the filesystem but rather actually listening for operating system events.
 * <p>
 * Whenever a file (or the directory) is created, updated or deleted, this {@link Source} will fire a {@link Message} which
 * payload reference the affected file and the attributes will be a {@link ListenerFileAttributes} instance.
 * <p>
 * There're however some special cases to be considered:
 * <p>
 * <ul>
 * <li>If the file has been deleted, then it's not possible to obtain its content so the message payload will be {@code null}</li>
 * <li>Also in the case of file deletion, all its attributes are also not available. Thus, the message attributes will actually be
 * an instance of {@link DeletedFileAttributes} which throws {@link IllegalStateException} whenever a non available attribute is
 * requested</li>
 * <li>When the event references a directory, then the payload is also a {@code null}</li>
 * </ul>
 * <p>
 * <b>When to use it</b>
 * <p>
 * This source is useful in cases in which a flow should respond to changes done on the filesystem, examples being trigger files,
 * transaction files being added on a drop folder, settings files updated, etc. All of the above cases could be done using a
 * {@code <file:list>} operation inside a poll scope (maybe also combined with a {@code <watermark>}.
 * <p>
 * However, although polling is a powerful and reliable solution, it's not an efficient one. Because this listener relies on
 * operating system notifications, it's much more efficient in terms of resources.
 * <p>
 * <b>Reliability</b>
 * <p>
 * The trade-off between a poll reliability and this listener's performance is reliability. Because operating system events don't
 * generally include the concept of transaction or replay, there's no way to guarantee that no event is going to be lost in case
 * of failure or server crash. Although you can always use mule to implement a reliable acquisition pattern, there's no fallback
 * is such acquisition fails. What this means is that this listener is no silver bullet and it should not been seen as the
 * recommended approach over a poll+list+watermark approach. Users should analyse each use case and environment to determine which
 * the best option is.
 * <p>
 * <b>Operating system limitations</b>
 * <p>
 * This component's behaviour might be slightly different depending on the OS on which it is deployed. The main differences are
 * usually (but not exclusively) related to:
 * <ul>
 * <li>Overflows: In highly concurrent scenarios a given file might be associated to hundreds of events per second. Some OS might
 * not be able to handle that gracefully and decide to drop some of those events or even fail.</li>
 * <li>Polling: Some operation systems (like older versions of OSX) don't actually support file system notifications. In those
 * cases, the JRE decides to compensate by using a high frequency poll, in which case the listener becomes pretty much the same as
 * using a poll element</li>
 * </ul>
 *
 * @since 4.0
 */
@Alias(DirectoryListener.DIRECTORY_LISTENER)
public class DirectoryListener extends Source<InputStream, ListenerFileAttributes> implements FlowConstructAware {

  private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryListener.class);
  static final String DIRECTORY_LISTENER = "directory-listener";

  @UseConfig
  private FileConnector config;

  /**
   * The directory on which notifications are being listened to
   */
  @Parameter
  @Optional
  private String directory;

  /**
   * Whether to react to creation notifications. Defaults to true
   */
  @Parameter
  @Optional(defaultValue = "true")
  private boolean notifyOnCreate = true;

  /**
   * Whether to react to update notifications. Defaults to true
   */
  @Parameter
  @Optional(defaultValue = "true")
  private boolean notifyOnUpdate = true;

  /**
   * Whether to react to deletion notifications. Defaults to true
   */
  @Parameter
  @Optional(defaultValue = "false")
  private boolean notifyOnDelete = false;

  /**
   * Whether or not to also listen for notification which happen on sub directories which are also contained on the main one.
   * <p>
   * This option is set to {@code false} by default. Consider that when enabled, some operating systems might fire many
   * notifications when an event happens on a subdirectory. One per each watched directory on the notification's path.
   */
  @Parameter
  @Optional(defaultValue = "false")
  @Summary("Whether or not to also listen for notification which happen on sub directories which are also contained "
      + "on the main one.")
  private boolean recursive = false;

  /**
   * A matcher used to filter events on files which do not meet the matcher's criteria
   */
  @Parameter
  @Optional
  @Alias("matchWith")
  @DisplayName(MATCH_WITH)
  private FilePredicateBuilder<FilePredicateBuilder, FileAttributes> predicateBuilder;

  @Inject
  private MuleContext muleContext;

  @Inject
  private SchedulerService schedulerService;

  @Connection
  private FileSystem fileSystem;

  private FlowConstruct flowConstruct;
  private WatchService watcher;
  private Predicate<FileAttributes> matcher;
  private Set<FileEventType> enabledEventTypes = new HashSet<>();
  private Scheduler scheduler;
  private Scheduler listenerExecutor;
  private PrimaryNodeLifecycleNotificationListener clusterListener;

  private final Map<WatchKey, Path> keyPaths = new HashMap<>();
  private final AtomicBoolean stopRequested = new AtomicBoolean(false);
  private boolean started = false;

  private Future<?> submittedListenerTask;

  @Override
  public void onStart(SourceCallback<InputStream, ListenerFileAttributes> sourceCallback) throws MuleException {
    if (!muleContext.isPrimaryPollingInstance()) {
      LOGGER.debug("{} source on flow {} not started because this is a secondary cluster node", DIRECTORY_LISTENER,
                   flowConstruct.getName());
      initialiseClusterListener(sourceCallback);
      return;
    }

    calculateEnabledEventTypes();
    createWatcherService();

    matcher = predicateBuilder != null ? predicateBuilder.build() : new NullFilePayloadPredicate();
    // TODO MULE-11018 format("%s%s.file.listener", getPrefix(muleContext), flowConstruct.getName())
    scheduler = schedulerService.ioScheduler();

    started = true;
    stopRequested.set(false);

    listenerExecutor =
        schedulerService.customScheduler(format("%s%s.file.listener", getPrefix(muleContext), flowConstruct.getName()), 1);

    submittedListenerTask = listenerExecutor.submit(() -> listen(sourceCallback));
  }

  private synchronized void initialiseClusterListener(SourceCallback<InputStream, ListenerFileAttributes> sourceCallback) {
    if (clusterListener == null) {
      clusterListener = new PrimaryNodeLifecycleNotificationListener(() -> {
        try {
          onStart(sourceCallback);
        } catch (Exception e) {
          throw new MuleRuntimeException(e);
        }
      }, muleContext);

      clusterListener.register();
    }
  }

  private void listen(SourceCallback<InputStream, ListenerFileAttributes> sourceCallback) {
    try {
      for (;;) {
        if (isRequestedToStop()) {
          return;
        }

        WatchKey key;
        try {
          key = watcher.take();
        } catch (InterruptedException | ClosedWatchServiceException e) {
          return;
        }

        try {
          key.pollEvents().forEach(event -> processEvent(event, key, sourceCallback));
        } finally {
          resetWatchKey(key);
        }
      }
    } catch (Exception e) {
      sourceCallback.onSourceException(e);
    }
  }

  private void processEvent(WatchEvent<?> watchEvent, WatchKey key,
                            SourceCallback<InputStream, ListenerFileAttributes> sourceCallback) {
    WatchEvent<Path> event = (WatchEvent<Path>) watchEvent;

    Path watchPath = keyPaths.get(key);

    if (watchPath == null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Got an unregistered path for key %s. Event context was: ", key, event.context()));
      }

      return;
    }

    final Path path = watchPath.resolve(event.context()).toAbsolutePath();
    final Kind<?> kind = event.kind();

    if (kind == OVERFLOW) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(format("Too many changes occurred concurrently on file '%s'. Events might have been lost or discarded"));
      }
      return;
    }

    ListenerFileAttributes attributes = new ListenerFileAttributes(path, FileEventType.of(kind));
    if (!matcher.test(attributes)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER
            .debug(format("Detected a '%s' event on path '%s' but it will be skipped because it does not meet the matcher's criteria",
                          FileEventType.of(kind), path.toString()));
      }
      return;
    }

    if (isRequestedToStop()) {
      return;
    }

    sourceCallback.handle(createResult(path, attributes));
    createAdditionalWatchers(attributes);
  }

  private void createAdditionalWatchers(ListenerFileAttributes attributes) {
    if (recursive && attributes.getEventType().equals(FileEventType.CREATE.name()) && attributes.isDirectory()) {
      try {
        registerPath(Paths.get(attributes.getPath()));
      } catch (Exception e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(format("Directory '%s' was created but failed to place a new listener on it", attributes.getPath()), e);
        }
      }
    }
  }

  private boolean isRequestedToStop() {
    return stopRequested.get() || Thread.currentThread().isInterrupted();
  }

  private Result<InputStream, ListenerFileAttributes> createResult(Path path, ListenerFileAttributes attributes) {
    InputStream payload = null;
    MediaType mediaType = MediaType.ANY;

    if (attributes.getEventType().equals(DELETE.name())) {
      attributes = new DeletedFileAttributes(path);
    } else if (!attributes.isDirectory()) {
      mediaType = fileSystem.getFileMessageMediaType(mediaType, attributes);
      payload = new FileInputStream(path, new NullPathLock());
    }

    return Result.<InputStream, ListenerFileAttributes>builder()
        .output(payload)
        .mediaType(mediaType)
        .attributes(attributes).build();
  }

  @Override
  public void onStop() {
    submittedListenerTask.cancel(false);
    stopRequested.set(true);
    started = false;

    closeWatcherService();
    shutdownScheduler();
  }

  private void shutdownScheduler() {
    if (listenerExecutor != null) {
      listenerExecutor.stop(muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS);
    }
    if (scheduler != null) {
      scheduler.stop(muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS);
    }
  }

  private void closeWatcherService() {
    if (watcher == null) {
      return;
    }

    try {
      watcher.close();
    } catch (IOException e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("Found exception trying to close watcher service for directory listener on flow " + flowConstruct.getName(),
                    e);
      }
    }

    keyPaths.clear();
  }

  private void resetWatchKey(WatchKey key) {
    if (key.reset()) {
      return;
    }

    Path path = keyPaths.remove(key);
    if (path != null) {
      try {
        registerPath(path);
      } catch (IOException e) {
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn(format("Directory '%s' became unavailable and a new listener could not be established on it",
                             path.toString()));
        }
      }
    }
  }

  private void createWatcherService() throws MuleException {
    try {
      watcher = FileSystems.getDefault().newWatchService();
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create watcher service"), e);
    }

    final Path rootPath = resolveRootPath();

    try {
      registerPath(rootPath);

      if (recursive) {
        walkFileTree(rootPath, new SimpleFileVisitor<Path>() {

          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (!dir.equals(rootPath)) {
              registerPath(dir);
            }
            return CONTINUE;
          }
        });
      }
    } catch (IOException e) {
      throw new DefaultMuleException(e);
    }
  }

  private void registerPath(Path path) throws IOException {
    WatchKey key = path.register(watcher, getEnabledEventKinds(), HIGH);
    keyPaths.put(key, path);
  }

  private Path resolveRootPath() {
    return new DirectoryListenerCommand((LocalFileSystem) fileSystem).resolveRootPath(directory);
  }

  private void calculateEnabledEventTypes() throws ConfigurationException {
    ImmutableSet.Builder<FileEventType> types = ImmutableSet.builder();
    addEventType(types, notifyOnCreate, () -> CREATE);
    addEventType(types, notifyOnUpdate, () -> UPDATE);
    addEventType(types, notifyOnDelete, () -> DELETE);

    enabledEventTypes = types.build();

    if (enabledEventTypes.isEmpty()) {
      throw new ConfigurationException(createStaticMessage(format("File listener in flow '%s' has disabled all notification types. At least one should be enabled",
                                                                  flowConstruct.getName())));
    }
  }

  private Kind<?>[] getEnabledEventKinds() {
    Set<Kind> kindSet = enabledEventTypes.stream().map(FileEventType::asEventKind).collect(toSet());
    Kind[] kinds = new Kind[kindSet.size()];
    return kindSet.toArray(kinds);
  }

  private void addEventType(ImmutableSet.Builder<FileEventType> types, boolean condition, Supplier<FileEventType> supplier) {
    if (condition) {
      types.add(supplier.get());
    }
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }

  /**
   * @return whether {@code this} source is actually started
   */
  public boolean isStarted() {
    return started;
  }
}
