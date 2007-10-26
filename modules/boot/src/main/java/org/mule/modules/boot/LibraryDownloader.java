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

import org.mule.util.ClassUtils;
import org.mule.util.FileUtils;
import org.mule.util.NumberUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

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
    private static final int HTTP_CONNECTION_TIMEOUT = 10000;
    private static final String REPO_CENTRAL = "http://repo1.maven.org/maven2";
    private static final String BOOTSTRAP_LIBRARY_DOWNLOAD_DESCRIPTION_PREFIX = "mule.bootstrap.library.download.description.";

    private static final String PROXY_ERROR_MESSAGE = "Unable to reach remote repository. This is most likely because you are behind a firewall and missing proxy settings in Mule."
        + "Proxy options can be passed during startup as follows:\n"
        + " -M-Dhttp.proxyHost=YOUR_HOST\n"
        + " -M-Dhttp.proxyPort=YOUR_PORT\n"
        + " -M-Dhttp.proxyUsername=YOUR_USERNAME\n"
        + " -M-Dhttp.proxyPassword=YOUR_PASSWORD\n";
    
    private MuleBootstrapUtils.ProxyInfo proxyInfo;
    private HostConfiguration hostConfig;
    private HttpState httpState;

    private File muleHome;
    private File mavenRepo = null;
    private HttpClient client;
    
    private List libraryDownloadDescriptions = new ArrayList();

    public LibraryDownloader(File muleHome)
    {
        this(muleHome, new MuleBootstrapUtils.ProxyInfo(
            System.getProperty("http.proxyHost"), 
            System.getProperty("http.proxyPort"), 
            System.getProperty("http.proxyUsername"), 
            System.getProperty("http.proxyPassword")));
    }
    
    public LibraryDownloader(File muleHome, String proxyHost, String proxyPort, String proxyUsername, String proxyPassword)
    {
        this(muleHome, new MuleBootstrapUtils.ProxyInfo(proxyHost, proxyPort, proxyUsername, proxyPassword));
    }
    
    public LibraryDownloader(File muleHome, MuleBootstrapUtils.ProxyInfo proxyInfo)
    {
        this.muleHome = muleHome;
        this.proxyInfo = proxyInfo;

        configureMavenRepository();
        configureHttpClient();
        configureHttpProxy();
        
        readLibraryDownloadDescriptions();
    }
    
    public List downloadLibraries() throws IOException
    {
        List libraryUrls = new ArrayList();
        Exception proxyException = null;
        try
        {
            Iterator iter = libraryDownloadDescriptions.iterator();
            while (iter.hasNext())
            {
                URL libraryUrl = downloadLibrary((Library) iter.next());
                if (libraryUrl != null)
                {
                    libraryUrls.add(libraryUrl);
                }
            }
        }
        catch (UnknownHostException uhe)
        {
            proxyException = uhe;
        }
        catch (ConnectTimeoutException cte)
        {
            proxyException = cte;
        }
        finally
        {
            if (proxyException != null)
            {
                System.err.println();
                IOException ex = new IOException(PROXY_ERROR_MESSAGE);
                ex.initCause(proxyException);
                throw ex;
            }
        }
        return libraryUrls;        
    }
    
    private void configureMavenRepository()
    {
        String mavenRepoVar = System.getProperty("m2.repo");
        if (!StringUtils.isBlank(mavenRepoVar))
        {
            mavenRepo = new File(mavenRepoVar).getAbsoluteFile();
            if (!mavenRepo.exists() || !mavenRepo.isDirectory())
            {
                mavenRepo = null;
            }
        }
    }

    private void configureHttpClient()
    {
        client = new HttpClient();
        HttpConnectionManagerParams connParams = new HttpConnectionManagerParams();
        connParams.setConnectionTimeout(HTTP_CONNECTION_TIMEOUT);
        client.getHttpConnectionManager().setParams(connParams);
    }
    
    private void configureHttpProxy()
    {
        hostConfig = new HostConfiguration();
        if (StringUtils.isNotBlank(proxyInfo.host))
        {
            hostConfig.setProxy(proxyInfo.host, NumberUtils.toInt(proxyInfo.port));
        }
        httpState = new HttpState();
        if (StringUtils.isNotBlank(proxyInfo.username))
        {
            httpState.setProxyCredentials(new AuthScope(null, -1, null, null),
                new UsernamePasswordCredentials(proxyInfo.username, proxyInfo.password));
        }
    }
    
    private void readLibraryDownloadDescriptions()
    {
        Properties properties = System.getProperties();
        Enumeration keys = properties.keys();
        
        while (keys.hasMoreElements())
        {
            String key = (String) keys.nextElement();
            if (key.startsWith(BOOTSTRAP_LIBRARY_DOWNLOAD_DESCRIPTION_PREFIX))
            {
                String[] descriptions = properties.getProperty(key).split(",");
                if (descriptions != null && descriptions.length == 3)
                {
                    libraryDownloadDescriptions.add(new Library(descriptions[0].trim(), descriptions[1].trim(), descriptions[2].trim()));
                }
            }
        }
    }
    
    private URL downloadLibrary(Library library) throws IOException
    {
        URL libraryUrl = null;
        if (! ClassUtils.isClassOnPath(library.testClassName, MuleBootstrapUtils.class))
        {
            libraryUrl = downloadLibraryFromLocalRepository(library.jarPath, library.jarName);
            if (libraryUrl == null)
            {
                libraryUrl = downloadLibraryFromRemoteRepository(library.jarPath, library.jarName);
            }            
        }
        return libraryUrl;
    }

    private URL downloadLibraryFromLocalRepository(String path, String destinationFileName) throws IOException
    {
        URL libraryUrl = null;
        if (mavenRepo != null)
        {
            File sourceFile = new File(mavenRepo, path + File.separator + destinationFileName).getCanonicalFile();
            if (sourceFile.exists())
            {
                System.out.print("Copying from local repository " + sourceFile.getAbsolutePath() + " ...");
                File destinationFile = new File(new File(muleHome, DefaultMuleClassPathConfig.USER_DIR).getCanonicalFile(), destinationFileName).getCanonicalFile();
                FileUtils.copyFile(sourceFile, destinationFile);
                System.out.println("done");
                libraryUrl = destinationFile.toURL();
            }
        }
        return libraryUrl;
    }

    private URL downloadLibraryFromRemoteRepository(String path, String destinationFileName) throws IOException
    {
        URL libraryUrl = null;
        HttpMethod httpMethod = new GetMethod(REPO_CENTRAL + path + '/' + destinationFileName);
        try
        {
            System.out.print("Downloading " + httpMethod.getURI() + " ...");
            client.executeMethod(hostConfig, httpMethod, httpState);
            if (httpMethod.getStatusCode() == HttpStatus.SC_OK)
            {
                File destinationFile = new File(new File(muleHome, DefaultMuleClassPathConfig.USER_DIR), destinationFileName);
                FileUtils.copyStreamToFile(httpMethod.getResponseBodyAsStream(), destinationFile);
                System.out.println("done");
                libraryUrl = destinationFile.toURL();
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
        return libraryUrl;
    }
    
    private static class Library
    {
        public String testClassName;
        public String jarPath;
        public String jarName;
        
        public Library(String testClassName, String jarPath, String jarName)
        {
            this.testClassName = testClassName;
            this.jarPath = jarPath;
            this.jarName = jarName;
        }
    }
}
