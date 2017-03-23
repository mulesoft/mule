/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.xml.transformer;

import javax.xml.transform.Result;

/**
 * A result type which delays writing until something further down stream can setup the underlying output result.
 */
public interface DelayedResult extends Result {

  void write(Result result) throws Exception;
}
