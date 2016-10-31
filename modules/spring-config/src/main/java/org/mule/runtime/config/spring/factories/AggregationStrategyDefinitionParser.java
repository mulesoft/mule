/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.spring.factories;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.routing.AggregationContext;
import org.mule.runtime.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.runtime.config.spring.parsers.processors.CheckExclusiveAttribute;
import org.mule.runtime.core.routing.AggregationStrategy;

import org.w3c.dom.Element;

public class AggregationStrategyDefinitionParser extends ChildDefinitionParser {

  public AggregationStrategyDefinitionParser() {
    super("aggregationStrategy", null, AggregationStrategy.class);
    this.setAllowClassAttribute(true);
    this.registerPreProcessor(new CheckExclusiveAttribute(ATTRIBUTE_CLASS));
    this.registerPreProcessor(new CheckExclusiveAttribute(ATTRIBUTE_REF));
  }

  @Override
  protected Class<?> getBeanClass(Element element) {
    Class<?> clazz = super.getBeanClass(element);
    if (clazz != null) {
      return clazz;
    } else {
      if (element.hasAttribute(ATTRIBUTE_REF)) {
        return AggregationStrategyDelegate.class;
      } else {
        throw new IllegalStateException("<custom-merge-strategy> requires you to provide a value for either 'class' or 'ref' attributes");
      }
    }
  }

  public static class AggregationStrategyDelegate implements AggregationStrategy {

    private AggregationStrategy ref;

    @Override
    public Event aggregate(AggregationContext context) throws MuleException {
      return this.ref.aggregate(context);
    }

    public void setRef(AggregationStrategy ref) {
      this.ref = ref;
    }
  }

}
