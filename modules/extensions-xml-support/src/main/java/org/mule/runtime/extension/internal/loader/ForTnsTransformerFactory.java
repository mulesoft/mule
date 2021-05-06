/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.vibur.objectpool.PoolObjectFactory;

public class ForTnsTransformerFactory implements PoolObjectFactory<Transformer> {

  private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();
  private static final String TRANSFORMATION_FOR_TNS_RESOURCE = "META-INF/transform_for_tns.xsl";

  @Override
  public Transformer create() {
    try (InputStream in = new BufferedInputStream(XmlExtensionLoaderDelegate.class.getClassLoader()
        .getResourceAsStream(TRANSFORMATION_FOR_TNS_RESOURCE))) {
      return TRANSFORMER_FACTORY.newTransformer(new StreamSource(in));
    } catch (TransformerException | IOException e) {
      throw new MuleRuntimeException(createStaticMessage(format("There was an issue creating the transformer to remove the content of the <body> element to generate an XSD")),
                                     e);
    }
  }

  @Override
  public boolean readyToTake(Transformer obj) {
    return true;
  }

  @Override
  public boolean readyToRestore(Transformer obj) {
    return true;
  }

  @Override
  public void destroy(Transformer obj) {
    obj.reset();
  }

}
