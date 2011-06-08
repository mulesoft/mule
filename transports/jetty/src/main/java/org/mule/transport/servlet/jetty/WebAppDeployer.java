//========================================================================
//$Id$
//Copyright 2006 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mule.transport.servlet.jetty;

import java.util.ArrayList;

import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HandlerContainer;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.log.Log;
import org.mortbay.resource.Resource;
import org.mortbay.util.URIUtil;

/**
 * A repackaged version of the Jetty WebAppDeployer which makes it possible to 
 * override the server classes of the WebAppContext;
 */
public class WebAppDeployer extends AbstractLifeCycle
{
    private HandlerContainer _contexts;
    private String _webAppDir;
    private String _defaultsDescriptor;
    private String[] _configurationClasses;
    private boolean _extract;
    private boolean _parentLoaderPriority;
    private boolean _allowDuplicates;
    private ArrayList _deployed;
    private String[] serverClasses;
    private String[] systemClasses;
    
    public String[] getConfigurationClasses()
    {
        return _configurationClasses;
    }

    public void setConfigurationClasses(String[] configurationClasses)
    {
        _configurationClasses=configurationClasses;
    }

    public HandlerContainer getContexts()
    {
        return _contexts;
    }

    public void setContexts(HandlerContainer contexts)
    {
        _contexts=contexts;
    }

    public String getDefaultsDescriptor()
    {
        return _defaultsDescriptor;
    }

    public void setDefaultsDescriptor(String defaultsDescriptor)
    {
        _defaultsDescriptor=defaultsDescriptor;
    }

    public boolean isExtract()
    {
        return _extract;
    }

    public void setExtract(boolean extract)
    {
        _extract=extract;
    }

    public boolean isParentLoaderPriority()
    {
        return _parentLoaderPriority;
    }

    public void setParentLoaderPriority(boolean parentPriorityClassLoading)
    {
        _parentLoaderPriority=parentPriorityClassLoading;
    }

    public String getWebAppDir()
    {
        return _webAppDir;
    }

    public void setWebAppDir(String dir)
    {
        _webAppDir=dir;
    }

    public boolean getAllowDuplicates()
    {
        return _allowDuplicates;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param allowDuplicates If false, do not deploy webapps that have already been deployed or duplicate context path
     */
    public void setAllowDuplicates(boolean allowDuplicates)
    {
        _allowDuplicates=allowDuplicates;
    }

    public String[] getServerClasses()
    {
        return serverClasses;
    }

    public void setServerClasses(String[] serverClasses)
    {
        this.serverClasses = serverClasses;
    }

    public String[] getSystemClasses()
    {
        return systemClasses;
    }

    public void setSystemClasses(String[] systemClasses)
    {
        this.systemClasses = systemClasses;
    }

    /* ------------------------------------------------------------ */
    /**
     * @throws Exception 
     */
    public void doStart() throws Exception
    {
        _deployed=new ArrayList();
        
        scan();
        
    }
    
    /* ------------------------------------------------------------ */
    /** Scan for webapplications.
     * 
     * @throws Exception
     */
    public void scan() throws Exception
    {
        if (_contexts==null)
            throw new IllegalArgumentException("No HandlerContainer");

        Resource r=Resource.newResource(_webAppDir);
        if (!r.exists())
            throw new IllegalArgumentException("No such webapps resource "+r);

        if (!r.isDirectory())
            throw new IllegalArgumentException("Not directory webapps resource "+r);

        String[] files=r.list();

        files: for (int f=0; files!=null&&f<files.length; f++)
        {
            String context=files[f];

            if (context.equalsIgnoreCase("CVS/")||context.equalsIgnoreCase("CVS")||context.startsWith("."))
                continue;

            Resource app=r.addPath(r.encode(context));

            if (context.toLowerCase().endsWith(".war")||context.toLowerCase().endsWith(".jar"))
            {
                context=context.substring(0,context.length()-4);
                Resource unpacked=r.addPath(context);
                if (unpacked!=null&&unpacked.exists()&&unpacked.isDirectory())
                    continue;
            }
            else if (!app.isDirectory())
                continue;

            if (context.equalsIgnoreCase("root")||context.equalsIgnoreCase("root/"))
                context=URIUtil.SLASH;
            else
                context="/"+context;
            if (context.endsWith("/")&&context.length()>0)
                context=context.substring(0,context.length()-1);

            // Check the context path has not already been added or the webapp itself is not already deployed
            if (!_allowDuplicates)
            {
                Handler[] installed=_contexts.getChildHandlersByClass(ContextHandler.class);
                for (int i=0; i<installed.length; i++)
                {
                    ContextHandler c=(ContextHandler)installed[i];
        
                    if (context.equals(c.getContextPath()))
                        continue files;

                    try
                    {
                        String path=null;
                        if (c instanceof WebAppContext)
                            path = Resource.newResource(((WebAppContext)c).getWar()).getFile().getAbsolutePath();
                        else if (c.getBaseResource()!=null)
                            path = c.getBaseResource().getFile().getAbsolutePath();

                        if (path!=null && path.equals(app.getFile().getAbsolutePath()))
                            continue files;
                    }
                    catch (Exception e)
                    {
                        Log.ignore(e);
                    }
                }
            }

            // create a webapp
            WebAppContext wah=null;
            if (_contexts instanceof ContextHandlerCollection && 
                WebAppContext.class.isAssignableFrom(((ContextHandlerCollection)_contexts).getContextClass()))
            {
                try
                {
                    wah=(WebAppContext)((ContextHandlerCollection)_contexts).getContextClass().newInstance();
                }
                catch (Exception e)
                {
                    throw new Error(e);
                }
            }
            else
            {
                wah=new WebAppContext();
            }
            
            // configure it
            wah.setContextPath(context);
            if (_configurationClasses!=null)
                wah.setConfigurationClasses(_configurationClasses);
            if (_defaultsDescriptor!=null)
                wah.setDefaultsDescriptor(_defaultsDescriptor);
            wah.setExtractWAR(_extract);
            wah.setWar(app.toString());
            wah.setParentLoaderPriority(_parentLoaderPriority);

            if (serverClasses != null)
            {
                wah.setServerClasses(serverClasses);
            }

            if (systemClasses != null)
            {
                wah.setSystemClasses(systemClasses);
            }
            
            // add it
            _contexts.addHandler(wah);
            _deployed.add(wah);
            
            if (_contexts.isStarted())
                _contexts.start();  // TODO Multi exception
        }
    }
    
    public void doStop() throws Exception
    {
        for (int i=_deployed.size();i-->0;)
        {
            ContextHandler wac = (ContextHandler)_deployed.get(i);
            wac.stop();// TODO Multi exception
        }
    }
}
