/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.xni.parser;

import com.sun.org.apache.xerces.internal.xni.parser.XMLErrorHandler;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import java.util.List;

/**
 * Represents a specific type of {@link XMLErrorHandler} which gathers as many errors as possible to be displayed later for either
 * logging purposes or to propagate an exception with the full list of errors.
 *
 * @since 4.4.0
 */
public interface XmlGathererErrorHandler extends XMLErrorHandler {

  /**
   * @return a collection with all the {@link XMLParseException} exceptions gathered from
   * {@link XMLErrorHandler#error(String, String, XMLErrorHandler)}.
   * <p/>
   * An empty list means there were no error while parsing the file. Non null.
   */
  List<XMLParseException> getErrors();
}
