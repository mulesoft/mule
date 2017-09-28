/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api;

import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;

/**
 * Factory object to create instances of {@link XmlGathererErrorHandler} that will be used in the reading of XML files.
 *
 * @since 4.0
 */
public interface XmlGathererErrorHandlerFactory {

  /**
   * @return Creates an {@link XmlGathererErrorHandler} to be used when executing
   *         {@link org.springframework.beans.factory.xml.DocumentLoader#loadDocument(InputSource, EntityResolver, ErrorHandler, int, boolean)}
   */
  XmlGathererErrorHandler create();
}
