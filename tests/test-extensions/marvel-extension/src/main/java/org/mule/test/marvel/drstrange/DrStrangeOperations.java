/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.drstrange;

import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import static org.mule.test.marvel.drstrange.DrStrangeErrorTypeDefinition.CUSTOM_ERROR;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.reference.FlowReference;
import org.mule.runtime.extension.api.annotation.param.stereotype.Stereotype;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.mule.sdk.api.annotation.param.stereotype.ComponentId;
import org.mule.sdk.api.runtime.operation.FlowListener;
import org.mule.test.marvel.model.Relic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

public class DrStrangeOperations {

  @Inject
  private ConfigurationComponentLocator locator;

  @MediaType(TEXT_PLAIN)
  public String seekStream(@Connection MysticConnection connection, @Optional(defaultValue = PAYLOAD) InputStream stream,
                           int position)
      throws IOException {
    checkArgument(stream instanceof CursorStream, "Stream was not cursored");

    CursorStream cursor = (CursorStream) stream;
    cursor.seek(position);

    return readStream(connection, cursor);
  }

  @Throws(CustomErrorProvider.class)
  @MediaType(TEXT_PLAIN)
  public String readStream(@org.mule.sdk.api.annotation.param.Connection MysticConnection connection,
                           @Optional(defaultValue = PAYLOAD) InputStream stream)
      throws IOException {
    try {
      return IOUtils.toString(stream);
    } catch (Exception e) {
      throw new CustomErrorException(e, CUSTOM_ERROR);
    }
  }

  @MediaType(TEXT_PLAIN)
  public InputStream toStream(@Connection MysticConnection connection, @Optional(defaultValue = PAYLOAD) String data) {
    return connection.manage(new ByteArrayInputStream(data.getBytes()));
  }

  @MediaType(TEXT_PLAIN)
  public InputStream objectToStream(@Connection MysticConnection connection,
                                    @org.mule.sdk.api.annotation.param.Optional(defaultValue = PAYLOAD) Object data) {
    return connection.manage((InputStream) data);
  }

  public void crashCar(@Config DrStrange dr) {
    throw new RuntimeException();
  }

  @Stereotype(ReferableOperationStereotypeDefinition.class)
  public void withFlowReference(@org.mule.sdk.api.annotation.param.Config DrStrange dr, @Optional @FlowReference String flowRef,
                                @ComponentId String name) {

    if (!StringUtils.isBlank(flowRef)) {
      if (!locator.find(Location.builder().globalName(flowRef).build()).isPresent()) {
        throw new IllegalArgumentException("The referenced flow does not exist in this application");
      }
    }

  }

  public List<String> readObjectStream(@Content Iterator<String> iteratorValues) {
    List<String> objects = new LinkedList<>();
    while (iteratorValues.hasNext()) {
      objects.add(iteratorValues.next());
    }

    return objects;
  }

