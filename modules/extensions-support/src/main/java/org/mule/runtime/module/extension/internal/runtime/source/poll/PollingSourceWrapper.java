/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source.poll;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.notification.AbstractServerNotification.NO_ACTION_ID;
import static org.mule.runtime.api.notification.PollingSourceItemNotification.ITEM_DISPATCHED;
import static org.mule.runtime.api.notification.PollingSourceItemNotification.ITEM_REJECTED_IDEMPOTENCY;
import static org.mule.runtime.api.notification.PollingSourceItemNotification.ITEM_REJECTED_SOURCE_STOPPING;
import static org.mule.runtime.api.notification.PollingSourceItemNotification.ITEM_REJECTED_WATERMARK;
import static org.mule.runtime.api.store.ObjectStoreSettings.unmanagedPersistent;
import static org.mule.runtime.api.store.ObjectStoreSettings.unmanagedTransient;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.internal.util.ConcurrencyUtils.safeUnlock;
import static org.mule.runtime.module.extension.internal.runtime.source.poll.WatermarkStatus.ON_HIGH;
import static org.mule.runtime.module.extension.internal.runtime.source.poll.WatermarkStatus.ON_NEW_HIGH;
import static org.mule.runtime.module.extension.internal.runtime.source.poll.WatermarkStatus.PASSED;
import static org.mule.runtime.module.extension.internal.runtime.source.poll.WatermarkStatus.REJECT;
import static org.mule.sdk.api.runtime.source.PollContext.PollItemStatus.ACCEPTED;
import static org.mule.sdk.api.runtime.source.PollContext.PollItemStatus.ALREADY_IN_PROCESS;
import static org.mule.sdk.api.runtime.source.PollContext.PollItemStatus.FILTERED_BY_WATERMARK;
import static org.mule.sdk.api.runtime.source.PollContext.PollItemStatus.SOURCE_STOPPING;
import static org.mule.sdk.api.runtime.source.PollingSource.IDS_ON_UPDATED_WATERMARK_OS_NAME_SUFFIX;
import static org.mule.sdk.api.runtime.source.PollingSource.OS_NAME_MASK;
import static org.mule.sdk.api.runtime.source.PollingSource.RECENTLY_PROCESSED_IDS_OS_NAME_SUFFIX;
import static org.mule.sdk.api.runtime.source.PollingSource.UPDATED_WATERMARK_ITEM_OS_KEY;
import static org.mule.sdk.api.runtime.source.PollingSource.WATERMARK_ITEM_OS_KEY;
import static org.mule.sdk.api.runtime.source.PollingSource.WATERMARK_OS_NAME_SUFFIX;

import static java.lang.String.format;
import static java.util.Comparator.naturalOrder;
import static java.util.Optional.ofNullable;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.scheduler.SchedulingStrategy;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.module.extension.internal.runtime.source.SourceCallbackContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.source.SourceWrapper;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.source.PollContext;
import org.mule.sdk.api.runtime.source.PollContext.PollItem;
import org.mule.sdk.api.runtime.source.PollingSource;
import org.mule.sdk.api.runtime.source.Source;
import org.mule.sdk.api.runtime.source.SourceCallback;
import org.mule.sdk.api.runtime.source.SourceCallbackContext;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.slf4j.Logger;

/**
 * A {@link SourceWrapper} implementation that provides Polling related capabilities to any wrapped {@link Source}, like scheduled
 * polling, watermarking and idempotent processing.
 *
 * @param <T>
 * @param <A>
 *
 * @since 4.1
 */
public class PollingSourceWrapper<T, A> extends SourceWrapper<T, A> implements Restartable {

  public static final String ACCEPTED_POLL_ITEM_INFORMATION = "mule-polling-source-accepted-poll-item-information";

  public static final String REJECTED_ITEM_MESSAGE = "Item with id:[{}] is rejected with status:[{}]";
  public static final String ACCEPTED_ITEM_MESSAGE = "Item with id:[{}] is accepted";
  public static final String WATERMARK_SAVED_MESSAGE =
      "Watermark with key:[{}] and value:[{}] saved to the ObjectStore for flow:[{}]";
  public static final String WATERMARK_RETURNED_MESSAGE =
      "Watermark with key:[{}] and value:[{}] returned from the ObjectStore for flow:[{}]";
  public static final String WATERMARK_NOT_RETURNED_MESSAGE =
      "Watermark with key:[{}] not found on the ObjectStore for flow:[{}]";
  public static final String WATERMARK_REMOVED_MESSAGE = "Watermark with key:[{}] removed from the ObjectStore for flow:[{}]";
  public static final String WATERMARK_COMPARISON_MESSAGE =
      "Watermark comparison of {}:[{}] with {}:[{}] for flow:[{}] returns:[{}]";

