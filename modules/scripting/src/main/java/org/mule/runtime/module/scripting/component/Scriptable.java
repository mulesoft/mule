/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.scripting.component;

import static org.mule.runtime.core.config.i18n.CoreMessages.cannotLoadFromClasspath;
import static org.mule.runtime.core.config.i18n.CoreMessages.propertiesNotSet;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.IOUtils.getResourceAsStream;
import org.mule.runtime.core.DefaultMuleEventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.el.context.FlowVariableMapContext;
import org.mule.runtime.core.el.context.SessionVariableMapContext;
import org.mule.runtime.core.util.CollectionUtils;
import org.mule.runtime.core.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JSR 223 Script service. Allows any JSR 223 compliant script engines such as JavaScript, Groovy or Rhino to be embedded as
 * Mule components.
 */
public class Scriptable implements Initialisable, MuleContextAware {

  private static final String BINDING_LOG = "log";
  private static final String BINDING_RESULT = "result";
  private static final String BINDING_MULE_CONTEXT = "muleContext";
  private static final String BINDING_REGISTRY = "registry";
  private static final String BINDING_PAYLOAD = "payload";
  private static final String BINDING_SRC = "src";
  private static final String BINDING_EVENT_CONTEXT = "eventContext";
  private static final String BINDING_ID = "id";
  private static final String BINDING_FLOW_CONSTRUCT = "flowConstruct";
  private static final String BINDING_FLOW_VARS = "flowVars";
  private static final String BINDING_SESSION_VARS = "sessionVars";
  private static final String BINDING_EXCEPTION = "exception";
  public static final String BINDING_MESSAGE = "message";

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

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  public Scriptable() {
    // For Spring
  }

