/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.filters;

import static org.mule.runtime.core.DefaultMessageContext.create;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.util.ClassUtils.hash;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.routing.filter.ObjectFilter;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.transformer.simple.ByteArrayToObject;
import org.mule.runtime.core.util.AttributeEvaluator;
import org.mule.runtime.core.util.ClassUtils;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>RegExFilter</code> is used to match a String argument against a regular expression.
 */
public class RegExFilter implements Filter, ObjectFilter, MuleContextAware, Initialisable {

  private static final int NO_FLAGS = 0;
  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  private Pattern pattern;
  private MuleContext muleContext;
  private int flags = NO_FLAGS;
  private AttributeEvaluator value;

  @Override
  public void initialise() throws InitialisationException {
    if (value != null) {
      value.initialize(muleContext.getExpressionManager());
    }
  }

  public RegExFilter() {
    super();
  }

  public RegExFilter(String pattern) {
    this(pattern, null, NO_FLAGS);
  }

  public RegExFilter(String pattern, int flags) {
    this(pattern, null, flags);
  }

  public RegExFilter(String pattern, String value) {
    this(pattern, value, NO_FLAGS);
  }

  public RegExFilter(String pattern, String value, int flags) {
    this.pattern = Pattern.compile(pattern, flags);
    this.flags = flags;
    this.value = new AttributeEvaluator(value);
  }

  @Override
  public boolean accept(MuleMessage message) {
    // TODO MULE-9341 Remove Filters that are not needed
    Flow flowConstruct = new Flow("", muleContext);
    return accept(MuleEvent.builder(create(flowConstruct, "RegExFilter")).message(message).exchangePattern(ONE_WAY)
        .flow(flowConstruct).build());
  }

  @Override
  public boolean accept(MuleEvent event) {
    try {
      if (value != null && value.getRawValue() != null) {
        return accept(value.resolveValue(event));
      } else {
        final MuleMessage transformedMessage =
            muleContext.getTransformationService().transform(event.getMessage(), DataType.STRING);
        // If the payload is a stream and we've consumed it, then we should set the payload on the
        // message. This is the only time this method will alter the payload on the message
        if (event.getMessage().getDataType().isStreamType()) {
          event.setMessage(transformedMessage);
        }
        return accept((Object) transformedMessage.getPayload());
      }
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public boolean accept(Object object) {
    if (object == null) {
      return false;
    }

    Object tempObject = object;

    // check whether the payload is a byte[] or a char[]. If it is, then it has
    // to be transformed otherwise the toString will not represent the true
    // contents
    // of the payload for the RegEx filter to use.
    if (object instanceof byte[]) {
      ByteArrayToObject transformer = new ByteArrayToObject();
      try {
        object = transformer.transform(object);
      } catch (TransformerException e) {
        logger.warn(CoreMessages.transformFailedBeforeFilter().toString(), e);
        // revert transformation
        object = tempObject;
      }
    } else if (object instanceof char[]) {
      object = new String((char[]) object);
    }

    return (pattern != null && pattern.matcher(object.toString()).find());
  }

  public String getPattern() {
    return (pattern == null ? null : pattern.pattern());
  }

  public void setPattern(String pattern) {
    this.pattern = (pattern != null ? Pattern.compile(pattern, flags) : null);
  }

  public int getFlags() {
    return flags;
  }

  public void setFlags(int flags) {
    this.flags = flags;
    this.pattern = (this.pattern != null ? Pattern.compile(pattern.pattern(), flags) : null);
  }

  public void setValue(String value) {
    this.value = new AttributeEvaluator(value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final RegExFilter other = (RegExFilter) obj;
    boolean patternsAreEqual = ClassUtils.equal(pattern.pattern(), other.pattern.pattern());
    boolean flagsAreEqual = (flags == other.flags);
    boolean valuesAreEquals = areValuesEqual(value, other.value);
    return (patternsAreEqual && flagsAreEqual && valuesAreEquals);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public int hashCode() {
    return hash(new Object[] {this.getClass(), pattern, value == null ? null : value.getRawValue()});
  }

  private boolean areValuesEqual(AttributeEvaluator value1, AttributeEvaluator value2) {
    if (value1 == null) {
      return value2 == null;
    } else {
      if (value2 == null) {
        return false;
      }
      if (value1.getRawValue() == null) {
        return value2.getRawValue() == null;
      }
      return value1.getRawValue().equals(value2.getRawValue());
    }
  }
}