  private static final Logger LOGGER = getLogger(PollingSourceWrapper.class);
  private static final String ITEM_RELEASER_CTX_VAR = "itemReleaser";
  private static final String UPDATE_PROCESSED_LOCK = "OSClearing";
  private static final String INFLIGHT_IDS_OS_NAME_SUFFIX = "inflight-ids";

  private final PollingSource<T, A> delegate;
  private final SchedulingStrategy scheduler;
  private final int maxItemsPerPoll;
  private final SystemExceptionHandler systemExceptionHandler;

  @Inject
  private LockFactory lockFactory;

  @Inject
  @Named(OBJECT_STORE_MANAGER)
  private ObjectStoreManager objectStoreManager;

  @Inject
  private SchedulerService schedulerService;

  @Inject
  private NotificationDispatcher notificationDispatcher;

  private ObjectStore<Serializable> watermarkObjectStore;
  private ObjectStore<Serializable> inflightIdsObjectStore;
  private ObjectStore<Serializable> recentlyProcessedIds;
  private ObjectStore<Serializable> idsOnUpdatedWatermark;

  private ComponentLocation componentLocation;
  private String flowName;
  private final AtomicBoolean stopRequested = new AtomicBoolean(false);
  private org.mule.runtime.api.scheduler.Scheduler executor;
  private AtomicBoolean restarting = new AtomicBoolean(false);
  private DelegateRunnable delegateRunnable;

  public PollingSourceWrapper(PollingSource<T, A> delegate, SchedulingStrategy scheduler, int maxItemsPerPoll,
                              SystemExceptionHandler systemExceptionHandler) {
    super(delegate);
    this.delegate = delegate;
    this.scheduler = scheduler;
    this.maxItemsPerPoll = maxItemsPerPoll;
    this.systemExceptionHandler = systemExceptionHandler;
  }

  @Override
  public void onStart(SourceCallback<T, A> sourceCallback) throws MuleException {
    delegate.onStart(sourceCallback);
    flowName = componentLocation.getRootContainerName();
    inflightIdsObjectStore = objectStoreManager.getOrCreateObjectStore(formatKey(INFLIGHT_IDS_OS_NAME_SUFFIX),
                                                                       unmanagedTransient());

    recentlyProcessedIds = objectStoreManager.getOrCreateObjectStore(formatKey(RECENTLY_PROCESSED_IDS_OS_NAME_SUFFIX),
                                                                     unmanagedPersistent());

    idsOnUpdatedWatermark = objectStoreManager.getOrCreateObjectStore(formatKey(IDS_ON_UPDATED_WATERMARK_OS_NAME_SUFFIX),
                                                                      unmanagedPersistent());

    watermarkObjectStore = objectStoreManager.getOrCreateObjectStore(formatKey(WATERMARK_OS_NAME_SUFFIX),
                                                                     unmanagedPersistent());

    stopRequested.set(false);
    if (restarting.compareAndSet(true, false)) {
      poll(sourceCallback);
      delegateRunnable.setDelegate(() -> poll(sourceCallback));
    } else {
      executor = schedulerService.customScheduler(SchedulerConfig.config()
          .withMaxConcurrentTasks(1)
          .withWaitAllowed(true)
          .withName(formatKey("executor")));
      delegateRunnable = new DelegateRunnable(() -> poll(sourceCallback));
      scheduler.schedule(executor, delegateRunnable);
    }
  }

  private String formatKey(String key) {
    return format(OS_NAME_MASK, flowName, key);
  }

  @Override
  public void onStop() {
    stopRequested.set(true);
    if (!restarting.get()) {
      shutdownScheduler();
      delegateRunnable = null;
    }
    try {
      delegate.onStop();
    } catch (Throwable t) {
      LOGGER.error(format("Found error while stopping source at location '%s'. %s", flowName, t.getMessage()), t);
    }
  }

