/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import static org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporterUtils.processSpanExporter;

import static java.lang.System.nanoTime;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import static io.opentelemetry.sdk.internal.ThrowableUtil.propagateIfFatal;
import static io.opentelemetry.sdk.trace.internal.JcTools.drain;
import static io.opentelemetry.sdk.trace.internal.JcTools.newFixedSizeQueue;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.opentelemetry.sdk.internal.DaemonThreadFactory;

/**
 * A queue for the export of Open Telemetry spans.
 *
 * @since 4.5.0
 */
public class OpenTelemetryExportQueue {

  public static final String OPTEL_EXPORTER_PREFIX = "optel-exporter-";

  public static final int EXPORT_QUEUE_CAPACITY = 2048;
  public static final int EXPORT_BATCH_SIZE = 512;
  public static final int SCHEDULE_DELAY_MILLIS = 3000;

  private final Queue<OpenTelemetrySpanExporter> queue;
  private final Worker worker;

  private final BlockingQueue<Boolean> signal = new ArrayBlockingQueue<>(1);

  public OpenTelemetryExportQueue(String artifactId) {
    queue = newFixedSizeQueue(EXPORT_QUEUE_CAPACITY);
    worker = new Worker();
    Thread workerThread = new DaemonThreadFactory(OPTEL_EXPORTER_PREFIX + artifactId).newThread(worker);
    workerThread.start();
  }

  public void offer(OpenTelemetrySpanExporter openTelemetrySpanExporter) {
    if (queue.offer(openTelemetrySpanExporter) && queue.size() >= EXPORT_BATCH_SIZE) {
      signal.offer(true);
    }
  }

  public void stopWorker() {
    worker.stop();
  }

  /**
   * The worker to create the open telemetry span and export it on end.
   */
  private class Worker implements Runnable {

    private volatile boolean continueWork = true;

    private long nextExportTime;

    private final ArrayList<OpenTelemetrySpanExporter> batch = new ArrayList<>(EXPORT_BATCH_SIZE);

    @Override
    public void run() {
      updateNextExportTime();

      while (continueWork) {
        drain(queue, EXPORT_BATCH_SIZE - batch.size(), batch::add);

        if (batch.size() >= EXPORT_BATCH_SIZE || nanoTime() >= nextExportTime) {
          exportCurrentBatch();
          updateNextExportTime();
        }
        if (queue.isEmpty()) {
          try {
            long pollWaitTime = nextExportTime - nanoTime();
            if (pollWaitTime > 0) {
              signal.poll(pollWaitTime, NANOSECONDS);
            }
          } catch (InterruptedException e) {
            currentThread().interrupt();
            return;
          }
        }
      }
    }

    private void exportCurrentBatch() {
      if (batch.isEmpty()) {
        return;
      }

      try {
        for (OpenTelemetrySpanExporter span : batch) {
          processSpanExporter(span);
        }
      } catch (Throwable t) {
        propagateIfFatal(t);
      } finally {
        batch.clear();
      }
    }

    private void updateNextExportTime() {
      nextExportTime = nanoTime() + SCHEDULE_DELAY_MILLIS;
    }

    public void stop() {
      continueWork = false;
    }
  }
}
