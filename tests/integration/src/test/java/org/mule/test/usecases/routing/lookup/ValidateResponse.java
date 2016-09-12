/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.routing.lookup;

import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.I18nMessageFactory;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.util.IOUtils;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Throws an exception if the message does not contain "success". In the real world, we might use XPath to extract a particular
 * tag or error code based on the expected response message format.
 */
public class ValidateResponse extends AbstractTransformer {

  @Override
  protected Object doTransform(Object src, Charset encoding) throws TransformerException {
    String response = null;
    if (src instanceof InputStream) {
      response = IOUtils.toString((InputStream) src);
    } else if (src instanceof String) {
      response = (String) src;
    }

    if (response != null && response.contains("<ErrorStatus>Success</ErrorStatus>")) {
      return response;
    } else {
      throw new TransformerException(I18nMessageFactory.createStaticMessage("Invalid response from service: " + response));
    }
  }
}


