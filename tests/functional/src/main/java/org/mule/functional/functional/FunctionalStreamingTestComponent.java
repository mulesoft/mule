/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.functional;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.IOUtils.ifInputStream;
import static org.mule.runtime.core.api.util.StringMessageUtils.getBoilerPlate;

import org.mule.functional.api.component.EventCallback;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.ClassUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A service that can be used by streaming functional tests. This service accepts an EventCallback that can be used to assert the
 * state of the current event. To access the service when embedded in an (XML) model, make sure that the descriptor sets the
 * singleton attribute true - see uses in TCP and FTP.
 * <p>
 * Note that although this implements the full StreamingService interface, nothing is written to the output stream - this is
 * intended as a final sink.
 *
 * @see EventCallback
 */
public abstract class FunctionalStreamingTestComponent extends AbstractComponent implements Processor, MuleContextAware {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  private static AtomicInteger count = new AtomicInteger(0);
  private int number = count.incrementAndGet();

  public static final int STREAM_SAMPLE_SIZE = 4;
  public static final int STREAM_BUFFER_SIZE = 4096;
  private EventCallback eventCallback;
  private String summary = null;
  private long targetSize = -1;

  private MuleContext muleContext;

  public FunctionalStreamingTestComponent() {
    logger.debug("creating " + toString());
  }

  public void setEventCallback(EventCallback eventCallback, long targetSize) {
    logger.debug("setting callback: " + eventCallback + " in " + toString());
    this.eventCallback = eventCallback;
    this.targetSize = targetSize;
  }

  public String getSummary() {
    return summary;
  }

  public int getNumber() {
    return number;
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return CoreEvent.builder(event)
        .message(Message.builder(event.getMessage()).value(ifInputStream(event.getMessage().getPayload().getValue(), in -> {
          try {
            logger.debug("arrived at " + toString());
            byte[] startData = new byte[STREAM_SAMPLE_SIZE];
            long startDataSize = 0;
            byte[] endData = new byte[STREAM_SAMPLE_SIZE]; // ring buffer
            long endDataSize = 0;
            long endRingPointer = 0;
            long streamLength = 0;
            byte[] buffer = new byte[STREAM_BUFFER_SIZE];

            // throw data on the floor, but keep a record of size, start and end values
            long bytesRead = 0;

            while (bytesRead >= 0) {
              bytesRead = read(in, buffer);
              if (bytesRead > 0) {
                if (logger.isDebugEnabled()) {
                  logger.debug("read " + bytesRead + " bytes");
                }

                streamLength += bytesRead;
                long startOfEndBytes = 0;
                for (long i = 0; startDataSize < STREAM_SAMPLE_SIZE && i < bytesRead; ++i) {
                  startData[(int) startDataSize++] = buffer[(int) i];
                  ++startOfEndBytes; // skip data included in startData
                }
                startOfEndBytes = Math.max(startOfEndBytes, bytesRead - STREAM_SAMPLE_SIZE);
                for (long i = startOfEndBytes; i < bytesRead; ++i) {
                  ++endDataSize;
                  endData[(int) (endRingPointer++ % STREAM_SAMPLE_SIZE)] = buffer[(int) i];
                }
                if (streamLength >= targetSize) {
                  doCallback(startData, startDataSize, endData, endDataSize, endRingPointer, streamLength, event);
                }
              }
            }

            in.close();
          } catch (Exception e) {
            in.close();

            e.printStackTrace();
            if (logger.isDebugEnabled()) {
              logger.debug("Error on test component", e);
            }
            throw e;
          }

          return null;
        })).build()).build();
  }

  protected int read(InputStream in, byte[] buffer) throws IOException {
    return in.read(buffer);
  }

  private void doCallback(byte[] startData, long startDataSize, byte[] endData, long endDataSize, long endRingPointer,
                          long streamLength, CoreEvent event)
      throws Exception {
    // make a nice summary of the data
    StringBuilder result = new StringBuilder("Received stream");
    result.append("; length: ");
    result.append(streamLength);
    result.append("; '");

    for (long i = 0; i < startDataSize; ++i) {
      result.append((char) startData[(int) i]);
    }

    long endSize = Math.min(endDataSize, STREAM_SAMPLE_SIZE);
    if (endSize > 0) {
      result.append("...");
      for (long i = 0; i < endSize; ++i) {
        result.append((char) endData[(int) ((endRingPointer + i) % STREAM_SAMPLE_SIZE)]);
      }
    }
    result.append("'");

    summary = result.toString();

    String msg = getBoilerPlate("Message Received in service: " + this.getLocation().getRootContainerName() + ". " + summary
        + "\n callback: " + eventCallback, '*', 80);

    logger.info(msg);

    if (eventCallback != null) {
      eventCallback.eventReceived(event, this, muleContext);
    }
  }

  @Override
  public String toString() {
    return ClassUtils.getSimpleName(getClass()) + "/" + number;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  /**
   * @return the first {@link FunctionalStreamingTestComponent} instance from a flow with the provided name.
   */
  public static FunctionalStreamingTestComponent getFromFlow(ConfigurationComponentLocator locator, String flowName)
      throws Exception {
    final FlowConstruct flowConstruct = (FlowConstruct) locator.find(Location.builder().globalName(flowName).build()).get();

    if (flowConstruct != null) {
      if (flowConstruct instanceof Pipeline) {
        Pipeline flow = (Pipeline) flowConstruct;
        // Retrieve the first component
        for (Processor processor : flow.getProcessors()) {
          if (processor instanceof FunctionalStreamingTestComponent) {
            return (FunctionalStreamingTestComponent) processor;
          }
        }
      }

      throw new MuleRuntimeException(createStaticMessage("Can't get component from flow construct " + flowConstruct.getName()));
    } else {
      throw new MuleRuntimeException(createStaticMessage("Flow " + flowName + " not found in Registry"));
    }
  }
}
