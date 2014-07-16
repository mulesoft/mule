/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

public class ReloadControl
{

    // since java 6 only
    static class Always extends ResourceBundle.Control
    {
        boolean needsReload = true;

        @Override
        public boolean needsReload(String baseName, Locale locale, String format, ClassLoader loader, ResourceBundle bundle, long loadTime)
        {
            // don't cache, always reload
            return true;
        }

        @Override
        public long getTimeToLive(String baseName, Locale locale)
        {
            if (needsReload)
            {
                // must be zero, as other 'DONT_CACHE' constant doesn't work here, and is -1
                return 0;
            }

            return ResourceBundle.Control.TTL_NO_EXPIRATION_CONTROL;
        }
    }
}
