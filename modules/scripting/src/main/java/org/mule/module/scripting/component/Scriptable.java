/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting.component;

import org.mule.DefaultMuleEventContext;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.NullPayload;
import org.mule.util.CollectionUtils;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A JSR 223 Script service. Allows any JSR 223 compliant script engines such as JavaScript, Groovy or Rhino
 * to be embedded as Mule components.
 */
public class Scriptable implements Initialisable, MuleContextAware
{
    /** The actual body of the script */
    private String scriptText;

    /** A file from which the script will be loaded */
    private String scriptFile;

    /** Parameters to be made available to the script as variables */
    private Properties properties;

    /** The name of the JSR 223 scripting engine (e.g., "groovy") */
    private String scriptEngineName;

    // ///////////////////////////////////////////////////////////////////////////
    // Internal variables, not exposed as properties
    // ///////////////////////////////////////////////////////////////////////////

    /** A compiled version of the script, if the scripting engine supports it */
    private CompiledScript compiledScript;

    private ScriptEngine scriptEngine;
    private ScriptEngineManager scriptEngineManager;

    private MuleContext muleContext;

    protected transient Log logger = LogFactory.getLog(getClass());

    public Scriptable()
    {
        // For Spring
    }

    public Scriptable(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public void initialise() throws InitialisationException
    {
        scriptEngineManager = new ScriptEngineManager();

        // Create scripting engine
        if (scriptEngineName != null)
        {
            scriptEngine = createScriptEngineByName(scriptEngineName);
            if (scriptEngine == null)
            {
                throw new InitialisationException(
                    MessageFactory.createStaticMessage("Scripting engine '" + scriptEngineName
                                                       + "' not found.  Available engines are: "
                                                       + listAvailableEngines()), this);
            }
        }
        // Determine scripting engine to use by file extension
        else if (scriptFile != null)
        {
            int i = scriptFile.lastIndexOf(".");
            if (i > -1)
            {
                logger.info("Script Engine name not set. Guessing by file extension.");
                String ext = scriptFile.substring(i + 1);
                scriptEngine = createScriptEngineByExtension(ext);
                if (scriptEngine == null)
                {
                    throw new InitialisationException(
                        MessageFactory.createStaticMessage("File extension '"
                                                           + ext
                                                           + "' does not map to a scripting engine.  Available engines are: "
                                                           + listAvailableEngines()), this);
                }
                else
                {
                    setScriptEngineName(ext);
                }
            }
        }

        Reader script = null;
        try
        {
            // Load script from variable
            if (StringUtils.isNotBlank(scriptText))
            {
                script = new StringReader(scriptText);
            }
            // Load script from file
            else if (scriptFile != null)
            {
                InputStream is;
                try
                {
                    is = IOUtils.getResourceAsStream(scriptFile, getClass());
                }
                catch (IOException e)
                {
                    throw new InitialisationException(CoreMessages.cannotLoadFromClasspath(scriptFile), e, this);
                }
                if (is == null)
                {
                    throw new InitialisationException(CoreMessages.cannotLoadFromClasspath(scriptFile), this);
                }
                script = new InputStreamReader(is);
            }
            else
            {
                throw new InitialisationException(CoreMessages.propertiesNotSet("scriptText, scriptFile"), this);
            }

            // Pre-compile script if scripting engine supports compilation.
            if (scriptEngine instanceof Compilable)
            {
                try
                {
                    compiledScript = ((Compilable) scriptEngine).compile(script);
                }
                catch (ScriptException e)
                {
                    throw new InitialisationException(e, this);
                }
            }
        }
        finally
        {
            if (script != null)
            {
                try
                {
                    script.close();
                }
                catch (IOException e)
                {
                    throw new InitialisationException(e, this);
                }
            }
        }
    }

    public void populateDefaultBindings(Bindings bindings)
    {
        if (properties != null)
        {
            bindings.putAll((Map) properties);
        }
        bindings.put("log", logger);
        // A place holder for a returned result if the script doesn't return a result.
        // The script can overwrite this binding
        bindings.put("result", NullPayload.getInstance());
        bindings.put("muleContext", muleContext);
        bindings.put("registry", muleContext.getRegistry());
    }

    public void populateBindings(Bindings bindings, Object payload)
    {
        populateDefaultBindings(bindings);
        bindings.put("payload", payload);
        // For backward compatability. Usually used by the script transformer since
        // src maps with the argument passed into the transformer
        bindings.put("src", payload);
    }

    public void populateBindings(Bindings bindings, MuleMessage message)
    {
        populateDefaultBindings(bindings);
        if (message == null)
        {
            message = new DefaultMuleMessage(NullPayload.getInstance(), muleContext);
        }

        populateVariablesInOrder(bindings, message);

        bindings.put("message", message);
        // This will get overwritten if populateBindings(Bindings bindings, MuleEvent event) is called
        // and not this method directly.
        bindings.put("payload", message.getPayload());
        // For backward compatability
        bindings.put("src", message.getPayload());

        populateHeadersVariablesAndException(bindings, message);
    }

    public void populateBindings(Bindings bindings, MuleEvent event)
    {
        populateBindings(bindings, event.getMessage());
        bindings.put("originalPayload", event.getMessage().getPayload());
        bindings.put("payload", event.getMessage().getPayload());
        bindings.put("eventContext", new DefaultMuleEventContext(event));
        bindings.put("id", event.getId());
        bindings.put("flowConstruct", event.getFlowConstruct());
        if (event.getFlowConstruct() instanceof Service)
        {
            bindings.put("service", event.getFlowConstruct());
        }
    }

    private void populateHeadersVariablesAndException(Bindings bindings, MuleMessage message)
    {
        bindings.put("flowVars", new MesssagePropertyMap(message, PropertyScope.INVOCATION));
        bindings.put("sessionVars", new MesssagePropertyMap(message, PropertyScope.SESSION));

        // Only add exception is present
        if (message.getExceptionPayload() != null)
        {
            bindings.put("exception", message.getExceptionPayload().getException());
        }
        else
        {
            bindings.put("exception", null);
        }
    }

    private void populateVariablesInOrder(Bindings bindings, MuleMessage message)
    {
        for (String key : message.getSessionPropertyNames())
        {
            bindings.put(key, message.getSessionProperty(key));
        }
        for (String key : message.getInvocationPropertyNames())
        {
            bindings.put(key, message.getInvocationProperty(key));
        }
    }

    public Object runScript(Bindings bindings) throws ScriptException
    {
        Object result;
        try
        {
            RegistryLookupBindings registryLookupBindings = new RegistryLookupBindings(
                muleContext.getRegistry(), bindings);
            if (compiledScript != null)
            {
                result = compiledScript.eval(registryLookupBindings);
            }
            else
            {
                result = scriptEngine.eval(scriptText, registryLookupBindings);
            }

            // The result of the script can be returned directly or it can
            // be set as the variable "result".
            if (result == null)
            {
                result = registryLookupBindings.get("result");
            }
        }
        catch (ScriptException e)
        {
            // re-throw
            throw e;
        }
        catch (Exception ex)
        {
            throw new ScriptException(ex);
        }
        return result;
    }

    protected ScriptEngine createScriptEngineByName(String name)
    {
        return scriptEngineManager.getEngineByName(name);
    }

    protected ScriptEngine createScriptEngineByExtension(String ext)
    {
        return scriptEngineManager.getEngineByExtension(ext);
    }

    protected String listAvailableEngines()
    {
        return CollectionUtils.toString(scriptEngineManager.getEngineFactories(), false);
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Getters and setters
    // //////////////////////////////////////////////////////////////////////////////

    public String getScriptText()
    {
        return scriptText;
    }

    public void setScriptText(String scriptText)
    {
        this.scriptText = scriptText;
    }

    public String getScriptFile()
    {
        return scriptFile;
    }

    public void setScriptFile(String scriptFile)
    {
        this.scriptFile = scriptFile;
    }

    public void setScriptEngineName(String scriptEngineName)
    {
        this.scriptEngineName = scriptEngineName;
    }

    public String getScriptEngineName()
    {
        return scriptEngineName;
    }

    public Properties getProperties()
    {
        return properties;
    }

    public void setProperties(Properties properties)
    {
        this.properties = properties;
    }

    public ScriptEngine getScriptEngine()
    {
        return scriptEngine;
    }

    protected void setScriptEngine(ScriptEngine scriptEngine)
    {
        this.scriptEngine = scriptEngine;
    }

    protected CompiledScript getCompiledScript()
    {
        return compiledScript;
    }

    protected void setCompiledScript(CompiledScript compiledScript)
    {
        this.compiledScript = compiledScript;
    }

    private static class MesssagePropertyMap implements Map<String, Object>
    {
        MuleMessage message;
        PropertyScope propertyScope;

        public MesssagePropertyMap(MuleMessage message, PropertyScope propertyScope)
        {
            this.message = message;
            this.propertyScope = propertyScope;
        }

        @Override
        public void clear()
        {
            message.clearProperties(propertyScope);
        }

        @Override
        public boolean containsKey(Object key)
        {
            return message.getPropertyNames(propertyScope).contains(key);
        }

        @Override
        public boolean containsValue(Object value)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<java.util.Map.Entry<String, Object>> entrySet()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object get(Object key)
        {
            return message.getProperty((String) key, propertyScope);
        }

        @Override
        public boolean isEmpty()
        {
            return message.getPropertyNames(propertyScope).isEmpty();
        }

        @Override
        public Set<String> keySet()
        {
            return message.getPropertyNames(propertyScope);
        }

        @Override
        public Object put(String key, Object value)
        {
            if (PropertyScope.INBOUND.equals(propertyScope))
            {
                throw new UnsupportedOperationException("Inbound message properties are read-only");
            }
            else
            {
                message.setProperty(key, value, propertyScope);
                return value;
            }
        }

        @Override
        public void putAll(Map<? extends String, ? extends Object> m)
        {
            for (Map.Entry<? extends String, ? extends Object> entry : m.entrySet())
            {
                put(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public Object remove(Object key)
        {
            if (PropertyScope.INBOUND.equals(propertyScope))
            {
                throw new UnsupportedOperationException("Inbound message properties are read-only");
            }
            else
            {
                return message.removeProperty((String) key, propertyScope);
            }
        }

        @Override
        public int size()
        {
            return message.getPropertyNames(propertyScope).size();
        }

        @Override
        public Collection<Object> values()
        {
            throw new UnsupportedOperationException();
        }
    }

}
