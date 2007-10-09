/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.object;

import org.mule.config.i18n.CoreMessages;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;

import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JndiObjectFactory implements ObjectFactory, Initialisable, Disposable
{
    /**
     * If true, the object is looked up from JNDI each time create() is called, otherwise it
     * is looked up once and stored locally.  Default value is false.
     */
    private boolean lookupOnEachCall = false;
    
    private String objectName;

    private String initialFactory;

    private String url;

    private Map properties;
    
    private Context _context;
    
    private Object _object;

    protected final Log logger = LogFactory.getLog(getClass());
    
    public JndiObjectFactory()
    {
        // for IoC only
    }
    
    public JndiObjectFactory(String objectName, String initialFactory, String url)
    {
        this(objectName, initialFactory, url, null);
    }
    
    public JndiObjectFactory(String objectName, String initialFactory, String url, Map properties)
    {
        this.objectName = objectName;
        this.initialFactory = initialFactory;
        this.url = url;
        this.properties = properties;
    }
    
    public void initialise() throws InitialisationException
    {
        if (_context == null)
        {
            Hashtable props = new Hashtable();

            if (initialFactory != null)
            {
                props.put(Context.INITIAL_CONTEXT_FACTORY, initialFactory);
            }
            else if (properties == null
                    || !properties.containsKey(Context.INITIAL_CONTEXT_FACTORY))
            {
                throw new InitialisationException(CoreMessages.objectIsNull("jndiInitialFactory"), this);
            }

            if (url != null)
            {
                props.put(Context.PROVIDER_URL, url);
            }

            if (properties != null)
            {
                props.putAll(properties);
            }
            
            try
            {
                _context = new InitialContext(props);
            }
            catch (NamingException e)
            {
                throw new InitialisationException(e, this);
            }
        }
    }
    
    public void dispose() 
    {
        if (_context != null)
        {
            try
            {
                _context.close();
            }
            catch (NamingException e)
            {
                logger.error("JNDI Context failed to dispose properly: ", e);
            }
            finally
            {
                _context = null;
            }
        }
    }
    
    public Object getOrCreate() throws Exception
    {
        if (_object == null || lookupOnEachCall == true)
        {
            _object = _context.lookup(objectName);
        }    
        return _object;
    }
    
    /** {@inheritDoc} */
    public Object lookup(String id) throws Exception
    {
        throw new UnsupportedOperationException();
    }
    
    /** {@inheritDoc} */
    public void release(Object object) throws Exception
    {
        throw new UnsupportedOperationException();
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////
    // Getters and Setters
    ///////////////////////////////////////////////////////////////////////////////////////////    

    public String getInitialFactory()
    {
        return initialFactory;
    }

    public void setInitialFactory(String initialFactory)
    {
        this.initialFactory = initialFactory;
    }

    public boolean isLookupOnEachCall()
    {
        return lookupOnEachCall;
    }

    public void setLookupOnEachCall(boolean lookupOnEachCall)
    {
        this.lookupOnEachCall = lookupOnEachCall;
    }

    public String getObjectName()
    {
        return objectName;
    }

    public void setObjectName(String objectName)
    {
        this.objectName = objectName;
    }

    public Map getProperties()
    {
        return properties;
    }

    public void setProperties(Map properties)
    {
        this.properties = properties;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public Context getContext()
    {
        return _context;
    }

    protected void setContext(Context context)
    {
        this._context = context;
    }
}
