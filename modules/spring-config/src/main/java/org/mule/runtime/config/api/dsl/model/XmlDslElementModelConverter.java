/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model;

import org.mule.runtime.config.internal.dsl.model.DefaultXmlDslElementModelConverter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides a way to convert any {@link DslElementModel} into a its XML {@link Element}
 * representation.
 *
 * @since 4.0
 */
public interface XmlDslElementModelConverter {

  /**
   * Provides a default implementation of the {@link XmlDslElementModelConverter}.
   * <p>
   * The required {@link Document} will be used for creating the {@link Element}s,
   * thus avoiding the need of deep importing the nodes after creation.
   * This {@link Document} will also be enriched with the {@code xmlns} alias declarations
   * in the root element, which removes the need of declaring the {@code xmlns} in each
   * {@link Element} and provides an overall cleaner serialization afterwards.
   *
   * @param owner the {@link Document} that will contain the converted {@link Element}
   * @return a default implementation of the {@link XmlDslElementModelConverter}
   */
  static XmlDslElementModelConverter getDefault(Document owner) {
    return new DefaultXmlDslElementModelConverter(owner);
  }

  /**
   * Converts the given {@link DslElementModel} into its XML {@link Element} representation,
   * populating the given {@code owner} {@link Document} with all the information required
   * in order for the returned {@link Element} to be correctly parsed.
   * <p>
   * This method will not append the returned {@link Element} to the {@code owner} {@link Document},
   * leaving its usage open to whom consumes this service.
   *
   * @param elementModel the {@link DslElementModel} to be converted
   * @return the XML {@link Element} representation of the given {@link DslElementModel}
   */
  Element asXml(DslElementModel elementModel);

}
