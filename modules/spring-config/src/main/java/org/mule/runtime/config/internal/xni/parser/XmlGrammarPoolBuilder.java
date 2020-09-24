/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.xni.parser;

import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import org.mule.runtime.config.api.xni.parser.XmlGathererErrorHandler;
import org.mule.runtime.config.api.xni.parser.XmlSchemaProvider;

/**
 * Provides a way of creating {@link XMLGrammarPool} instances.
 *
 * @since 4.4.0
 */
public interface XmlGrammarPoolBuilder {

  /**
   * Instantiates a new raw builder, whose components will be declared in the provided {@code extensionModels}.
   *
   * @param schemaProvider provides {@link XMLInputSource} schemas to be loaded.
   * @param errorHandler a {@link XmlGathererErrorHandler} which gathers as many errors as possible.
   * @param entityResolver a {@link XMLEntityResolver} that resolve entities over mule schemas.
   * @return the newly created builder
   */
  public static XmlGrammarPoolBuilder builder(XmlSchemaProvider schemaProvider, XmlGathererErrorHandler errorHandler,
                                              XMLEntityResolver entityResolver) {
    return new DefaultXmlGrammarPoolBuilder(schemaProvider, errorHandler, entityResolver);
  }

  /**
   * Builds the target {@link XMLGrammarPool}
   *
   * @return the target {@link XMLGrammarPool}
   */
  XMLGrammarPool build();


}
