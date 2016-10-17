/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.metadata;

import javax.wsdl.BindingOperation;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.extensions.ElementExtensible;

public class InputTypeResolverDelegate implements TypeResolverDelegate {

  @Override
  public Message getMessage(Operation operation) {
    return operation.getInput().getMessage();
  }

  @Override
  public ElementExtensible getBindingType(BindingOperation operation) {
    return operation.getBindingInput();
  }
}
