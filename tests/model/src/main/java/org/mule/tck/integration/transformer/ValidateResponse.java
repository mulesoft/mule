/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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


