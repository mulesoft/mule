/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
