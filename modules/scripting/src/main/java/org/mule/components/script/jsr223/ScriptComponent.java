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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.util.MuleLogger;

import javax.script.Namespace;

/**
 * A JSR 223 Script component. Allows any JSR 223 compliant script engines
 * such as javaScript, Groovy or Rhino to be embedded as Mule components
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ScriptComponent extends Scriptable implements Callable {

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());



    private Namespace namespace;

    public void initialise() throws InitialisationException, RecoverableException {

        super.initialise();
        namespace = getScriptEngine().createNamespace();
    }

    public Object onCall(UMOEventContext eventContext) throws Exception {
        populateNamespace(namespace, eventContext);
        Object result = runScript(namespace);
        if(result==null) {
            result = namespace.get("result");
        }
        return result;
    }

    protected void populateNamespace(Namespace namespace, UMOEventContext context) {
        namespace.put("eventContext", context);
        namespace.put("managementContext", MuleManager.getInstance());
        namespace.put("message", context.getMessage());
        namespace.put("descriptor", context.getComponentDescriptor());
        namespace.put("componentNamespace", this.namespace);
        namespace.put("log", new MuleLogger(logger));
        namespace.put("result", new Object());
    }

    public Namespace getNamespace() {
        return namespace;
    }
}

