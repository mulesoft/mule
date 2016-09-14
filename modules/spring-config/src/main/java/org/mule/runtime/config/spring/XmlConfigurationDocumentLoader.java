/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import org.mule.runtime.core.api.MuleRuntimeException;

import java.io.InputStream;

import org.springframework.beans.factory.xml.DelegatingEntityResolver;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Loads a mule configuration file into a {@link Document} object.
 *
 * @since 4.0
 */
public class XmlConfigurationDocumentLoader {

  /**
   * Indicates that XSD validation should be used (found no "DOCTYPE" declaration).
   */
  private static final int VALIDATION_XSD = 3;

  /**
   * Creates a {@link Document} from an {@link InputStream} with the required configuration
   * of a mule configuration file parsing.
   *
   * @param inputStream the input stream with the xml configuration content.
   * @return a new {@link Document} object with the provided content.
   */
  public Document loadDocument(InputStream inputStream) {
    try {
      Document document = new MuleDocumentLoader()
          .loadDocument(new InputSource(inputStream),
                        new DelegatingEntityResolver(Thread.currentThread().getContextClassLoader()), new DefaultHandler(),
                        VALIDATION_XSD, true);
      return document;
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

}
