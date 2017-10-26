/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.integration.transformer;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.api.transformer.AbstractTransformer;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Throws an exception if the message does not contain "success".
 */
public class ValidateResponse extends AbstractTransformer {

  @Override
  protected Object doTransform(Object src, Charset encoding) throws TransformerException {
    String response = null;
    if (src instanceof String) {
      response = (String) src;
    } else if (src instanceof InputStream) {
      response = IOUtils.toString((InputStream) src);
    }

    if (response != null && response.contains("success")) {
      return response;
    } else {
      throw new TransformerException(createStaticMessage("Invalid response from flow: " + response));
    }
  }
}


