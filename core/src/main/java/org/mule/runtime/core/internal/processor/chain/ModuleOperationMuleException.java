/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.chain;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessageFactory;

/**
 * Similar to a {@link MuleException}, this class is intended to be created only when an exception from within a
 * {@link ModuleOperationMessageProcessorChainBuilder} is used, so that later on it can be treated differently.
 * <p/>
 * The use case for this class is when logging in an application, where instead of digging until the rootest {@link MuleException}
 * is found, we need to stop in the first occurrence of a <module-operation-chain />.
 *
 * @since 4.2
 */
public class ModuleOperationMuleException extends MuleException {

  public ModuleOperationMuleException(Throwable root) {
    super(I18nMessageFactory.createStaticMessage(root.getMessage()));
  }
}
