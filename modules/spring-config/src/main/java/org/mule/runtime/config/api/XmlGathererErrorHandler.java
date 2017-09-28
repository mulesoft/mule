/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * Represents a specific type of {@link ErrorHandler} which gathers as many errors as possible to be displayed later for
 * either logging purposes or to propagate an exception with the full list of errors.
 * <p/>
 * Any implementation must be careful on how to treat the {@link ErrorHandler#fatalError(SAXParseException)} method, as
 * if the exception is not propagated immediately, breaking the current file parsing, the state of the DOM is in most
 * of the cases unusable.
 *
 * @see {@link XmlConfigurationDocumentLoader#loadDocument(Optional, String, InputStream)}
 *
 * @since 4.0
 */
public interface XmlGathererErrorHandler extends ErrorHandler {

  /**
   * @return a collection with all the {@link SAXParseException} exceptions gathered from {@link ErrorHandler#error(SAXParseException)}.
   * <p/>
   * An empty list means there were no error while parsing the file. Non null.
   */
  List<SAXParseException> getErrors();
}
