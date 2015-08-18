/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.extension.annotations.Extension;

/**
 * This class infers the version of an extension from the MANIFEST.MF contained in it's JAR.
 * If this cannot be accomplished, a fallback method is executed.
 */
public abstract class VersionResolver
{

    public String resolveVersion(Class extensionType, Extension extension)
    {
        String version = extensionType.getPackage().getImplementationVersion();
        if (version == null)
        {
            version = fallback(extensionType);
        }
        if (version == null)
        {
            throw new MuleRuntimeException(createStaticMessage(String.format("Could not resolve version from MANIFEST.MF for extension %s", extension.name())));
        }
        return version;
    }

    /**
     * This method will be invoked when the default mechanism fails. It should return a proper version or null if it fails too.
     */
    public abstract String fallback(Class extensionType);
}
