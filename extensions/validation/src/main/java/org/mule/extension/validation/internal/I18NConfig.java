/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import static org.apache.commons.lang.StringUtils.EMPTY;
import org.mule.extension.annotations.Parameter;
import org.mule.extension.annotations.param.Optional;

/**
 * A simple object to configure internationalization.
 *
 * @since 3.7.0
 */
public final class I18NConfig
{

    /**
     * The path to a bundle file containing the messages.
     * If {@code null} then the platform will choose a default one
     */
    @Parameter
    private String bundlePath;

    /**
     * The locale of the {@link #bundlePath}. If {@code null}
     * the platform will choose the system default
     */
    @Parameter
    @Optional(defaultValue = EMPTY)
    private String locale;

    public String getBundlePath()
    {
        return bundlePath;
    }

    public String getLocale()
    {
        return locale;
    }
}