  @Override
  public void onTerminate(CoreEvent event, Map<String, Object> parameters, SourceCallbackContext context,
                          CompletableCallback<Void> callback) {
    releaseOnCallback(context, callback);
  }

  @Override
  public void onBackPressure(CoreEvent event,
                             Map<String, Object> parameters,
                             SourceCallbackContext context,
                             CompletableCallback<Void> callback) {
    releaseOnCallback(context, callback);
  }

  private void releaseOnCallback(SourceCallbackContext context, CompletableCallback<Void> callback) {
    release(context);
    callback.complete(null);
  }

  private void poll(SourceCallback<T, A> sourceCallback) {
    if (isRequestedToStop()) {
      return;
    }

    withWatermarkLock(() -> {
      DefaultPollContext pollContext = new DefaultPollContext(sourceCallback, getCurrentWatermark(), getUpdatedWatermark());

      try {
        delegate.poll(pollContext);
      } catch (RuntimeException e) {
        LOGGER.error(format("Found exception trying to process item on source at flow '%s'. %s",
                            flowName, e.getMessage()),
                     e);
        systemExceptionHandler.handleException(e, componentLocation);
        return;
      }

      try {
        if (!isRequestedToStop()) {
          pollContext.getUpdatedWatermark()
              .ifPresent(w -> updateWatermark(w, pollContext.getWatermarkComparator(),
                                              pollContext.getMinimumRejectedByLimitPassingWatermark().orElse(null)));
        }
      } catch (Throwable t) {
        LOGGER.error(format("Found exception trying to process item on source at flow '%s'. %s",
                            flowName, t.getMessage()),
                     t);
      }
    });
  }

  private int compareWatermarks(String w1Label, Serializable w1, String w2Label, Serializable w2, Comparator comparator)
      throws IllegalArgumentException {
    if (comparator == null) {
      if (w1 instanceof Serializable && w2 instanceof Serializable) {
        comparator = naturalOrder();
      } else {
        throw new IllegalStateException(format("Non comparable watermark values [%s, %s] were provided on source at flow '%s'. "
            + "Use comparable values or set a custom comparator. Watermark not updated.",
                                               w1, w2, flowName));

      }
    }
    int result = comparator.compare(w1, w2);
    LOGGER.trace(WATERMARK_COMPARISON_MESSAGE, w1Label, w1, w2Label, w2, flowName, result);
    return result;
  }

  @Override
  public RestartContext beginRestart() {
    restarting.set(true);
    delegateRunnable.setDelegate(null);
    return new RestartContext(executor, delegateRunnable);
  }

  @Override
  public void finishRestart(RestartContext restartContext) {
    restarting.set(true);

    executor = restartContext.getExecutor();
    delegateRunnable = restartContext.getDelegateRunnable();
  }

  private class DefaultPollContext implements PollContext<T, A> {

    private final SourceCallback<T, A> sourceCallback;
    private Serializable currentWatermark;
    private Serializable updatedWatermark;
    private Serializable minimumRejectedByLimitPassingWatermark;
    private Comparator<Serializable> watermarkComparator = null;
    private ZonedDateTime timestamp;

    private int currentPollItems;

    private DefaultPollContext(SourceCallback<T, A> sourceCallback, Serializable currentWatermark,
                               Serializable updatedWatermark) {
      this.sourceCallback = sourceCallback;
      this.currentWatermark = currentWatermark;
      this.updatedWatermark = updatedWatermark;
      this.currentPollItems = 0;
      this.minimumRejectedByLimitPassingWatermark = null;
      this.timestamp = ZonedDateTime.now();
    }

    public String getPollId() {
      return componentLocation.getRootContainerName() + " @ " + timestamp;
    }

