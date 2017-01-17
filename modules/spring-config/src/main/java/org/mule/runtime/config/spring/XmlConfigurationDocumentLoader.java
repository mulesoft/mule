/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.Optional.empty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.extension.ExtensionManager;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.xml.DocumentLoader;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

/**
 * Loads a mule configuration file into a {@link Document} object.
 * <p/>
 * If when loading the configuration one, or more, {@link ErrorHandler#error(SAXParseException)} are call, at the end of
 * {@link #loadDocument(Optional, String, InputStream)} will throw an exception containing all the errors.
 *
 * @see {@link #loadDocument(Optional, String, InputStream)}
 *
 * @since 4.0
 */
public class XmlConfigurationDocumentLoader {

  /**
   * Indicates that XSD validation should be used (found no "DOCTYPE" declaration).
   */
  private static final int VALIDATION_XSD = 3;

  private final XmlGathererErrorHandlerFactory xmlGathererErrorHandlerFactory;

  /**
   * Creates an {@link XmlConfigurationDocumentLoader} using the default {@link DefaultXmlGathererErrorHandlerFactory}
   * to instantiate {@link XmlGathererErrorHandler}s objects when executing the {@link #loadDocument(Optional, String, InputStream)}
   * method.
   * <p/>
   * Using the default constructor implies that all XSD validations will take place and, at the end of the parsing, all
   * the errors will be contained in an {@link MuleRuntimeException} which will be thrown.
   */
  public XmlConfigurationDocumentLoader() {
    this(new DefaultXmlGathererErrorHandlerFactory());
  }

  /**
   * Creates an {@link XmlConfigurationDocumentLoader} using a parametrized {@link XmlGathererErrorHandlerFactory}
   * to instantiate {@link XmlGathererErrorHandler}s objects when executing the {@link #loadDocument(Optional, String, InputStream)}
   * method.
   * <p/>
   * Depending on what type of {@link XmlGathererErrorHandler} the factory returns, the {@link #loadDocument(Optional, String, InputStream)}
   * method will not thrown any exception if {@link XmlGathererErrorHandler#getErrors()} is an empty list.
   *
   * @param xmlGathererErrorHandlerFactory to create {@link XmlGathererErrorHandler} in the {@link #loadDocument(Optional, String, InputStream)}
   */
  public XmlConfigurationDocumentLoader(XmlGathererErrorHandlerFactory xmlGathererErrorHandlerFactory) {
    this.xmlGathererErrorHandlerFactory = xmlGathererErrorHandlerFactory;
  }

  /**
   * Creates a {@link Document} from an {@link InputStream} with the required configuration
   * of a mule configuration file parsing.
   *
   * @param filename name of the file to display a better error messages (if there are any). Non null.
   * @param inputStream the input stream with the XML configuration content.
   * @return a new {@link Document} object with the provided content.
   * @throws MuleRuntimeException if an error occurs in {@link DocumentLoader} factory, or if the current {@code filename}
   * contains 1 or more errors.
   * @see {@link DefaultXmlLoggerErrorHandler#getErrors()}
   */
  public Document loadDocument(String filename, InputStream inputStream) {
    return loadDocument(empty(), filename, inputStream);
  }

  /**
   * Creates a {@link Document} from an {@link InputStream} with the required configuration
   * of a mule configuration file parsing.
   *
   * @param extensionManager if the current {@code inputStream} relies in other schemas pending to be loaded from an
   * @param filename name of the file to display a better error messages (if there are any). Non null.
   * @param inputStream the input stream with the XML configuration content.
   * @return a new {@link Document} object with the provided content.
   * @throws MuleRuntimeException if an error occurs in {@link DocumentLoader} factory, or if the current {@code filename}
   * contains 1 or more errors.
   * @see {@link DefaultXmlLoggerErrorHandler#getErrors()}
   */
  public Document loadDocument(Optional<ExtensionManager> extensionManager, String filename, InputStream inputStream) {
    final XmlGathererErrorHandler errorHandler = xmlGathererErrorHandlerFactory.create();
    Document document;
    try {
      document = new MuleDocumentLoader()
          .loadDocument(new InputSource(inputStream),
                        new ModuleDelegatingEntityResolver(extensionManager), errorHandler,
                        VALIDATION_XSD, true);
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
    throwExceptionIfErrorsWereFound(errorHandler, filename);
    return document;
  }

  private void throwExceptionIfErrorsWereFound(XmlGathererErrorHandler errorHandler, String filename) {
    final List<SAXParseException> errors = errorHandler.getErrors();
    if (!errors.isEmpty()) {
      final String subMessage = format(errors.size() == 1 ? "was '%s' error" : "were '%s' errors", errors.size());
      final StringBuilder sb =
          new StringBuilder(format("There %s while parsing the file '%s'.", subMessage, filename));
      sb.append(lineSeparator()).append("Full list:");
      errors.stream().forEach(error -> sb.append(lineSeparator()).append(error));
      sb.append(lineSeparator());
      throw new MuleRuntimeException(createStaticMessage(sb.toString()));
    }
  }
}
