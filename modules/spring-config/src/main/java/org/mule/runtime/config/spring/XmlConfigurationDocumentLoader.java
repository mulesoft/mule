/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static java.lang.String.format;
import static java.util.Optional.empty;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.ExtensionManager;

import java.io.InputStream;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(XmlConfigurationDocumentLoader.class);

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

  /**
   * Creates a {@link Document} from an {@link InputStream} with the required configuration
   * of a mule configuration file parsing.
   *
   * @param extensionManager if the current {@code inputStream} relies in other schemas pending to be loaded from an
   * {@link ExtensionModel}, then it will delegate to the manager for the lookup on them.
   *
   * @param inputStream the input stream with the XML configuration content.
   * @return a new {@link Document} object with the provided content.
   *
   * @see ModuleDelegatingEntityResolver#getSchema(ExtensionModel)
   */
  public Document loadDocument(Optional<ExtensionManager> extensionManager, InputStream inputStream) {
    try {
      Document document = new MuleDocumentLoader()
          .loadDocument(new InputSource(inputStream),
                        new ModuleDelegatingEntityResolver(extensionManager), new MuleLoggerErrorHandler(),
                        VALIDATION_XSD, true);
      return document;
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  /**
   * helper class to gather all errors while applying the found XSDs for the current input stream
   */
  private static class MuleLoggerErrorHandler extends DefaultHandler {

    @Override
    public void error(SAXParseException e) throws SAXException {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Found exception parsing document, message '%s'", e.toString()), e);
      }
    }
  }
}
