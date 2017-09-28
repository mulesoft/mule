/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.util;

import static java.util.Collections.emptyList;

import java.util.List;

import org.mule.runtime.config.api.XmlGathererErrorHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * {@link XmlGathererErrorHandler} implementation that doesn't handle errors.
 *
 * @since 4.0
 */
public class NoOpXmlErrorHandler implements XmlGathererErrorHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(NoOpXmlErrorHandler.class);

  @Override
  public List<SAXParseException> getErrors() {
    return emptyList();
  }

  @Override
  public void warning(SAXParseException e) throws SAXException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("[WARNING] " + getMessage(e), e);
    }
  }

  @Override
  public void error(SAXParseException e) throws SAXException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("[ERROR] " + getMessage(e), e);
    }
  }

  @Override
  public void fatalError(SAXParseException e) throws SAXException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("[FATAL] " + getMessage(e), e);
    }
  }

  private String getMessage(SAXParseException e) {
    return "while parsing the XML at location [" + e.getLineNumber() + ":" + e.getColumnNumber() + "]";
  }

}
