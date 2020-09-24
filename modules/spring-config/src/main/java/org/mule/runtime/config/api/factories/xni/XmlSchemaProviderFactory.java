/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.factories.xni;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.config.api.xni.parser.XmlSchemaProvider;
import org.mule.runtime.config.internal.factories.xni.DefaultXmlSchemaProviderFactory;

/**
 * Factory object to create instances of {@link XmlSchemaProvider} that will be used to create
 * {@link com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool}.
 *
 * @since 4.4.0
 */
@NoImplement
public interface XmlSchemaProviderFactory {

  public static XmlSchemaProviderFactory getDefault() {
    return new DefaultXmlSchemaProviderFactory();
  }

  public XmlSchemaProvider create();
}
