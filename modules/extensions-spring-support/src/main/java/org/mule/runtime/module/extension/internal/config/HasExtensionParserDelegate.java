/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

/**
 * Contract interface for an object capable of receiving a {@link XmlExtensionParserDelegate}
 *
 * @since 4.0
 */
interface HasExtensionParserDelegate
{

    void setParserDelegate(XmlExtensionParserDelegate parserDelegate);
}
