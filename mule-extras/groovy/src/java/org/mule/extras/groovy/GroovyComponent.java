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
package org.mule.extras.groovy;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyObject;
import org.mule.components.script.AbstractScriptComponent;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.ClassHelper;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * <code>GroovyComponent</code> allows a grooy object ot managed as a Mule
 * component.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GroovyComponent extends AbstractScriptComponent
{
    public static final String DEFAULT_METHOD_NAME = "onCall";
    private GroovyObject component = null;
    private String methodName = DEFAULT_METHOD_NAME;

    /**
     * Loads the script for this component
     * @param script the script file location
     * @throws InitialisationException if anything fails while starting up
     */
    protected void loadInterpreter(String script) throws InitialisationException
    {
        try
        {
            File f = new File(script);
            if(f.exists()) {
                fileChanged(f);
            } else {
                GroovyClassLoader loader = new GroovyClassLoader(Thread.currentThread().getContextClassLoader());
                URL url = ClassHelper.getResource(script, getClass());
                if(url==null) {
                    throw new InitialisationException(new Message(Messages.FAILED_LOAD_X, "Groovy script: " + script), this);
                }
                Class groovyClass = loader.parseClass(new GroovyCodeSource(url));
                component = (GroovyObject) groovyClass.newInstance();
            }
        } catch (Exception e)
        {
            if(e instanceof InitialisationException) throw (InitialisationException)e;
            throw new InitialisationException(new Message(Messages.FAILED_LOAD_X, "Groovy component"), e, this);
        }
    }

    protected String getDefaultFileExtension()
    {
        return ".groovy";
    }

    /**
     * Called when one of the monitored files are created, deleted
     * or modified.
     *
     * @param file File which has been changed.
     */
    public void fileChanged(File file) throws IOException
    {
        try
        {
            GroovyClassLoader loader = new GroovyClassLoader(getClass().getClassLoader());
            Class groovyClass = loader.parseClass(new GroovyCodeSource(file));
            component = (GroovyObject) groovyClass.newInstance();
        } catch (Exception e)
        {
            throw new IOException("Failed to reload groovy script: " + e.getMessage());
        }
    }

    public String getMethodName()
    {
        return methodName;
    }

    public void setMethodName(String methodName)
    {
        this.methodName = methodName;
    }

    /**
     * Passes the context to the listener
     *
     * @param context the context ot process
     * @return Object this object can be anything. When the <code>UMOLifecycleAdapter</code> for
     *         the components receives this object it will first see if the Object is an
     *         <code>UMOEvent</code> if not and the Object is not null a new context will be created using
     *         the returned object as the payload.  This new context will then get published to the configured
     *         outbound endpoint if-
     *         <ol>
     *         <li>One has been configured for the UMO.</li>
     *         <li>the <code>setStopFurtherProcessing(true)</code> wasn't called on the previous context.</li>
     *         </ol>
     * @throws Exception if the context fails to process properly. If exceptions aren't handled by the implementation
     *                   they will be handled by the exceptionListener associated with the components
     */
    public Object onCall(UMOEventContext context) throws Exception
    {
        if(methodName.equals(DEFAULT_METHOD_NAME)) {
            return component.invokeMethod(getMethodName(), context);
        } else {
            Object msg = context.getTransformedMessage();
            return component.invokeMethod(getMethodName(), msg);
        }
    }
}
