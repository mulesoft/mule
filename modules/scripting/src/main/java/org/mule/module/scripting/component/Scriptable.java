/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting.component;

import static org.mule.api.transport.PropertyScope.INBOUND;
import static org.mule.api.transport.PropertyScope.INVOCATION;
import static org.mule.api.transport.PropertyScope.SESSION;
import static org.mule.config.i18n.CoreMessages.cannotLoadFromClasspath;
import static org.mule.config.i18n.CoreMessages.propertiesNotSet;
import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.util.IOUtils.getResourceAsStream;

import org.mule.DefaultMuleEventContext;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.NullPayload;
import org.mule.util.CollectionUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
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

    private static final String BINDING_LOG = "log";
    private static final String BINDING_RESULT = "result";
    private static final String BINDING_MULE_CONTEXT = "muleContext";
    private static final String BINDING_REGISTRY = "registry";
    private static final String BINDING_PAYLOAD = "payload";
    private static final String BINDING_SRC = "src";
    private static final String BINDING_MESSAGE = "message";
    private static final String BINDING_ORIGINAL_PAYLOAD = "originalPayload";
    private static final String BINDING_EVENT_CONTEXT = "eventContext";
    private static final String BINDING_ID = "id";
    private static final String BINDING_FLOW_CONSTRUCT = "flowConstruct";
    private static final String BINDING_SERVICE = "service";
    private static final String BINDING_FLOW_VARS = "flowVars";
    private static final String BINDING_SESSION_VARS = "sessionVars";
    private static final String BINDING_EXCEPTION = "exception";

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

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        scriptEngineManager = new ScriptEngineManager();

        // Create scripting engine
        if (scriptEngineName != null)
        {
            scriptEngine = createScriptEngineByName(scriptEngineName);
            if (scriptEngine == null)
            {
                throw new InitialisationException(createStaticMessage("Scripting engine '" + scriptEngineName
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
                String extension = scriptFile.substring(i + 1);
                scriptEngine = createScriptEngineByExtension(extension);
                if (scriptEngine == null)
                {
                    throw new InitialisationException(createStaticMessage("File extension '"
                                                                          + extension
                                                                          + "' does not map to a scripting engine.  Available engines are: "
                                                                          + listAvailableEngines()), this);
                }
                else
                {
                    setScriptEngineName(extension);
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
                    is = getResourceAsStream(scriptFile, getClass());
                }
                catch (IOException e)
                {
                    throw new InitialisationException(cannotLoadFromClasspath(scriptFile), e, this);
                }
                if (is == null)
                {
                    throw new InitialisationException(cannotLoadFromClasspath(scriptFile), this);
                }
                script = new InputStreamReader(is);
            }
            else
            {
                throw new InitialisationException(propertiesNotSet("scriptText, scriptFile"), this);
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

    protected void populatePropertyBindings(Bindings bindings)
    {
        if (properties != null)
        {
            bindings.putAll((Map) properties);
        }
    }

    /**
     * @deprecated This uses the deprecated method {@link ExpressionManager#parse(String, MuleMessage)} internally. Use
     *             {@link #populatePropertyBindings(Bindings, MuleEvent)} instead.
     */
    @Deprecated
    protected void populatePropertyBindings(Bindings bindings, MuleMessage message)
    {
        if (properties != null)
        {
            for (Entry entry : properties.entrySet())
            {
                String value = (String) entry.getValue();
                if (muleContext.getExpressionManager().isExpression(value))
                {
                    bindings.put((String) entry.getKey(), muleContext.getExpressionManager().parse(value, message));
                }
                else
                {
                    bindings.put((String) entry.getKey(), value);
                }
            }
        }
    }

    protected void populatePropertyBindings(Bindings bindings, MuleEvent event)
    {
        if (properties != null)
        {
            for (Entry entry : properties.entrySet())
            {
                String value = (String) entry.getValue();
                if (muleContext.getExpressionManager().isExpression(value))
                {
                    bindings.put((String) entry.getKey(), muleContext.getExpressionManager().parse(value, event));
                }
                else
                {
                    bindings.put((String) entry.getKey(), value);
                }
            }
        }
    }

    public void populateDefaultBindings(Bindings bindings)
    {
        bindings.put(BINDING_LOG, logger);
        // A place holder for a returned result if the script doesn't return a result.
        // The script can overwrite this binding
        bindings.put(BINDING_RESULT, NullPayload.getInstance());
        bindings.put(BINDING_MULE_CONTEXT, muleContext);
        bindings.put(BINDING_REGISTRY, muleContext.getRegistry());
    }

    public void populateBindings(Bindings bindings, Object payload)
    {
        populatePropertyBindings(bindings);
        populateDefaultBindings(bindings);
        bindings.put(BINDING_PAYLOAD, payload);
        // For backward compatability. Usually used by the script transformer since
        // src maps with the argument passed into the transformer
        bindings.put(BINDING_SRC, payload);
    }

    /**
     * @deprecated This uses the deprecated method {@link ExpressionManager#parse(String, MuleMessage)} internally. Use
     *             {@link #populateBindings(Bindings, MuleEvent)} instead.
     */
    @Deprecated
    public void populateBindings(Bindings bindings, MuleMessage message)
    {
        populatePropertyBindings(bindings, message);
        populateDefaultBindings(bindings);
        populateMessageBindings(bindings, message);
    }

    public void populateBindings(Bindings bindings, MuleEvent event)
    {
        populatePropertyBindings(bindings, event);
        populateDefaultBindings(bindings);
        populateMessageBindings(bindings, event.getMessage());

        bindings.put(BINDING_ORIGINAL_PAYLOAD, event.getMessage().getPayload());
        bindings.put(BINDING_EVENT_CONTEXT, new DefaultMuleEventContext(event));
        bindings.put(BINDING_ID, event.getId());
        bindings.put(BINDING_FLOW_CONSTRUCT, event.getFlowConstruct());
        if (event.getFlowConstruct() instanceof Service)
        {
            bindings.put(BINDING_SERVICE, event.getFlowConstruct());
        }
    }

    protected void populateMessageBindings(Bindings bindings, MuleMessage message)
    {
        if (message == null)
        {
            message = new DefaultMuleMessage(NullPayload.getInstance(), muleContext);
        }

        populateVariablesInOrder(bindings, message);

        bindings.put(BINDING_MESSAGE, message);
        // This will get overwritten if populateBindings(Bindings bindings, MuleEvent event) is called
        // and not this method directly.
        bindings.put(BINDING_PAYLOAD, message.getPayload());
        // For backward compatability
        bindings.put(BINDING_SRC, message.getPayload());

        populateHeadersVariablesAndException(bindings, message);
    }

    private void populateHeadersVariablesAndException(Bindings bindings, MuleMessage message)
    {
        bindings.put(BINDING_FLOW_VARS, new MesssagePropertyMap(message, INVOCATION));
        bindings.put(BINDING_SESSION_VARS, new MesssagePropertyMap(message, SESSION));

        // Only add exception is present
        if (message.getExceptionPayload() != null)
        {
            bindings.put(BINDING_EXCEPTION, message.getExceptionPayload().getException());
        }
        else
        {
            bindings.put(BINDING_EXCEPTION, null);
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
                result = registryLookupBindings.get(BINDING_RESULT);
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
            if (INBOUND.equals(propertyScope))
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
            if (INBOUND.equals(propertyScope))
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
