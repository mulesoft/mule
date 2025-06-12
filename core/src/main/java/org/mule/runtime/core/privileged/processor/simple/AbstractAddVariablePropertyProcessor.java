/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor.simple;

import static org.mule.runtime.api.config.MuleRuntimeFeature.SET_VARIABLE_WITH_NULL_VALUE;
import static org.mule.runtime.api.metadata.DataType.STRING;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.core.internal.util.attribute.AttributeEvaluator;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import java.nio.charset.Charset;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

public abstract class AbstractAddVariablePropertyProcessor<T> extends SimpleMessageProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAddVariablePropertyProcessor.class);

  private ExtendedExpressionManager expressionManager;

  private AttributeEvaluator identifierEvaluator;
  private String value;
  private AttributeEvaluator valueEvaluator;
  private Optional<DataType> returnType = empty();

  private StreamingManager streamingManager;

  @Inject
  FeatureFlaggingService featureFlaggingService;

  private ArtifactEncoding artifactEncoding;

  @Override
  public void initialise() throws InitialisationException {
    identifierEvaluator.initialize(expressionManager);
    valueEvaluator = new AttributeEvaluator(value, getReturnDataType());
    valueEvaluator.initialize(expressionManager);
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    String key = identifierEvaluator.resolveValue(event);
    if (key == null) {
      LOGGER.error("Setting Null variable keys is not supported, this entry is being ignored");
      return event;
    }

    TypedValue<T> typedValue = valueEvaluator.resolveTypedValue(event);

    if (!featureFlaggingService.isEnabled(SET_VARIABLE_WITH_NULL_VALUE) && typedValue.getValue() == null) {
      LOGGER.debug("Variable with key '{}', not found on message using '{}'."
          + " Since the value was marked optional, nothing was set on the message for this variable",
                   key, valueEvaluator.getRawValue());
      return removeProperty((PrivilegedEvent) event, key);
    }

    if (typedValue.getValue() != null) {
      typedValue = handleStreaming(typedValue, event, streamingManager);
    }

    return addProperty((PrivilegedEvent) event, key, typedValue.getValue(),
                       DataType.builder().type(typedValue.getDataType().getType()).mediaType(getMediaType(typedValue))
                           .charset(resolveEncoding(typedValue)).build());
  }

  protected TypedValue<T> handleStreaming(TypedValue<T> typedValue, CoreEvent event, StreamingManager streamingManager) {
    return typedValue;
  }

  private MediaType getMediaType(TypedValue<T> typedValue) {
    if (returnType.isPresent()) {
      return getReturnDataType().getMediaType();
    } else {
      return typedValue.getDataType().getMediaType();
    }
  }

  protected Charset resolveEncoding(Object src) {
    return getReturnDataType().getMediaType().getCharset().orElse(getEncoding(src));
  }

  private Charset getEncoding(Object src) {
    if (src instanceof Message) {
      return ((Message) src).getPayload().getDataType().getMediaType().getCharset()
          .orElse(artifactEncoding.getDefaultEncoding());
    } else {
      return artifactEncoding.getDefaultEncoding();
    }
  }

  /**
   * Adds the property with its value and dataType to a property or variables scope.
   *
   * @param event        event to which property is to be added
   * @param propertyName name of the property or variable to add
   * @param value        value of the property or variable to add
   * @param dataType     data type of the property or variable to add
   */
  protected abstract PrivilegedEvent addProperty(PrivilegedEvent event, String propertyName, T value, DataType dataType);

  /**
   * Removes the property from a property or variables scope.
   *
   * @param event        event to which property is to be removed
   * @param propertyName name of the property or variable to remove
   */
  protected abstract PrivilegedEvent removeProperty(PrivilegedEvent event, String propertyName);

  public void setIdentifier(String identifier) {
    if (StringUtils.isBlank(identifier)) {
      throw new IllegalArgumentException("Key cannot be blank");
    }
    this.identifierEvaluator = new AttributeEvaluator(identifier, STRING);
  }

  public void setValue(String value) {
    requireNonNull(value);
    this.value = value;
  }

  public void setReturnDataType(DataType type) {
    this.returnType = of(type);
  }

  public DataType getReturnDataType() {
    return returnType.orElse(DataType.OBJECT);
  }

  @Inject
  public void setStreamingManager(StreamingManager streamingManager) {
    this.streamingManager = streamingManager;
  }

  @Inject
  public void setExpressionManager(ExtendedExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
  }

  @Inject
  public void setArtifactEncoding(ArtifactEncoding artifactEncoding) {
    this.artifactEncoding = artifactEncoding;
  }
}