    @Override
    public PollItemStatus accept(Consumer<PollItem<T, A>> consumer) {
      final SourceCallbackContext callbackContext = sourceCallback.createContext();
      DefaultPollItem pollItem = new DefaultPollItem(callbackContext);
      consumer.accept(pollItem);
      pollItem.validate();
      String itemId = getItemId(pollItem);

      PollItemStatus status = ACCEPTED;
      boolean currentPollItemLimitApplied = false;
      if (isRequestedToStop()) {
        status = SOURCE_STOPPING;
      } else if (!acquireItem(pollItem, callbackContext)) {
        status = ALREADY_IN_PROCESS;
      } else {
        WatermarkStatus watermarkStatus = passesWatermark(pollItem);
        if (watermarkStatus == REJECT) {
          status = FILTERED_BY_WATERMARK;
        } else if (currentPollItems < maxItemsPerPoll) {
          currentPollItems++;
          sourceCallback.handle(pollItem.getResult(), callbackContext);
          saveWatermarkValue(watermarkStatus, pollItem);
        } else {
          currentPollItemLimitApplied = true;
          processLimitApplied(watermarkStatus, pollItem);
        }
      }

      if (status != ACCEPTED || currentPollItemLimitApplied) {
        LOGGER.debug(REJECTED_ITEM_MESSAGE, itemId, status);
        rejectItem(pollItem.getResult(), callbackContext);
      } else {
        LOGGER.debug(ACCEPTED_ITEM_MESSAGE, itemId);
      }

      return status;
    }

    private void processLimitApplied(WatermarkStatus watermarkStatus, DefaultPollItem pollItem) {
      Serializable itemWatermark = pollItem.getWatermark().orElse(null);
      if (itemWatermark == null || watermarkStatus != PASSED) {
        return;
      }
      if (minimumRejectedByLimitPassingWatermark == null ||
          compareWatermarks("itemWatermark", itemWatermark, "minimumRejectedByLimitPassingWatermark",
                            minimumRejectedByLimitPassingWatermark, watermarkComparator) < 0) {
        LOGGER.debug("An item that passed all previous validations is being rejected by the poll limit and its watermark" +
            "value will be stored so that is processed on future polls if sent for processing.");
        minimumRejectedByLimitPassingWatermark = itemWatermark;
      }
    }

    private void saveWatermarkValue(WatermarkStatus watermarkStatus, DefaultPollItem pollItem) {
      String itemId = pollItem.getItemId().orElse(null);
      Serializable itemWatermark = pollItem.getWatermark().orElse(null);
      if (itemWatermark == null) {
        return;
      }
      switch (watermarkStatus) {
        case ON_NEW_HIGH:
          renewUpdatedWatermark(itemWatermark);
          LOGGER.debug("A new watermark maximum has been found when processing item with id {} for source in flow {}", itemId,
                       flowName);
        case ON_HIGH:
          addToUpdatedWatermark(itemId, itemWatermark);
          LOGGER.debug("Watermark value for item with id {} is equal to the maximum value found for source in flow {}", itemId,
                       flowName);
        case PASSED:
          addToRecentlyProcessedIds(itemId, itemWatermark);
          LOGGER.debug("Item with id {} passed the watermark validation and will be processed in flow {}", itemId, flowName);
        case REJECT:
          break;
      }
    }

    private void renewUpdatedWatermark(Serializable itemWatermark) {
      try {
        idsOnUpdatedWatermark.clear();
        this.updatedWatermark = itemWatermark;
        removeWatermark(UPDATED_WATERMARK_ITEM_OS_KEY);
        saveWatermark(UPDATED_WATERMARK_ITEM_OS_KEY, updatedWatermark);
      } catch (ObjectStoreException e) {
        throw new MuleRuntimeException(
                                       createStaticMessage("An error occurred while trying to update the updatedWatermark in the the object store"),
                                       e);
      }
    }

    private void addToUpdatedWatermark(String itemId, Serializable itemWatermark) {
      if (itemId != null) {
        try {
          idsOnUpdatedWatermark.store(itemId, itemWatermark);
        } catch (ObjectStoreException e) {
          throw new MuleRuntimeException(
                                         createStaticMessage("An error occurred while updating the watermark for Item with ID [%s]",
                                                             itemId),
                                         e);
        }
      }
    }

    private void addToRecentlyProcessedIds(String itemId, Serializable itemWatermark) {
      try {
        if (itemId != null) {
          if (recentlyProcessedIds.contains(itemId)) {
            recentlyProcessedIds.remove(itemId);
          }
          recentlyProcessedIds.store(itemId, itemWatermark);
        }
      } catch (ObjectStoreException e) {
        throw new MuleRuntimeException(
                                       createStaticMessage("An error occurred while updating the watermark for Item with ID [%s]",
                                                           itemId),
                                       e);
      }
    }

