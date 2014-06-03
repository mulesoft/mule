/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.introspection.api;

/**
 * Discriminator to specify the type of extension. These types imply
 * different assumptions regarding the {@link org.mule.extensions.introspection.api.MuleExtension} behavior
 * and nature.
 * <p/>
 * TODO: For now I'm having this as singletons implementing the WKI pattern. I'm doing this
 * because I have a hunch that having the ability to easily add logic and implement patterns such as visitor
 * or double dispatch will come handy in the future. If that's not the case, this will turn into an enum.
 */
public abstract class MuleExtensionType
{

    public static final MuleExtensionType MODULE = ModuleExtensionType.INSTANCE;

    public static final MuleExtensionType CONNECTOR = ConnectorExtensionType.INSTANCE;

}
