/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;

import java.io.InputStream;

/**
 * Indicates that an {@link InputStream} or {@link CursorStreamProvider} was expected
 * but something else was found.
 *
 * @since 4.0
 */
public class NotAnInputStreamException extends MuleException {

  public NotAnInputStreamException(Object value) {
    super(createStaticMessage("Was expecting an InputStream or a stream provider but " +
        (value != null ? value.getClass().getName() : "null") +
        " was found instead"));
  }
}
