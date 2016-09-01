/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.source.polling.watermark.selector;

import static java.lang.String.format;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.runtime.core.source.polling.watermark.WatermarkUtils;

import java.io.NotSerializableException;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatermarkSelectorWrapper extends WatermarkSelector {

  private static final Logger logger = LoggerFactory.getLogger(WatermarkSelectorWrapper.class);

  private final String selectorExpression;
  private final WatermarkSelector wrapped;
  private final MuleEvent muleEvent;
  private MuleContext muleContext;

  protected WatermarkSelectorWrapper(WatermarkSelector wrapped, String selectorExpression, MuleEvent muleEvent,
                                     MuleContext muleContext) {
    this.selectorExpression = selectorExpression;
    this.wrapped = wrapped;
    this.muleEvent = MuleEvent.builder(muleEvent).session(new DefaultMuleSession(muleEvent.getSession())).build();
    this.muleContext = muleContext;
  }

  @Override
  public void acceptValue(Object value) {
    muleEvent.setMessage(MuleMessage.builder(muleEvent.getMessage()).payload(value).build());
    try {
      Serializable evaluated = WatermarkUtils.evaluate(this.selectorExpression, muleEvent, muleContext);
      this.wrapped.acceptValue(evaluated);
    } catch (NotSerializableException e) {
      logger.warn(format("Watermark selector expression '%s' did not resolved to a Serializable value. Value will be ignored",
                         this.selectorExpression),
                  e);
    }
  }

  @Override
  public Object getSelectedValue() {
    return this.wrapped.getSelectedValue();
  }

  @Override
  public void reset() {
    this.wrapped.reset();
  }

}
