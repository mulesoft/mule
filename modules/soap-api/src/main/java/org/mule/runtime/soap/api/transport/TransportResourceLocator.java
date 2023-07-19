/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.soap.api.transport;

import org.mule.runtime.soap.api.SoapService;
import java.io.InputStream;

/**
 * This interface acts as an additional layer of indirection between a the WSDL fetching and the WSDL parsing by the
 * {@link SoapService}.
 * <p>
 * It enables the retrieval of WSDL and XSD documents that are protected somehow.
 *
 * @since 4.0
 */
public interface TransportResourceLocator {

  /**
   * Given the external document url this method checks if the document can be retrieved by this {@link TransportResourceLocator}
   * or not.
   *
   * @param url the document's url
   * @return whether it can retrieve the document file or not.
   */
  boolean handles(String url);

  /**
   * Retrieves a document's content.
   *
   * @param url the document's url
   * @return an {@link InputStream} representing the document's content.
   */
  InputStream getResource(String url);
}
