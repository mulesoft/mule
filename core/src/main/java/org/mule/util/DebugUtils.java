/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import java.security.Provider;
import java.security.Security;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Useful for enumerating debug information about the current Java environment
 */
// @ThreadSafe
public final class DebugUtils
{

    /** Do not instanciate. */
    private DebugUtils ()
    {
        // no-op
    }

    /**
     * @return all available services types
     */
    public static String[] listSecurityServiceTypes()
    {
        Set result = new HashSet();

        // All all providers
        Provider[] providers = Security.getProviders();
        for (int i = 0; i < providers.length; i++)
        {
            // Get services provided by each provider
            Set keys = providers[i].keySet();
            for (Iterator it = keys.iterator(); it.hasNext();)
            {
                String key = (String) it.next();
                key = key.split(" ")[0];

                if (key.startsWith("Alg.Alias."))
                {
                    // Strip the alias
                    key = key.substring(10);
                }
                int ix = key.indexOf('.');
                result.add(key.substring(0, ix));
            }
        }
        return (String[]) result.toArray(new String[result.size()]);
    }

    /**
     * @return the available implementations for a service type
     */
    public static String[] listCryptoImplementations(String serviceType)
    {
        Set result = new HashSet();

        // All all providers
        Provider[] providers = Security.getProviders();
        for (int i = 0; i < providers.length; i++)
        {
            // Get services provided by each provider
            Set keys = providers[i].keySet();
            for (Iterator it = keys.iterator(); it.hasNext();)
            {
                String key = (String) it.next();
                key = key.split(" ")[0];

                if (key.startsWith(serviceType + "."))
                {
                    result.add(key.substring(serviceType.length() + 1));
                }
                else if (key.startsWith("Alg.Alias." + serviceType + "."))
                {
                    // This is an alias
                    result.add(key.substring(serviceType.length() + 11));
                }
            }
        }
        return (String[]) result.toArray(new String[result.size()]);
    }
}
