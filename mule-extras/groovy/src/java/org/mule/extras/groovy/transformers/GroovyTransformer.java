/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) zenAptix. All rights reserved.
 * http://www.zenaptix.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.groovy.transformers;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.InitialisationException;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

import java.io.File;
import java.net.URL;


/**
 * <code>GroovyTransformer</code> a classloader for groovy scripts.
 * The loader uses the logical name of the transformer as defined in the mule configuration to
 * locate a corresponding script in a scripts directory i.e. if the Transformer name is foo then
 * the classloader will attempt to load foo.groovy from the scripts directory.
 * Thus it is possible to have multiple transformer instances of the class <code>GroovyTransformer</code>
 * each with a unique name, indicating the appropriate script
 * The groovy script (class) must have a transform method returning the appropriate type
 * after it has transformed the supplied source
 *
 * @author <a href="mailto:ian@zenaptix.com">Ian de Beer</a>
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class GroovyTransformer extends AbstractTransformer
{
    protected final Log logger = LogFactory.getLog(getClass());
    private String script;
    private String methodName = "transform";
    private URL scriptLocation;
    private GroovyObject transformer = null;

    public GroovyTransformer()
    {
        registerSourceType(Object.class);
    }

    /* (non-Javadoc)
     * @see org.mule.transformers.AbstractTransformer#doTransform(java.lang.Object)
     */
    public Object doTransform(Object src) throws TransformerException
    {
        Object result = null;
        try
        {
            Object[] args = {src};
            result = transformer.invokeMethod(methodName, args);
            logger.debug("Groovy transform Result " + result);
        } catch (Exception e)
        {
            throw new TransformerException("Failed groovy transform: " + e.getMessage(), e);
        }
        return result;
    }

    public void initialise() throws InitialisationException
    {
        try
        {
            GroovyClassLoader loader = new GroovyClassLoader(getClass().getClassLoader());
            // defaults to the logical name of the Transformer can be changed by explicit setting of the scriptName
            if(script==null) {
                script = getName() + ".groovy";
            }
            if (scriptLocation == null)
            {
                scriptLocation = loader.getResource(script);
            }

            if(scriptLocation==null) {
                File file = new File(script);
                if(file.exists()) {
                    scriptLocation = file.toURL();
                } else {
                    throw new InitialisationException("Failed to load groovy script on classpath or fs: " + script);
                }
            }
            logger.info("Loading Groovy transformer with script " + scriptLocation.toExternalForm());
            Class groovyClass = loader.parseClass(new GroovyCodeSource(scriptLocation));
            transformer = (GroovyObject) groovyClass.newInstance();
        } catch (Exception e)
        {
            throw new InitialisationException("Failed to create Groovy transformer: " + e.getMessage(), e);
        } 
    }


    public void setScript(String script)
    {
        this.script = script;
    }

    public String getScript()
    {
        return script;
    }

    protected void setScriptLocation(URL scriptLocation)
    {
        this.scriptLocation = scriptLocation;
    }

    protected void setGroovyTransformer(GroovyObject trans) {
        transformer = trans;
    }

    public String getMethodName()
    {
        return methodName;
    }

    public void setMethodName(String methodName)
    {
        this.methodName = methodName;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    public Object clone() throws CloneNotSupportedException
    {
        GroovyTransformer trans = (GroovyTransformer)super.clone();
        trans.setMethodName(methodName);
        trans.setScript(script);
        trans.setScriptLocation(scriptLocation);
        trans.setGroovyTransformer(transformer);
        return trans;
    }
}