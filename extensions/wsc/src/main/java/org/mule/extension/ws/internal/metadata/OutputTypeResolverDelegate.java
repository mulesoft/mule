/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.metadata;

import javax.wsdl.BindingOperation;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.extensions.ElementExtensible;

/**
 * Output implementation of {@link TypeResolverDelegate}.
 * <p>
 * This class is responsible for returning the information of the messages that are used to resolve OUTPUT metadata.
 *
 * @since 4.0
 */
public class OutputTypeResolverDelegate implements TypeResolverDelegate {

  @Override
  public Message getMessage(Operation operation) {
    return operation.getOutput().getMessage();
  }

  @Override
  public ElementExtensible getBindingType(BindingOperation operation) {
    return operation.getBindingOutput();
  }
}