    @Override
    public boolean isSourceStopping() {
      return isRequestedToStop();
    }

    @Override
    public Optional<Serializable> getWatermark() {
      return ofNullable(currentWatermark);
    }

    @Override
    public void setWatermarkComparator(Comparator<? extends Serializable> comparator) {
      checkArgument(comparator != null, "Cannot set a null watermark comparator");
      this.watermarkComparator = (Comparator<Serializable>) comparator;
    }

    @Override
    public void onConnectionException(ConnectionException e) {
      sourceCallback.onConnectionException(e);
    }

    public Optional<Serializable> getMinimumRejectedByLimitPassingWatermark() {
      return ofNullable(minimumRejectedByLimitPassingWatermark);
    }

    private Optional<Serializable> getUpdatedWatermark() {
      return ofNullable(updatedWatermark);
    }

    private Comparator<Serializable> getWatermarkComparator() {
      return watermarkComparator;
    }

    private WatermarkStatus passesWatermark(DefaultPollItem pollItem) {
      Serializable itemWatermark = pollItem.getWatermark().orElse(null);
      if (itemWatermark == null) {
        return PASSED;
      }
      String itemId = pollItem.getItemId().orElse(null);

      WatermarkStatus status = PASSED;
      int compare;
      if (currentWatermark == null && updatedWatermark == null) {
        status = ON_NEW_HIGH;
      } else {
        compare = currentWatermark != null
            ? compareWatermarks("currentWatermark", currentWatermark, "itemWatermark", itemWatermark, watermarkComparator)
            : -1;
        if (compare < 0) {
          try {
            if (itemId != null && recentlyProcessedIds.contains(itemId)) {
              Serializable previousItemWatermark = recentlyProcessedIds.retrieve(itemId);
              if (compareWatermarks("itemWatermark", itemWatermark, "previousItemWatermark", previousItemWatermark,
                                    watermarkComparator) <= 0) {
                status = REJECT;
              }
            }
            if (status != REJECT) {
              int updatedWatermarkCompare =
                  updatedWatermark != null
                      ? compareWatermarks("updatedWatermark", updatedWatermark, "itemWatermark", itemWatermark,
                                          watermarkComparator)
                      : -1;
              if (updatedWatermarkCompare == 0) {
                status = ON_HIGH;
              } else if (updatedWatermarkCompare < 0) {
                status = ON_NEW_HIGH;
              }
            }
          } catch (ObjectStoreException e) {
            throw new MuleRuntimeException(
                                           createStaticMessage("An error occurred while checking the previous watermark" +
                                               " for an item id that was recently processed. Item with ID [%s]",
                                                               itemId),
                                           e);
          }
        } else if (compare == 0 && pollItem.getItemId().isPresent()) {
          try {
            status = recentlyProcessedIds.contains(itemId) ? REJECT : PASSED;
          } catch (ObjectStoreException e) {
            throw new MuleRuntimeException(
                                           createStaticMessage("An error occurred while checking the existence for Item with ID [%s]",
                                                               itemId),
                                           e);
          }
        } else {
          status = REJECT;
        }
      }

      if (status == REJECT) {
        LOGGER.atDebug()
            .setMessage("Source in flow '{}' is skipping item '{}' because it was rejected by the watermark")
            .addArgument(flowName)
            .addArgument(() -> getItemId(pollItem))
            .log();
      }

      return status;
    }
  }

  private String getItemId(DefaultPollItem pollItem) {
    return pollItem.getItemId().orElseGet(() -> pollItem.getResult().getAttributes().map(Object::toString).orElse(""));
  }

  private class DefaultPollItem implements PollItem<T, A> {

    private final SourceCallbackContext sourceCallbackContext;
    private Result<T, A> result;
    private Serializable watermark;
    private String itemId;

    private DefaultPollItem(SourceCallbackContext sourceCallbackContext) {
      this.sourceCallbackContext = sourceCallbackContext;
    }

    @Override
    public SourceCallbackContext getSourceCallbackContext() {
      return sourceCallbackContext;
    }

    @Override
    public PollItem<T, A> setResult(Result<T, A> result) {
      checkArgument(result != null, "Cannot set a null Result");
      this.result = result;

      return this;
    }