  /**
   * Generates a streaming result ({@link PagingProvider} in this case) but only after a certain {@code latch} is counted down.
   *
   * @param values              The values to generate the streaming result with.
   * @param fetchSize           The size of the pages.
   * @param latch               The {@link CountDownLatch} to wait on before producing the first page (can be used to simulate a
   *                            controlled delay).
   * @param providerClosedLatch A {@link CountDownLatch} to be counted down when the returned {@link PagingProvider} is closed.
   * @param flowListener        An implicit parameter, used to register callbacks for closing the stream when owning context
   *                            finishes.
   * @return A paginated result from the given {@code values}.
   */
  public PagingProvider<MysticConnection, String> latchedSayMagicWords(@Content List<String> values, int fetchSize,
                                                                       Object latch, Object providerClosedLatch,
                                                                       FlowListener flowListener) {
    if (!(latch instanceof CountDownLatch)) {
      throw new IllegalArgumentException("`latch` must be a CountDownLatch");
    }

    if (!(providerClosedLatch instanceof CountDownLatch)) {
      throw new IllegalArgumentException("`providerClosedLatch` must be a CountDownLatch");
    }

    return new SimplePagingProviderFromList(values, fetchSize) {

      @Override
      public List<String> getPage(MysticConnection connection) {
        if (index.get() == 0) {
          // These are needed because the disposal of the streaming state of the root event context will happen before this
          // PagingProvider is registered (due to the timeout).
          // The first page is fetched before the registration with the streaming state (see PagingResultTransformer).
          flowListener.onComplete(() -> this.close(connection));
          flowListener.onError(e -> this.close(connection));
          try {
            // Artificial delay on first page
            ((CountDownLatch) latch).await();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
        return super.getPage(connection);
      }

      @Override
      public void close(MysticConnection connection) {
        // Counts down on the latch, notifying the stream was closed
        ((CountDownLatch) providerClosedLatch).countDown();
      }
    };
  }

  public PagingProvider<MysticConnection, String> sayMagicWords(@Content List<String> values,
                                                                int fetchSize) {
    return new SimplePagingProviderFromList(values, fetchSize) {

      private int timesClosed = 0;

      @Override
      public void close(MysticConnection connection) {
        timesClosed++;
        if (timesClosed > 1) {
          throw new RuntimeException("Expected to be closed only once but was called twice");
        }
      }
    };
  }

  public PagingProvider<MysticConnection, Relic> getRelics(StreamingHelper streamingHelper) {
    return new PagingProvider<MysticConnection, Relic>() {

      private int currentPage = 1;
      private final int PAGES = 4;

      @Override
      public List<Relic> getPage(MysticConnection connection) {
        if (currentPage == PAGES) {
          return emptyList();
        }
        currentPage++;
        return new ArrayList<Relic>() {

          {
            add(getResolvedRelic("cloak"));
            add(getResolvedRelic("boots"));
            add(getResolvedRelic("staff"));
          }
        };
      }

      private Relic getResolvedRelic(String description) {
        return new Relic(streamingHelper.resolveCursorProvider(new ByteArrayInputStream(description.getBytes())));
      }

      @Override
      public java.util.Optional<Integer> getTotalResults(MysticConnection connection) {
        return java.util.Optional.empty();
      }

      @Override
      public void close(MysticConnection connection) throws MuleException {
        // Do nothing.
      }
    };
  }

  /**
   * "Dr. Strange and the Multiple Stream Readers of Madness", featuring an SDK scope.
   * <p>
   * Consume the payload stream both at the beginning and the end of the provided chain.
   *
   * @param operations
   * @param callback
   * @param payload
   * @param attributes
   */
  @MediaType(TEXT_PLAIN)
  public void scopeverse(Chain operations,
                         CompletionCallback<Object, Object> callback,
                         @Optional(defaultValue = "#[payload]") TypedValue<Object> payload,
                         @Optional(defaultValue = "#[attributes]") TypedValue<Object> attributes) {
    // Consume the stream
    IOUtils.toString(((InputStream) payload.getValue()));

    operations.process(payload, attributes,
                       result -> {
                         // Consume the stream
                         // the ResultOutput arrives here as a CursorStreamProvider. Changing this to a CursorStream in the chain
                         // infrastructure would break backwards compatibility, causing a CCE.
                         IOUtils.toString((((CursorStreamProvider) result.getOutput())).openCursor());
                         callback.success(result);
                       },
                       (error, previous) -> {
                         callback.error(error);
                       });
  }

  private static class SimplePagingProviderFromList implements PagingProvider<MysticConnection, String> {

    private final List<String> values;
    protected final AtomicInteger index = new AtomicInteger(0);
    private final int fetchSize;

    private SimplePagingProviderFromList(List<String> values, int fetchSize) {
      this.values = values;
      this.fetchSize = fetchSize;
    }

    @Override
    public List<String> getPage(MysticConnection connection) {
      final int i = index.get();
      if (i >= values.size()) {
        return emptyList();
      }

      List<String> words = values.subList(i, i + fetchSize);
      index.addAndGet(fetchSize);

      return words;
    }

    @Override
    public java.util.Optional<Integer> getTotalResults(MysticConnection connection) {
      return of(values.size());
    }

    @Override
    public void close(MysticConnection connection) {
      // This version does nothing
    }
  }
}
