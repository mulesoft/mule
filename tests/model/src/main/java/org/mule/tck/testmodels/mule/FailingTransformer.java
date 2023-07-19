/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.testmodels.mule;

import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.core.api.transformer.AbstractTransformer;

import java.nio.charset.Charset;

public class FailingTransformer extends AbstractTransformer {

  @Override
  protected Object doTransform(Object src, Charset encoding) throws TransformerException {
    throw new TransformerException(I18nMessageFactory.createStaticMessage("Failure"));
  }
}