    @Override
    public PollItem<T, A> setWatermark(Serializable watermark) {
      checkArgument(watermark != null, "Cannot set a null watermark");
      this.watermark = watermark;

      return this;
    }

    @Override
    public PollItem<T, A> setId(String id) {
      checkArgument(id != null, "Cannot set a null id");
      itemId = id;

      return this;
    }

    private Optional<Serializable> getWatermark() {
      return ofNullable(watermark);
    }

    private Optional<String> getItemId() {
      return ofNullable(itemId);
    }

    private Result<T, A> getResult() {
      return result;
    }

    private void validate() {
      if (result == null) {
        throw new IllegalStateException(format("Missing item Result. "
            + "Source in flow '%s' pushed an item with ID '%s' without configuring its Result",
                                               flowName, itemId));
      }
    }
  }

  private void rejectItem(Result<T, A> result, SourceCallbackContext context) {
    try {
      delegate.onRejectedItem(result, context);
    } finally {
      release(context);
      if (context instanceof SourceCallbackContextAdapter) {
        ((SourceCallbackContextAdapter) context).releaseConnection();
      }
    }
  }

  private void release(SourceCallbackContext context) {
    context.<ItemReleaser>getVariable(ITEM_RELEASER_CTX_VAR).ifPresent(ItemReleaser::release);
  }

  private void withWatermarkLock(CheckedRunnable runnable) {
    Lock lock = getWatermarkLock();
    lock.lock();
    try {
      runnable.run();
    } finally {
      safeUnlock(lock);
    }
  }

  private Lock getWatermarkLock() {
    return lockFactory.createLock(formatKey("watermark"));
  }

  private void updateWatermark(Serializable value, Comparator comparator,
                               Serializable minimumRejectedByLimitPassingWatermark) {
    try {
      if (minimumRejectedByLimitPassingWatermark != null) {
        LOGGER
            .debug("During the poll in the flow {}, items were rejected due to the item limit, a lower watermark than the maximum found will"
                +
                "have to be the new current watermark to ensure that those items are not left without being processed.",
                   flowName);
        setCurrentWatermarkAsMinimumRejectWatermark(minimumRejectedByLimitPassingWatermark);
      } else {
        updateWatermark(value, comparator);
      }
    } catch (ObjectStoreException e) {
      throw new MuleRuntimeException(
                                     createStaticMessage(format("Failed to update watermark value for message source at location '%s'. %s",
                                                                flowName, e.getMessage())),
                                     e);
    }
  }

  private void updateWatermark(Serializable value, Comparator comparator) throws ObjectStoreException {
    if (watermarkObjectStore.contains(WATERMARK_ITEM_OS_KEY)) {
      Serializable currentValue = watermarkObjectStore.retrieve(WATERMARK_ITEM_OS_KEY);
      if (compareWatermarks("currentValue", currentValue, "value", value, comparator) >= 0) {
        return;
      }
      watermarkObjectStore.remove(WATERMARK_ITEM_OS_KEY);
    }

    updateRecentlyProcessedIds();
    saveWatermark(WATERMARK_ITEM_OS_KEY, value);
  }

  private void setCurrentWatermarkAsMinimumRejectWatermark(Serializable minimumRejectedByLimitPassingWatermark)
      throws ObjectStoreException {
    removeWatermark(WATERMARK_ITEM_OS_KEY);
    saveWatermark(WATERMARK_ITEM_OS_KEY, minimumRejectedByLimitPassingWatermark);
  }

  private void updateRecentlyProcessedIds() throws ObjectStoreException {
    Lock osClearingLock = lockFactory.createLock(UPDATE_PROCESSED_LOCK);
    try {
      osClearingLock.lock();
      List<String> strings = idsOnUpdatedWatermark.allKeys();
      recentlyProcessedIds.clear();
      strings.forEach(key -> {
        try {
          recentlyProcessedIds.store(key, idsOnUpdatedWatermark.retrieve(key));
        } catch (ObjectStoreException e) {
          throw new MuleRuntimeException(createStaticMessage("An error occurred while updating the watermark Ids. Failed to update key '%s' in Watermark-IDs ObjectStore: %s",
                                                             key, e.getMessage()),
                                         e);
        }
      });
      idsOnUpdatedWatermark.clear();
    } finally {
      safeUnlock(osClearingLock);
    }
  }

