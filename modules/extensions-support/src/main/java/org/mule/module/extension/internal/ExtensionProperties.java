/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import static org.mule.api.config.MuleProperties.PROPERTY_PREFIX;
import org.mule.extension.api.runtime.ContentMetadata;
import org.mule.extension.api.runtime.ContentType;

/**
 * Constants for the Extensions Framework
 *
 * @since 4.0
 */
public class ExtensionProperties
{

    private ExtensionProperties()
    {
    }

    /**
     * The key of an operation's variable on which the connection to be used was set
     */
    public static final String CONNECTION_PARAM = PROPERTY_PREFIX + "CONNECTION_PARAM";

    /**
     * The key of an operation's variable on which the corresponding {@link ContentMetadata} is available
     */
    public static final String CONTENT_METADATA = PROPERTY_PREFIX + "CONTENT_METADATA";

    /**
     * The key of an operation's variable on which the corresponding input {@link ContentType} is available
     */
    public static final String CONTENT_TYPE = PROPERTY_PREFIX + "CONTENT_TYPE";

    /**
     * The name of a parameter that allows configuring the mimeType that should be applied
     */
    public static final String MIME_TYPE_PARAMETER_NAME = "outputMimeType";

    /**
     * The name of a parameter that allows configuring the encoding that should be applied
     */
    public static final String ENCODING_PARAMETER_NAME = "outputEncoding";

}
