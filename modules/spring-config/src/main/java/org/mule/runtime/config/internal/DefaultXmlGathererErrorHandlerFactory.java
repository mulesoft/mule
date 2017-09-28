/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.config.api.XmlGathererErrorHandler;
import org.mule.runtime.config.api.XmlGathererErrorHandlerFactory;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * Default implementation of {@link XmlGathererErrorHandlerFactory} which will return the {@link DefaultXmlLoggerErrorHandler}
 * instance that registers all errors when {@link ErrorHandler#error(SAXParseException)} is called, to then return the
 * complete gathered list of exceptions through {@link XmlGathererErrorHandler#getErrors()} method.
 *
 * @since 4.0
 */
public class DefaultXmlGathererErrorHandlerFactory implements XmlGathererErrorHandlerFactory {

  @Override
  public XmlGathererErrorHandler create() {
    return new DefaultXmlLoggerErrorHandler();
  }
}
