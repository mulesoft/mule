/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.DataType.fromType;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.transformer.AbstractTransformer;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Transforms instances of {@link LocalDateTime} and {@link LocalDate} into instances of
 * {@link Date}
 *
 * @since 4.0
 */
public class LocalDateToDate extends AbstractTransformer implements DiscoverableTransformer {

  /**
   * Give core transformers a slightly higher priority
   */
  private int priorityWeighting = DEFAULT_PRIORITY_WEIGHTING + 1;

  public LocalDateToDate() {
    registerSourceType(fromType(LocalDate.class));
    registerSourceType(fromType(LocalDateTime.class));
    setReturnDataType(fromType(Date.class));
  }

  @Override
  public Object doTransform(Object src, Charset outputEncoding) throws TransformerException {
    if (src instanceof LocalDateTime) {
      return Date.from(((LocalDateTime) src).atZone(ZoneId.systemDefault()).toInstant());
    } else if (src instanceof LocalDate) {
      return Date.from(((LocalDate) src).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    throw new TransformerException(createStaticMessage("Unexpected input type: " + src.getClass().getName()));
  }

  @Override
  public int getPriorityWeighting() {
    return priorityWeighting;
  }

  @Override
  public void setPriorityWeighting(int weighting) {
    priorityWeighting = weighting;
  }
}
