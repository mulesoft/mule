/*
 * $Id
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.boot;

import org.mule.util.FileUtils;
import org.mule.util.NumberUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;

public class LibraryDownloader
{
    static final int STARTUP_TIMEOUT = 120000;
    static final String REPO_CENTRAL = "http://repo1.maven.org/maven2";

    static String proxyHostname = System.getProperty("http.proxyHost");
    static String proxyPort = System.getProperty("http.proxyPort");
    static String proxyUsername = System.getProperty("http.proxyUsername");
    static String proxyPassword = System.getProperty("http.proxyPassword");

    // HTTP proxy configuration
    private static HostConfiguration hostConfig;
    private static HttpState httpState;

    private File muleHome;
    private File mavenRepo = null;
    private HttpClient client;

    public LibraryDownloader(File muleHome)
    {
        this.muleHome = muleHome;

        String mavenRepoVar = System.getProperty("m2.repo");
        if (!StringUtils.isBlank(mavenRepoVar))
        {
            mavenRepo = new File(mavenRepoVar).getAbsoluteFile();
            if (!mavenRepo.exists() || !mavenRepo.isDirectory())
            {
                mavenRepo = null;
            }
        }

        client = new HttpClient();
        // Set the connection timeout to 10 seconds.
        HttpConnectionManagerParams connParams = new HttpConnectionManagerParams();
        connParams.setConnectionTimeout(10000);
        client.getHttpConnectionManager().setParams(connParams);

        // Configure HTTP proxy support if needed.
        hostConfig = new HostConfiguration();
        if (StringUtils.isNotBlank(proxyHostname))
        {
            hostConfig.setProxy(proxyHostname, NumberUtils.toInt(proxyPort));
        }
        httpState = new HttpState();
        if (StringUtils.isNotBlank(proxyUsername))
        {
            httpState.setProxyCredentials(new AuthScope(null, -1, null, null),
                new UsernamePasswordCredentials(proxyUsername, proxyPassword));
        }
    }
    
    public LibraryDownloader(File muleHome, String proxyHostname, String proxyPort, String proxyUsername, String proxyPassword)
    {
        this.muleHome = muleHome;

        String mavenRepoVar = System.getProperty("m2.repo");
        if (!StringUtils.isBlank(mavenRepoVar))
        {
            mavenRepo = new File(mavenRepoVar).getAbsoluteFile();
            if (!mavenRepo.exists() || !mavenRepo.isDirectory())
            {
                mavenRepo = null;
            }
        }

        client = new HttpClient();
        // Set the connection timeout to 10 seconds.
        HttpConnectionManagerParams connParams = new HttpConnectionManagerParams();
        connParams.setConnectionTimeout(10000);
        client.getHttpConnectionManager().setParams(connParams);

        // Configure HTTP proxy support if needed.
        hostConfig = new HostConfiguration();
        if (StringUtils.isNotBlank(proxyHostname))
        {
            hostConfig.setProxy(proxyHostname, NumberUtils.toInt(proxyPort));
        }
        httpState = new HttpState();
        if (StringUtils.isNotBlank(proxyUsername))
        {
            httpState.setProxyCredentials(new AuthScope(null, -1, null, null),
                new UsernamePasswordCredentials(proxyUsername, proxyPassword));
        }
    }

    public List downloadLibraries() throws IOException
    {
        List libraries = new ArrayList();
        try
        {
            libraries.add(getLibrary(REPO_CENTRAL, "/javax/activation/activation/1.1/activation-1.1.jar",
                "activation-1.1.jar"));
            libraries.add(getLibrary(REPO_CENTRAL, "/javax/mail/mail/1.4/mail-1.4.jar", "mail-1.4.jar"));
            return libraries;
        }
        catch (UnknownHostException uhe)
        {
            System.err.println();
            IOException ex = new IOException(
                "Unable to reach a remote repository, this is most likely because you are behind a firewall and have not configured your HTTP proxy settings in $MULE_HOME/conf/wrapper.conf.");
            ex.initCause(uhe);
            throw ex;
        }
        catch (ConnectTimeoutException e)
        {
            System.err.println();
            IOException ex = new IOException(
                "Unable to reach a remote repository, this is most likely because you are behind a firewall and have not configured your HTTP proxy settings in $MULE_HOME/conf/wrapper.conf.");
            ex.initCause(e);
            throw ex;
        }
    }

    private URL getLibrary(String repository, String path, String destinationFileName) throws IOException
    {
        URL url = null;
        if (mavenRepo != null)
        {
            url = copyLibrary(path, destinationFileName);
        }
        if (url == null)
        {
            url = downloadLibrary(repository, path, destinationFileName);
        }
        return url;
    }

    private URL copyLibrary(String path, String destinationFileName) throws IOException
    {
        File sourceFile = new File(mavenRepo, path).getCanonicalFile();
        if (sourceFile.exists())
        {
            System.out.print("Copying from local repository " + sourceFile.getAbsolutePath() + " ...");
            File destinationFile = new File(new File(muleHome, DefaultMuleClassPathConfig.USER_DIR)
                .getCanonicalFile(), destinationFileName).getCanonicalFile();
            FileUtils.copyFile(sourceFile, destinationFile);
            System.out.println("done");
            return destinationFile.toURL();
        }
        else
            return null;
    }

    private URL downloadLibrary(String repository, String path, String destinationFileName)
        throws IOException
    {
        String url = repository + path;
        HttpMethod httpMethod = new GetMethod(url);
        try
        {
            System.out.print("Downloading " + url + " ...");
            client.executeMethod(hostConfig, httpMethod, httpState);
            if (httpMethod.getStatusCode() == HttpStatus.SC_OK)
            {
                File destinationFile = new File(new File(muleHome, DefaultMuleClassPathConfig.USER_DIR),
                    destinationFileName);
                FileUtils.copyStreamToFile(httpMethod.getResponseBodyAsStream(), destinationFile);
                System.out.println("done");
                return destinationFile.toURL();
            }
            else
            {
                System.out.println();
                throw new IOException("HTTP request failed: " + httpMethod.getStatusLine().toString());
            }
        }
        finally
        {
            httpMethod.releaseConnection();
        }
    }
}
