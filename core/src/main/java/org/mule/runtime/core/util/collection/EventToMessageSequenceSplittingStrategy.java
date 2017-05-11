/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.collection;

import static java.lang.String.format;
import static org.mule.runtime.core.el.DefaultExpressionManager.hasMelExpression;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.util.Copiable;
import org.mule.runtime.core.routing.ExpressionSplittingStrategy;
import org.mule.runtime.core.routing.MessageSequence;
import org.mule.runtime.core.routing.outbound.ArrayMessageSequence;
import org.mule.runtime.core.routing.outbound.CollectionMessageSequence;
import org.mule.runtime.core.routing.outbound.IteratorMessageSequence;
import org.mule.runtime.core.routing.outbound.NodeListMessageSequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.NodeList;

public class EventToMessageSequenceSplittingStrategy implements SplittingStrategy<Event, MessageSequence<?>> {

  private ExpressionSplittingStrategy expressionSplitterStrategy;

  /**
   * @param expressionSplitterStrategy expression splitter strategy to use as default mechanism if there was supported mechanism
   *        to split the payload
   */
  public EventToMessageSequenceSplittingStrategy(ExpressionSplittingStrategy expressionSplitterStrategy) {
    this.expressionSplitterStrategy = expressionSplitterStrategy;
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public MessageSequence<?> split(Event event) {
    if (expressionSplitterStrategy.hasDefaultExpression()) {
      Message msg = event.getMessage();
      Object payload = msg.getPayload().getValue();
      if (payload instanceof MessageSequence<?>) {
        return ((MessageSequence<?>) payload);
      }
      if (payload instanceof Iterator<?>) {
        return new IteratorMessageSequence(((Iterator<Object>) payload));
      }
      if (payload instanceof Collection) {
        return new CollectionMessageSequence(copyCollection((Collection) payload));
      } else if (payload instanceof CursorIteratorProvider) {
        return new IteratorMessageSequence(((CursorIteratorProvider) payload).openCursor());
      } else if (payload instanceof Iterable<?>) {
        return new IteratorMessageSequence(((Iterable<Object>) payload).iterator());
      } else if (payload instanceof Object[]) {
        return new ArrayMessageSequence((Object[]) payload);
      } else if (payload instanceof NodeList) {
        return new NodeListMessageSequence((NodeList) payload);
      } else if (payload instanceof Map<?, ?>) {
        List<Object> list = new LinkedList<>();
        Set<Map.Entry<?, ?>> set = ((Map) payload).entrySet();
        for (Map.Entry<?, ?> entry : set) {
          list.add(new ImmutableEntry<>(entry));
        }
        return new CollectionMessageSequence(list);
      }
    }
    try {
      Iterator<TypedValue<?>> valueIterator = expressionSplitterStrategy.split(event);
      if (hasMelExpression(expressionSplitterStrategy.getExpression())) {
        List<Object> iteratorCollection = new ArrayList<>();
        valueIterator.forEachRemaining(iteratorCollection::add);
        return new CollectionMessageSequence<>(iteratorCollection);
      }
      return new IteratorMessageSequence(valueIterator);
    } catch (ExpressionRuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException(
                                         format("Could not split result of expression %s. The provided value is not instance of %s java "
                                             +
                                             "type or it's not a collection in any other format",
                                                expressionSplitterStrategy.getExpression(),
                                                new Class[] {Iterable.class, Iterator.class, MessageSequence.class,
                                                    Collection.class}),
                                         e);
    }

  }

  private Collection copyCollection(Collection payload) {
    return payload instanceof Copiable ? ((Copiable<Collection>) payload).copy() : new LinkedList(payload);
  }


}
