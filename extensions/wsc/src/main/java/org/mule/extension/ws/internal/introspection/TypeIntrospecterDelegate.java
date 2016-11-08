/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.introspection;

import javax.wsdl.BindingOperation;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.extensions.ElementExtensible;

/**
 * Contract for classes that returns information about the messages that need to be introspected for the resolvers in order to
 * obtain input or output metadata.
 *
 * @since 4.0
 */
public interface TypeIntrospecterDelegate {

  Message getMessage(Operation operation);

  ElementExtensible getBindingType(BindingOperation operation);
}
