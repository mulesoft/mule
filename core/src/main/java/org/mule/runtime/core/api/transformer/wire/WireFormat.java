/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transformer.wire;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.MuleContextAware;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;


public interface WireFormat extends MuleContextAware {

  Object read(InputStream is) throws MuleException;

  void write(OutputStream out, Object o, Charset encoding) throws MuleException;
}
