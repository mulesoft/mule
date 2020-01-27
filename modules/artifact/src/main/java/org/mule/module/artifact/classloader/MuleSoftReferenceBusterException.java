/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * An exception that indicated that there were an error on busting soft references
 * 
 * @since 4.3.0
 */
public class MuleSoftReferenceBusterException extends MuleException {

  private static final long serialVersionUID = 3114378370421458493L;

  public static final String EXCEPTION_MESSAGE = "Error on busting references for '%s': '%s'";

  public MuleSoftReferenceBusterException(ClassLoader classLoader, Exception e) {
    this(createStaticMessage(EXCEPTION_MESSAGE, classLoader.getClass().getName(), e.getMessage()));
  }

  public MuleSoftReferenceBusterException(I18nMessage message) {
    super(message);
  }

}
