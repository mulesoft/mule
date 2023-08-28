/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.util;

import static java.util.Collections.emptyList;

import java.util.List;

import org.mule.runtime.dsl.api.xml.parser.XmlGathererErrorHandler;

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
