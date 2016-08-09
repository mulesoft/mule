/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * This class is just a workaround for MULE-9865. Should be deleted as soon as that issue is resolved. Do not user since we will
 * not wait to Mule 5.0 to delete it
 *
 * @since 3.7.0
 */
@Deprecated
public class ExtensionNamespaceHandler extends NamespaceHandlerSupport {

  @Override
  public void init() {}

}
