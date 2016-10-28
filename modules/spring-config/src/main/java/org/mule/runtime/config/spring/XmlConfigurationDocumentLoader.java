/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static java.lang.String.format;
import static java.util.Optional.empty;
import org.mule.runtime.config.spring.dsl.model.extension.loader.ModuleExtensionStore;
import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.InputStream;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
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
   * @param inputStream the input stream with the XML configuration content.
   * @return a new {@link Document} object with the provided content.
   */
  public Document loadDocument(InputStream inputStream) {
    return loadDocument(empty(), inputStream);
  }

  public Document loadDocument(Optional<ModuleExtensionStore> moduleExtensionStore, InputStream inputStream) {
    try {
      MuleLoggerErrorHandler errorHandler = new MuleLoggerErrorHandler();
      Document document = new MuleDocumentLoader()
          .loadDocument(new InputSource(inputStream),
                        new ModuleDelegatingEntityResolver(moduleExtensionStore), errorHandler,
                        VALIDATION_XSD, true);
      errorHandler.throwExceptionIfNeeded();
      return document;
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  /**
   * helper class to gather all errors while applying the found XSDs for the current input stream
   */
  private static class MuleLoggerErrorHandler extends DefaultHandler {

    StringBuilder sb = new StringBuilder();

    @Override
    public void error(SAXParseException e) throws SAXException {
      sb.append(format("\terror:%s\n", e.toString()));
    }

    public void throwExceptionIfNeeded() {
      String errors = sb.toString();
      if (StringUtils.isNotBlank(errors)) {
        String errorOrErrors = "Gathered errors:";
        throw new IllegalArgumentException(format(errorOrErrors + " \n %s", errors));
      }
    }
  }
}
