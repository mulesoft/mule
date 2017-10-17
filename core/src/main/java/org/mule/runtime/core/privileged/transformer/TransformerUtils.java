/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.transformer;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.mule.runtime.api.metadata.DataType.builder;
import static org.mule.runtime.api.metadata.MediaType.ANY;

import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public class TransformerUtils {

  private static Logger LOGGER = LoggerFactory.getLogger(AbstractTransformer.class);
  public static final String COMMA = ",";

  /**
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  public static void initialiseAllTransformers(List<Transformer> transformers) throws InitialisationException {
    if (transformers != null) {
      Iterator<Transformer> transformer = transformers.iterator();
      while (transformer.hasNext()) {
        (transformer.next()).initialise();
      }
    }
  }

  public static String toString(List<Transformer> transformers) {
    StringBuilder buffer = new StringBuilder();
    Iterator<Transformer> transformer = transformers.iterator();
    while (transformer.hasNext()) {
      buffer.append(transformer.next().toString());
      if (transformer.hasNext()) {
        buffer.append(" -> ");
      }
    }
    return buffer.toString();
  }

  public static Transformer firstOrNull(List<Transformer> transformers) {
    if (transformers != null && 0 != transformers.size()) {
      return transformers.get(0);
    } else {
      return null;
    }
  }

  /**
   * Builds a list of Transformers.
   *
   * @param names - a list of transformers separated by commands
   * @param muleContext the current muleContext. This is used to look up transformers in the registry
   * @return a list (possibly empty) of transformers or
   * @throws MuleException if any of the transformers cannot be found
   */
  public static List<Transformer> getTransformers(String names, MuleContext muleContext) throws MuleException {
    if (null != names) {
      List<Transformer> transformers = new LinkedList<>();
      StringTokenizer st = new StringTokenizer(names, COMMA);
      while (st.hasMoreTokens()) {
        String key = st.nextToken().trim();
        Transformer transformer = ((MuleContextWithRegistries) muleContext).getRegistry().lookupTransformer(key);

        if (transformer == null) {
          throw new DefaultMuleException(CoreMessages.objectNotRegistered("Transformer", key));
        }
        transformers.add(transformer);
      }
      return transformers;
    } else {
      return null;
    }
  }

  /**
   * Checks whether a given value is a valid output for a transformer.
   *
   * @param transformer the transformer used to validate
   * @param value the output value
   * @throws TransformerException if the output value is of a unexpected type.
   */
  public static void checkTransformerReturnClass(Transformer transformer, Object value) throws TransformerException {
    if (value == null
        && (transformer instanceof AbstractTransformer && ((AbstractTransformer) transformer).isAllowNullReturn())) {
      return;
    }

    if (transformer.getReturnDataType() != null) {
      DataType dt = DataType.fromObject(value);
      if (ANY.matches(dt.getMediaType())) { //To avoid getting an error because the DataType was constructed with a default mediaType
        dt = builder(dt).mediaType(transformer.getReturnDataType().getMediaType()).build();
      }
      if (!transformer.getReturnDataType().isCompatibleWith(dt)) {
        throw new TransformerException(CoreMessages.transformUnexpectedType(dt, transformer.getReturnDataType()), transformer);
      }
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("The transformed value is of expected type. Type is: " + ClassUtils.getSimpleName(value.getClass()));
    }
  }

  public static <T> Object transformToAny(T input, MuleContext muleContext, DataType... supportedTypes) {
    final DataType sourceType = DataType.fromType(input.getClass());
    Object transformedData = null;

    for (DataType supportedType : supportedTypes) {
      transformedData = attemptTransformation(sourceType, input, supportedType, muleContext);
      if (transformedData != null) {
        break;
      }
    }

    return transformedData;
  }

  private static <S, R> R attemptTransformation(DataType sourceDataType, S source, DataType resultDataType,
                                                MuleContext muleContext) {
    Transformer transformer;
    try {
      transformer = ((MuleContextWithRegistries) muleContext).getRegistry().lookupTransformer(sourceDataType, resultDataType);
    } catch (TransformerException e) {
      LOGGER.debug("Could not find a transformer from type {} to {}", sourceDataType.getType().getName(),
                   resultDataType.getType().getName());
      return null;
    }

    LOGGER.debug("Located transformer {} from type {} to type {}. Attempting transformation...", transformer.getName(),
                 sourceDataType.getType().getName(), resultDataType.getType().getName());

    try {
      return (R) transformer.transform(source);
    } catch (TransformerException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
                     String.format("Transformer %s threw exception while trying to transform an object of type %s into a %s",
                                   transformer.getName(), sourceDataType.getType().getName(), resultDataType.getType().getName()),
                     e);
      }

      return null;
    }
  }

  public static String generateTransformerName(Class<? extends Transformer> transformerClass, DataType returnType) {
    String transformerName = ClassUtils.getSimpleName(transformerClass);
    int i = transformerName.indexOf("To");
    if (i > 0 && returnType != null) {
      String target = ClassUtils.getSimpleName(returnType.getType());
      if (target.equals("byte[]")) {
        target = "byteArray";
      }
      transformerName = transformerName.substring(0, i + 2) + capitalize(target);
    }
    return transformerName;
  }
}
