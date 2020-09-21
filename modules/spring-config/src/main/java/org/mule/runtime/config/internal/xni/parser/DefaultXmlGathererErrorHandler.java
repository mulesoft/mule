/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.xni.parser;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime implementation of {@link XmlGathererErrorHandler} which collects all errors, and on a fatal exception will propagate an
 * exception.
 *
 * @since 4.4.0
 */
public class DefaultXmlGathererErrorHandler implements XmlGathererErrorHandler {

  private static final Logger LOGGER = getLogger(DefaultXmlGathererErrorHandler.class);

  private List<XMLParseException> errors = new ArrayList<>();

  @Override
  public void warning(String domain, String key, XMLParseException e) throws XNIException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Found warning parsing domain: %s, key: %s", domain, key), e);
    }
  }

  @Override
  public void error(String domain, String key, XMLParseException e) throws XNIException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Found error parsing domain: %s, key: %s", domain, key), e);
    }
    errors.add(e);
  }

  @Override
  public void fatalError(String domain, String key, XMLParseException e) throws XNIException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Found fatal error parsing domain: %s, key: %s", domain, key), e);
    }
    throw e;
  }

  @Override
  public List<XMLParseException> getErrors() {
    return errors;
  }
}
