/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.extension.api.introspection.exception.ExceptionEnricher;
import org.mule.runtime.extension.api.introspection.exception.ExceptionEnricherFactory;

public final class DefaultExceptionEnricherFactory implements ExceptionEnricherFactory {

  private final ExceptionEnricher enricher;

  public DefaultExceptionEnricherFactory(Class<? extends ExceptionEnricher> enricherType) {
    checkArgument(enricherType != null, "ExceptionEnricher type cannot be null");
    try {
      enricher = enricherType.newInstance();
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create ExceptionEnricher of type " + enricherType.getName()),
                                     e);
    }
  }

  @Override
  public ExceptionEnricher createEnricher() {
    return enricher;
  }
}
