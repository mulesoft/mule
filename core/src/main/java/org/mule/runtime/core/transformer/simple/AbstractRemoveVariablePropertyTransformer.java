/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;
import org.mule.runtime.core.util.AttributeEvaluator;
import org.mule.runtime.core.util.WildcardAttributeEvaluator;

import java.nio.charset.Charset;
import java.util.Set;

public abstract class AbstractRemoveVariablePropertyTransformer extends AbstractMessageTransformer {

  private AttributeEvaluator identifierEvaluator;
  private WildcardAttributeEvaluator wildcardAttributeEvaluator;

  public AbstractRemoveVariablePropertyTransformer() {
    registerSourceType(DataType.OBJECT);
    setReturnDataType(DataType.OBJECT);
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    this.identifierEvaluator.initialize(muleContext.getExpressionManager());
  }

  @Override
  public Object transformMessage(MuleEvent event, Charset outputEncoding) throws TransformerException {
    if (wildcardAttributeEvaluator.hasWildcards()) {
      wildcardAttributeEvaluator.processValues(getPropertyNames(event), matchedValue -> {
        removeProperty(event, matchedValue);
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Removing property: '%s' from scope: '%s'", matchedValue, getScopeName()));
        }
      });
    } else {
      Object keyValue = identifierEvaluator.resolveValue(event);
      if (keyValue != null) {
        removeProperty(event, keyValue.toString());
      } else {
        logger.info("Key expression return null, no property will be removed");
      }
    }
    return event.getMessage();
  }

  protected abstract Set<String> getPropertyNames(MuleEvent event);

  protected abstract void removeProperty(MuleEvent event, String propertyName);

  @Override
  public Object clone() throws CloneNotSupportedException {
    AbstractRemoveVariablePropertyTransformer clone = (AbstractRemoveVariablePropertyTransformer) super.clone();
    clone.setIdentifier(this.identifierEvaluator.getRawValue());
    return clone;
  }

  public void setIdentifier(String identifier) {
    if (identifier == null) {
      throw new IllegalArgumentException("Remove with null identifier is not supported");
    }
    this.identifierEvaluator = new AttributeEvaluator(identifier);
    this.wildcardAttributeEvaluator = new WildcardAttributeEvaluator(identifier);
  }

  protected abstract String getScopeName();

}
