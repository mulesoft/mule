/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config;

import org.mule.tck.size.SmallTest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

@SmallTest
public class MuleManifestTestCase
{

    @Test
    public void getCoreManifest() throws Exception
    {
        MuleManifest.UrlPrivilegedAction action = new MuleManifest.UrlPrivilegedAction();
        URL url = action.getManifestJarURL(getUrlsEnum("mule-bas-3.jar", "mule-core-3.jar", "mule-foo-3.jar"));
        Assert.assertTrue(url.toExternalForm().contains("mule-core-3.jar"));
    }

    @Test
    public void getCoreEeManifest() throws Exception
    {
        MuleManifest.UrlPrivilegedAction action = new MuleManifest.UrlPrivilegedAction();
        URL url = action.getManifestJarURL(getUrlsEnum("mule-bas-3.jar", "mule-core-3.jar", "mule-core-ee-3.jar", "mule-foo-3.jar"));
        Assert.assertTrue(url.toExternalForm().contains("mule-core-ee-3.jar"));
    }

    @Test
    public void getEmbeddedManifest() throws Exception
    {
        MuleManifest.UrlPrivilegedAction action = new MuleManifest.UrlPrivilegedAction();
        URL url = action.getManifestJarURL(getUrlsEnum("mule-bas-3.jar", "mule-3-embedded.jar", "mule-foo-3.jar"));
        Assert.assertTrue(url.toExternalForm().contains("mule-3-embedded.jar"));
    }

    private Enumeration<URL> getUrlsEnum(String... names) throws MalformedURLException
    {
        List<URL> urls = new ArrayList<URL>();
        for (String name : names)
        {
            urls.add(new URL("file://" + name));
        }
        return Collections.enumeration(urls);
    }
}