  public Scriptable(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public void initialise() throws InitialisationException {
    scriptEngineManager = new ScriptEngineManager();

    // Create scripting engine
    if (scriptEngineName != null) {
      scriptEngine = createScriptEngineByName(scriptEngineName);
      if (scriptEngine == null) {
        throw new InitialisationException(createStaticMessage("Scripting engine '" + scriptEngineName
            + "' not found.  Available engines are: " + listAvailableEngines()), this);
      }
    }
    // Determine scripting engine to use by file extension
    else if (scriptFile != null) {
      int i = scriptFile.lastIndexOf(".");
      if (i > -1) {
        logger.info("Script Engine name not set. Guessing by file extension.");
        String extension = scriptFile.substring(i + 1);
        scriptEngine = createScriptEngineByExtension(extension);
        if (scriptEngine == null) {
          throw new InitialisationException(createStaticMessage("File extension '" + extension
              + "' does not map to a scripting engine.  Available engines are: " + listAvailableEngines()), this);
        } else {
          setScriptEngineName(extension);
        }
      }
    }

    Reader script = null;
    try {
      // Load script from variable
      if (StringUtils.isNotBlank(scriptText)) {
        script = new StringReader(scriptText);
      }
      // Load script from file
      else if (scriptFile != null) {
        InputStream is;
        try {
          is = getResourceAsStream(scriptFile, getClass());
        } catch (IOException e) {
          throw new InitialisationException(cannotLoadFromClasspath(scriptFile), e, this);
        }
        if (is == null) {
          throw new InitialisationException(cannotLoadFromClasspath(scriptFile), this);
        }
        script = new InputStreamReader(is);
      } else {
        throw new InitialisationException(propertiesNotSet("scriptText, scriptFile"), this);
      }

      // Pre-compile script if scripting engine supports compilation.
      if (scriptEngine instanceof Compilable) {
        try {
          compiledScript = ((Compilable) scriptEngine).compile(script);
        } catch (ScriptException e) {
          throw new InitialisationException(e, this);
        }
      }
    } finally {
      if (script != null) {
        try {
          script.close();
        } catch (IOException e) {
          throw new InitialisationException(e, this);
        }
      }
    }
  }

  protected void populatePropertyBindings(Bindings bindings) {
    if (properties != null) {
      bindings.putAll((Map) properties);
    }
  }

  protected void populatePropertyBindings(Bindings bindings, MuleEvent event) {
    if (properties != null) {
      for (Entry entry : properties.entrySet()) {
        String value = (String) entry.getValue();
        if (muleContext.getExpressionManager().isExpression(value)) {
          bindings.put((String) entry.getKey(), muleContext.getExpressionManager().parse(value, event));
        } else {
          bindings.put((String) entry.getKey(), value);
        }
      }
    }
  }

  public void populateDefaultBindings(Bindings bindings) {
    bindings.put(BINDING_LOG, logger);
    // A place holder for a returned result if the script doesn't return a result.
    // The script can overwrite this binding
    bindings.put(BINDING_RESULT, null);
    bindings.put(BINDING_MULE_CONTEXT, muleContext);
    bindings.put(BINDING_REGISTRY, muleContext.getRegistry());
  }

  public void populateBindings(Bindings bindings, MuleEvent event) {
    populatePropertyBindings(bindings, event);
    populateDefaultBindings(bindings);
    populateMessageBindings(bindings, event);

    bindings.put(BINDING_EVENT_CONTEXT, new DefaultMuleEventContext(event));
    bindings.put(BINDING_ID, event.getId());
    bindings.put(BINDING_FLOW_CONSTRUCT, event.getFlowConstruct());
  }

  protected void populateMessageBindings(Bindings bindings, MuleEvent event) {
    MuleMessage message = event.getMessage();

    populateVariablesInOrder(bindings, event);

    // TODO MULE-10121 Provide a MessageBuilder API in scripting components to improve usability
    bindings.put(BINDING_MESSAGE, event.getMessage());
    // This will get overwritten if populateBindings(Bindings bindings, MuleEvent event) is called
    // and not this method directly.
    bindings.put(BINDING_PAYLOAD, message.getPayload());
    // For backward compatability
    bindings.put(BINDING_SRC, message.getPayload());

    populateHeadersVariablesAndException(bindings, event);
  }

  private void populateHeadersVariablesAndException(Bindings bindings, MuleEvent event) {
    bindings.put(BINDING_FLOW_VARS, new FlowVariableMapContext(event));
    bindings.put(BINDING_SESSION_VARS, new SessionVariableMapContext(event.getSession()));

    // Only add exception is present
    if (event.getMessage().getExceptionPayload() != null) {
      bindings.put(BINDING_EXCEPTION, event.getMessage().getExceptionPayload().getException());
    } else {
      bindings.put(BINDING_EXCEPTION, null);
    }
  }

  private void populateVariablesInOrder(Bindings bindings, MuleEvent event) {
    for (String key : event.getSession().getPropertyNamesAsSet()) {
      bindings.put(key, event.getSession().getProperty(key));
    }
    for (String key : event.getFlowVariableNames()) {
      bindings.put(key, event.getFlowVariable(key));
    }
  }

  public Object runScript(Bindings bindings) throws ScriptException {
    Object result;
    try {
      RegistryLookupBindings registryLookupBindings = new RegistryLookupBindings(muleContext.getRegistry(), bindings);
      if (compiledScript != null) {
        result = compiledScript.eval(registryLookupBindings);
      } else {
        result = scriptEngine.eval(scriptText, registryLookupBindings);
      }

      // The result of the script can be returned directly or it can
      // be set as the variable "result".
      if (result == null) {
        result = registryLookupBindings.get(BINDING_RESULT);
      }
    } catch (ScriptException e) {
      // re-throw
      throw e;
    } catch (Exception ex) {
      throw new ScriptException(ex);
    }
    return result;
  }

  protected ScriptEngine createScriptEngineByName(String name) {
    return scriptEngineManager.getEngineByName(name);
  }

  protected ScriptEngine createScriptEngineByExtension(String ext) {
    return scriptEngineManager.getEngineByExtension(ext);
  }

  protected String listAvailableEngines() {
    return CollectionUtils.toString(scriptEngineManager.getEngineFactories(), false);
  }

  // //////////////////////////////////////////////////////////////////////////////
  // Getters and setters
  // //////////////////////////////////////////////////////////////////////////////

  public String getScriptText() {
    return scriptText;
  }

  public void setScriptText(String scriptText) {
    this.scriptText = scriptText;
  }

  public String getScriptFile() {
    return scriptFile;
  }

  public void setScriptFile(String scriptFile) {
    this.scriptFile = scriptFile;
  }

  public void setScriptEngineName(String scriptEngineName) {
    this.scriptEngineName = scriptEngineName;
  }

  public String getScriptEngineName() {
    return scriptEngineName;
  }

  public Properties getProperties() {
    return properties;
  }

  public void setProperties(Properties properties) {
    this.properties = properties;
  }

  public ScriptEngine getScriptEngine() {
    return scriptEngine;
  }

  protected void setScriptEngine(ScriptEngine scriptEngine) {
    this.scriptEngine = scriptEngine;
  }

  protected CompiledScript getCompiledScript() {
    return compiledScript;
  }

  protected void setCompiledScript(CompiledScript compiledScript) {
    this.compiledScript = compiledScript;
  }

}
