/*
 * $Header$
 * $Revision$
 * $Date$
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

import javax.script.*;

/**
 * A message builder component that can execute message building as a script
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ScriptMessageBuilder extends AbstractMessageBuilder implements Initialisable {

    /** Delegating script component that actually does the work */
    protected ScriptComponent scriptComponent;

    public ScriptMessageBuilder() {
        this.scriptComponent = new ScriptComponent();
    }

    public Object buildMessage(UMOMessage request, UMOMessage response) throws MessageBuilderException {
        Namespace namespace = scriptComponent.getNamespace();
        populateNamespace(namespace, request, response);
        Object result = null;
        try {
            result = runScript(namespace);
        } catch (ScriptException e) {
            throw new MessageBuilderException(response, e);
        }
        if (result == null) throw new NullPointerException("A result payload must be returned from the groovy script");
        return result;
    }

    public void initialise() throws InitialisationException, RecoverableException {
        scriptComponent.initialise();
    }

    protected void populateNamespace(Namespace namespace, UMOMessage request, UMOMessage response) {
        namespace.put("request", request);
        namespace.put("response", response);
        namespace.put("descriptor", descriptor);
        namespace.put("componentNamespace", namespace);
        namespace.put("log", logger);
    }

    public ScriptEngine getScriptEngine() {
        return scriptComponent.getScriptEngine();
    }

    public void setScriptEngine(ScriptEngine scriptEngine) {
        scriptComponent.setScriptEngine(scriptEngine);
    }

    public CompiledScript getCompiledScript() {
        return scriptComponent.getCompiledScript();
    }

    public void setCompiledScript(CompiledScript compiledScript) {
        scriptComponent.setCompiledScript(compiledScript);
    }

    public String getScriptText() {
        return scriptComponent.getScriptText();
    }

    public void setScriptText(String scriptText) {
        scriptComponent.setScriptText(scriptText);
    }

    public String getScriptFile() {
        return scriptComponent.getScriptFile();
    }

    public void setScriptFile(String scriptFile) {
        scriptComponent.setScriptFile(scriptFile);
    }

    public void setScriptEngineName(String scriptEngineName) {
        scriptComponent.setScriptEngineName(scriptEngineName);
    }

    public Namespace getNamespace() {
        return scriptComponent.getNamespace();
    }

    protected void populateNamespace(Namespace namespace, UMOEventContext context) {
        scriptComponent.populateNamespace(namespace, context);
    }

    protected void compileScript(Compilable compilable) throws InitialisationException {
        scriptComponent.compileScript(compilable);
    }

    protected Object evaluteScript(Namespace namespace) throws ScriptException {
        return scriptComponent.evaluteScript(namespace);
    }

    protected Object runScript(Namespace namespace) throws ScriptException {
        return scriptComponent.runScript(namespace);
    }

    protected ScriptEngine createScriptEngine() {
        return scriptComponent.createScriptEngine();
    }

}
