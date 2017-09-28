/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;

import org.mule.runtime.config.api.XmlGathererErrorHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.xml.DocumentLoader;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Default implementation of {@link XmlGathererErrorHandler} which collects all errors, and on a fatal exception will
 * propagate an exception.
 * <p/>
 * If logging is enabled, it will also log all warnings, errors and fatal when encountered.
 * <p/>
 * Instances of this class are not reusable among several readings of {@link DocumentLoader#loadDocument(InputSource, EntityResolver, ErrorHandler, int, boolean)},
 * as it holds state of the exceptions that were gathered.
 *
 * @since 4.0
 */
public class DefaultXmlLoggerErrorHandler implements XmlGathererErrorHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultXmlLoggerErrorHandler.class);

  private List<SAXParseException> errors = new ArrayList<>();

  @Override
  public void warning(SAXParseException e) throws SAXException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Found a waring exception parsing document, message '%s'", e.toString()), e);
    }
  }

  @Override
  public void fatalError(SAXParseException e) throws SAXException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Found a fatal error exception parsing document, message '%s'", e.toString()), e);
    }
    throw e;
  }

  @Override
  public void error(SAXParseException e) throws SAXException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Found error exception parsing document, message '%s'", e.toString()), e);
    }
    errors.add(e);
  }

  @Override
  public List<SAXParseException> getErrors() {
    return errors;
  }
}
