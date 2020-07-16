/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static java.lang.String.format;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.exception.ErrorTypeMatcher;
import org.mule.runtime.core.api.exception.SingleErrorTypeMatcher;
import org.mule.runtime.core.internal.exception.EnrichedErrorMapping;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;

import javax.inject.Inject;


public class EnrichedErrorMappingsFactoryBean extends AbstractComponentFactory<EnrichedErrorMapping> {

  public static final String CORE_ERROR_NS = CORE_PREFIX.toUpperCase();

  @Inject
  private ErrorTypeRepository errorTypeRepository;

  private String source;
  private String target;

  @Override
  public EnrichedErrorMapping doGetObject() throws Exception {
    ComponentIdentifier sourceType = buildFromStringRepresentation(source);

    ErrorType errorType = errorTypeRepository
        .lookupErrorType(sourceType)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not find error '%s'.",
                                                                        source)));

    ErrorTypeMatcher errorTypeMatcher =
        new SingleErrorTypeMatcher(errorType);
    ErrorType targetValue = resolveErrorType(target);
    return new EnrichedErrorMapping(errorTypeMatcher, targetValue);
  }

  private ErrorType resolveErrorType(String representation) {
    ComponentIdentifier errorIdentifier = parserErrorType(representation);
    if (CORE_ERROR_NS.equals(errorIdentifier.getNamespace())) {
      return errorTypeRepository.lookupErrorType(errorIdentifier)
          .orElseThrow(() -> new MuleRuntimeException(createStaticMessage(format("There's no MULE error named '%s'.",
                                                                                 errorIdentifier.getName()))));
    }
    return errorTypeRepository.lookupErrorType(errorIdentifier)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not find synthetic error '%s' in registry",
                                                                        errorIdentifier)));
  }

  public static ComponentIdentifier parserErrorType(String representation) {
    int separator = representation.indexOf(':');
    String namespace;
    String identifier;
    if (separator > 0) {
      namespace = representation.substring(0, separator).toUpperCase();
      identifier = representation.substring(separator + 1).toUpperCase();
    } else {
      namespace = CORE_ERROR_NS;
      identifier = representation.toUpperCase();
    }

    return ComponentIdentifier.builder().namespace(namespace).name(identifier).build();
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

}
