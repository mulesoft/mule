/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.annotations.example;

import java.util.Locale;

/**
 * A simple language service that will return the greeting for the specified locale
 */
public interface LanguageService
{
    public String getGreeting(Locale locale) throws LanguageNotSupportedException;
}
