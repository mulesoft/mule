/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.components.script.jsr223;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Namespace;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.util.IOUtils;

/**
 * A JSR 223 Script component. Allows any JSR 223 compliant script engines
 * such as javaScript, Groovy or Rhino to be embedded as Mule components
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class Scriptable implements Initialisable {

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());


    private String scriptText;
    private String scriptFile;
    private Reader script;

    private CompiledScript compiledScript;
    private ScriptEngine scriptEngine;
    private String scriptEngineName;

    public void initialise() throws InitialisationException, RecoverableException {

        if (scriptEngine == null) {
            if (compiledScript == null) {
                if (scriptEngineName != null) {
                    scriptEngine = createScriptEngine();
                } else if(scriptFile!=null) {
                    int i = scriptFile.lastIndexOf(".");
                    if(i > -1) {
                        setScriptEngineName(scriptFile.substring(i + 1));
                        logger.info("Script Engine name not set.  Defaulting to file extension: " + getScriptEngineName());
                        scriptEngine = createScriptEngine();
                    }
                }
                if (scriptEngine == null) {
                    throw new InitialisationException(new Message(Messages.PROPERTIES_X_NOT_SET, "scriptEngine, scriptEngineName, compiledScript"), this);
                }
            } else {
                scriptEngine = compiledScript.getEngine();
            }
        }
        if (compiledScript == null) {
            if (script == null) {
                if (scriptText == null && scriptFile == null) {
                    throw new InitialisationException(new Message(Messages.PROPERTIES_X_NOT_SET, "scriptText, scriptFile"), this);
                } else if (scriptText != null) {
                    script = new StringReader(scriptText);
                } else {
                    InputStream is = null;
                    try {
                        is = IOUtils.getResourceAsStream(scriptFile, getClass());
                        script = new InputStreamReader(is);
                    } catch (IOException e) {
                        throw new InitialisationException(new Message(Messages.CANT_LOAD_X_FROM_CLASSPATH_FILE, scriptFile), e, this);
                    }
                }
            }
            try {
                compiledScript = compileScript(script);
            } catch (ScriptException e) {
                throw new InitialisationException(e, this);
            }
        }
    }


    public ScriptEngine getScriptEngine() {
        return scriptEngine;
    }

    public void setScriptEngine(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    public CompiledScript getCompiledScript() {
        return compiledScript;
    }

    public void setCompiledScript(CompiledScript compiledScript) {
        this.compiledScript = compiledScript;
    }

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

    protected CompiledScript compileScript(Compilable compilable, Reader scriptReader) throws ScriptException {
        return compilable.compile(scriptReader);
    }

    protected CompiledScript compileScript(Reader scriptReader) throws ScriptException {
        if (scriptEngine instanceof Compilable) {
            Compilable compilable = (Compilable) scriptEngine;
            return compileScript(compilable, scriptReader);
        }
        return null;
    }

    protected CompiledScript compileScript(Compilable compilable) throws ScriptException {
        return compileScript(compilable, script);
    }

    protected Object evaluteScript(Namespace namespace) throws ScriptException {
        return scriptEngine.eval(scriptText, namespace);
    }

    public Object runScript(Namespace namespace) throws ScriptException {
        Object result = null;
        if (compiledScript != null) {
            result = compiledScript.eval(namespace);
        } else {
            result = evaluteScript(namespace);
        }
        return result;
    }

    public Object runScript(CompiledScript compiledScript, Namespace namespace) throws ScriptException {
        Object result = null;
        if (compiledScript != null) {
            result = compiledScript.eval(namespace);
        }
        return result;
    }

    protected ScriptEngine createScriptEngine() {
        ScriptEngineManager manager = new ScriptEngineManager();
        return manager.getEngineByName(scriptEngineName);
    }
}

