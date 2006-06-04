/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.components.script.jsr223;

import org.mule.components.builder.AbstractMessageBuilder;
import org.mule.components.builder.MessageBuilderException;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Namespace;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * A message builder component that can execute message building as a script
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ScriptMessageBuilder extends AbstractMessageBuilder implements Initialisable {

    /** Delegating script component that actually does the work */
    protected Scriptable scriptable;

    public ScriptMessageBuilder() {
        this.scriptable = new Scriptable();
    }

    public Object buildMessage(UMOMessage request, UMOMessage response) throws MessageBuilderException {
        Namespace namespace = scriptable.getScriptEngine().createNamespace();
        populateNamespace(namespace, request, response);
        Object result = null;
        try {
            result = runScript(namespace);
        } catch (ScriptException e) {
            throw new MessageBuilderException(response, e);
        }
        if (result == null) {
            throw new NullPointerException("A result payload must be returned from the groovy script");
        }
        return result;
    }

    public void initialise() throws InitialisationException, RecoverableException {
        scriptable.initialise();
    }

    protected void populateNamespace(Namespace namespace, UMOMessage request, UMOMessage response) {
        namespace.put("request", request);
        namespace.put("response", response);
        namespace.put("descriptor", descriptor);
        namespace.put("componentNamespace", namespace);
        namespace.put("log", logger);
    }

    public ScriptEngine getScriptEngine() {
        return scriptable.getScriptEngine();
    }

    public void setScriptEngine(ScriptEngine scriptEngine) {
        scriptable.setScriptEngine(scriptEngine);
    }

    public CompiledScript getCompiledScript() {
        return scriptable.getCompiledScript();
    }

    public void setCompiledScript(CompiledScript compiledScript) {
        scriptable.setCompiledScript(compiledScript);
    }

    public String getScriptText() {
        return scriptable.getScriptText();
    }

    public void setScriptText(String scriptText) {
        scriptable.setScriptText(scriptText);
    }

    public String getScriptFile() {
        return scriptable.getScriptFile();
    }

    public void setScriptFile(String scriptFile) {
        scriptable.setScriptFile(scriptFile);
    }

    public void setScriptEngineName(String scriptEngineName) {
        scriptable.setScriptEngineName(scriptEngineName);
    }

    protected void populateNamespace(Namespace namespace, UMOEventContext context) {
        namespace.put("context", context);
        namespace.put("message", context.getMessage());
        namespace.put("descriptor", context.getComponentDescriptor());
        namespace.put("componentNamespace", namespace);
        namespace.put("log", logger);
        namespace.put("result", new Object());
    }

    protected void compileScript(Compilable compilable) throws ScriptException {
        scriptable.compileScript(compilable);
    }

    protected Object evaluteScript(Namespace namespace) throws ScriptException {
        return scriptable.evaluteScript(namespace);
    }

    protected Object runScript(Namespace namespace) throws ScriptException {
        return scriptable.runScript(namespace);
    }

    protected ScriptEngine createScriptEngine() {
        return scriptable.createScriptEngine();
    }

}