  private Serializable getWatermark(String watermarkKey) {
    try {
      if (watermarkObjectStore.contains(watermarkKey)) {
        Serializable watermark = watermarkObjectStore.retrieve(watermarkKey);
        LOGGER.trace(WATERMARK_RETURNED_MESSAGE, watermarkKey, watermark, flowName);
        return watermark;
      } else {
        LOGGER.trace(WATERMARK_NOT_RETURNED_MESSAGE, watermarkKey, flowName);
        return null;
      }
    } catch (ObjectStoreException e) {
      throw new MuleRuntimeException(
                                     createStaticMessage(format("Failed to fetch watermark for Message source at location '%s'. %s",
                                                                flowName, e.getMessage())),
                                     e);
    }
  }

  private void saveWatermark(String watermarkKey, Serializable watermarkValue) throws ObjectStoreException {
    watermarkObjectStore.store(watermarkKey, watermarkValue);
    LOGGER.trace(WATERMARK_SAVED_MESSAGE, watermarkKey, watermarkValue, flowName);
  }

  private void removeWatermark(String watermarkKey) throws ObjectStoreException {
    if (watermarkObjectStore.contains(watermarkKey)) {
      watermarkObjectStore.remove(watermarkKey);
      LOGGER.trace(WATERMARK_REMOVED_MESSAGE, watermarkKey, flowName);
    }
  }

  private Serializable getCurrentWatermark() {
    return getWatermark(WATERMARK_ITEM_OS_KEY);
  }

  private Serializable getUpdatedWatermark() {
    return getWatermark(UPDATED_WATERMARK_ITEM_OS_KEY);
  }

  private boolean acquireItem(DefaultPollItem pollItem, SourceCallbackContext callbackContext) {
    if (!pollItem.getItemId().isPresent()) {
      return true;
    }

    String id = pollItem.getItemId().get();
    Lock lock = lockFactory.createLock(flowName + "/" + id);
    if (!lock.tryLock()) {
      LOGGER.debug("Source at flow '{}' is skipping processing of item '{}' because another thread or node already has a mule "
          + "lock on it", flowName, id);
      return false;
    }

    try {
      if (inflightIdsObjectStore.contains(id)) {
        LOGGER.debug("Source at flow '{}' polled item '{}', but skipping it since it is already being processed in another "
            + "thread or node", flowName, id);
        return false;
      } else {
        try {
          inflightIdsObjectStore.store(id, id);
          callbackContext.addVariable(ITEM_RELEASER_CTX_VAR, new ItemReleaser(id));
          return true;
        } catch (ObjectStoreException e) {
          LOGGER.atError()
              .setCause(e)
              .log("Flow at source '{}' could not track item '{}' as being processed. {}",
                   flowName, id, e.getMessage());
          return false;
        }
      }
    } catch (Exception e) {
      LOGGER.atError()
          .setCause(e)
          .log("Could not guarantee idempotency for item '{}' for source at flow '{}'. '{}'",
               id, flowName, e.getMessage());
      return false;
    } finally {
      safeUnlock(lock);
    }
  }

  private boolean isRequestedToStop() {
    return stopRequested.get() || Thread.currentThread().isInterrupted();
  }

  private void shutdownScheduler() {
    if (executor != null) {
      executor.stop();
      executor = null;
    }
  }

  private int statusToNotificationType(PollContext.PollItemStatus status, boolean currentPollItemLimitApplied) {
    switch (status) {
      case ACCEPTED:
        return ITEM_DISPATCHED;
      case FILTERED_BY_WATERMARK:
        return ITEM_REJECTED_WATERMARK;
      case ALREADY_IN_PROCESS:
        return ITEM_REJECTED_IDEMPOTENCY;
      case SOURCE_STOPPING:
        return ITEM_REJECTED_SOURCE_STOPPING;
    }
    return NO_ACTION_ID;
  }

  private class ItemReleaser {

    private final String id;

    private ItemReleaser(String id) {
      this.id = id;
    }

    private void release() {
      try {
        if (inflightIdsObjectStore.contains(id)) {
          inflightIdsObjectStore.remove(id);
        }
      } catch (ObjectStoreException e) {
        LOGGER.atError()
            .setCause(e)
            .log("Could not untrack item '{}' in source at flow '{}'. {}",
                 id, flowName, e.getMessage());
      }
    }
  }
}
