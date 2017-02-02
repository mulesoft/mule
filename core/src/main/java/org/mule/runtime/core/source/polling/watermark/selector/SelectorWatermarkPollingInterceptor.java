/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.source.polling.watermark.selector;

import static java.lang.String.format;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.source.polling.watermark.Watermark;
import org.mule.runtime.core.source.polling.watermark.WatermarkPollingInterceptor;
import org.mule.runtime.core.streaming.ProvidesTotalHint;

import java.io.Serializable;
import java.util.Iterator;

/**
 * Implementation of {@link org.mule.runtime.core.source.polling.watermark.WatermarkPollingInterceptor} that uses a
 * {@link WatermarkSelector} to return the new watermark value.
 * 
 * @since 3.5.0
 */
public class SelectorWatermarkPollingInterceptor extends WatermarkPollingInterceptor implements MuleContextAware {

  private final WatermarkSelector selector;
  private final String selectorExpression;
  private MuleContext muleContext;

  public SelectorWatermarkPollingInterceptor(Watermark watermark, WatermarkSelector selector, String selectorExpression) {
    super(watermark);
    this.selector = selector;
    this.selectorExpression = selectorExpression;
  }

  /**
   * <p>
   * {@inheritDoc}
   * </p>
   * <p>
   * If the payload is a {@link Iterable}, then it is iterated passing all values evaluated through the selector. This is so
   * because not only different kinds of collections can be traversed in unpredictable ways, but also collections are often copied
   * before being iterated in which case we have no interception point.
   * </p>
   * <p>
   * If the payload is an {@link Iterator}, then a static proxy is generated so that we can intercept all values an evaluate them
   * through the selector. <b>Notice that if the {@link Iterable} or {@link Iterator} are not fully consumed, the unretrieved
   * values will not be received by the {@link WatermarkSelector}
   * </p>
   */
  @SuppressWarnings("unchecked")
  @Override
  public Event prepareRouting(Event sourceEvent, Event event, FlowConstruct flow) throws ConfigurationException {
    event = super.prepareRouting(sourceEvent, event, flow);
    Object payload = event.getMessage().getPayload().getValue();
    final WatermarkSelector selector = new WatermarkSelectorWrapper(this.selector, this.selectorExpression, event, muleContext);

    if (payload instanceof Iterable) {
      // consume early since the user could consume this collection in
      // unpredictable ways. He could even not consume it completely at all
      for (Object object : (Iterable<?>) payload) {
        selector.acceptValue(object);
      }
    } else if (payload instanceof Iterator) {
      event = Event.builder(event).message(InternalMessage.builder(event.getMessage())
          .payload(new SelectorIteratorProxy<>((Iterator<Object>) payload, selector)).build()).build();
    } else {
      throw new ConfigurationException(CoreMessages
          .createStaticMessage(format("Poll executing with payload of class %s but selector can only handle Iterator and Iterable objects when watermark is to be updated via selectors",
                                      payload.getClass().getCanonicalName())));
    }

    return event;
  }

  @Override
  public void postProcessRouting(Event event) throws ObjectStoreException {
    this.watermark.updateWith(event, (Serializable) this.selector.getSelectedValue());
  }

  private static class SelectorIteratorProxy<T> implements Iterator<T>, ProvidesTotalHint {

    private final Iterator<T> delegate;
    private final WatermarkSelector selector;

    private SelectorIteratorProxy(Iterator<T> delegate, WatermarkSelector selector) {
      this.delegate = delegate;
      this.selector = selector;
    }

    @Override
    public boolean hasNext() {
      return delegate.hasNext();
    }

    @Override
    public T next() {
      T next = delegate.next();
      selector.acceptValue(next);
      return next;
    }

    @Override
    public void remove() {
      delegate.remove();
    }

    @Override
    public int size() {
      return (delegate instanceof ProvidesTotalHint) ? ((ProvidesTotalHint) delegate).size() : -1;
    }
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}
