package com.javaforge.bobber.plugin.archetype;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.javaforge.bobber.archetype.model.Template;
import com.javaforge.bobber.archetype.model.Variable;
import com.javaforge.bobber.archetype.model.io.xpp3.BobberArchetypeXpp3Reader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.maven.archetype.Archetype;
import org.apache.maven.archetype.ArchetypeDescriptorException;
import org.apache.maven.archetype.ArchetypeNotFoundException;
import org.apache.maven.archetype.ArchetypeTemplateProcessingException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.velocity.VelocityContext;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.plexus.velocity.VelocityComponent;

//source blatantly copied from org.apache.maven.archetype.DefaultArchetype. Also stole code from DefaultPluginVersionManager.java

//and maven-model for the mdo
public class BobberArchetype
        extends AbstractLogEnabled
        implements Archetype {

    private static final String NEW_LINE = System.getProperty("line.separator");

    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    /**
     * @component
     */
    private VelocityComponent velocity;

    /**
     * @component
     */
    private ArtifactResolver artifactResolver;

    /**
     * @component
     */
    private InputHandler inputHandler;

    /**
     * @component
     */
    private ArtifactFactory artifactFactory;


    /**
     * @component
     */
    private MavenSettingsBuilder settingsBuilder;
    private static final int MESSAGE_LINE_LENGTH = 80;


    public void createArchetype (String archetypeGroupId, String archetypeArtifactId, String archetypeVersion,
                                 ArtifactRepository localRepository, List remoteRepositories, Map parameters)
            throws ArchetypeNotFoundException, ArchetypeDescriptorException, ArchetypeTemplateProcessingException {

        // ---------------------------------------------------------------------
        //locate the archetype file
        // ---------------------------------------------------------------------
        Artifact archetypeArtifact = artifactFactory.createArtifact(archetypeGroupId, archetypeArtifactId,
                archetypeVersion, Artifact.SCOPE_RUNTIME, "jar");

        try {
            artifactResolver.resolve(archetypeArtifact, remoteRepositories, localRepository);
        } catch (ArtifactResolutionException e) {
            throw new ArchetypeDescriptorException("Error attempting to download archetype: " + e.getMessage(), e);
        }

        // ----------------------------------------------------------------------
        // Load the archetype descriptor
        // ----------------------------------------------------------------------

        BobberArchetypeXpp3Reader builder = new BobberArchetypeXpp3Reader();

        com.javaforge.bobber.archetype.model.BobberArchetype archetype;

        JarFile archetypeJarFile;

        try {

            archetypeJarFile = new JarFile(archetypeArtifact.getFile());

            final ZipEntry zipEntry = archetypeJarFile.getEntry(ARCHETYPE_DESCRIPTOR);
            InputStream is = archetypeJarFile.getInputStream(zipEntry);

            if (is == null) {
                throw new ArchetypeDescriptorException(
                        "The " + ARCHETYPE_DESCRIPTOR + " descriptor cannot be found.");
            }

            archetype = builder.read(new InputStreamReader(is));

            archetypeJarFile.close();
        } catch (IOException e) {
            throw new ArchetypeDescriptorException("Error reading the " + ARCHETYPE_DESCRIPTOR + " descriptor.", e);
        } catch (XmlPullParserException e) {
            throw new ArchetypeDescriptorException("Error reading the " + ARCHETYPE_DESCRIPTOR + " descriptor.", e);
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        String basedir = (String) parameters.get("basedir");

        String artifactId = (String) parameters.get("artifactId");

        File pomFile = new File(basedir, ARCHETYPE_POM);

        File outputDirectoryFile;

        if (pomFile.exists() && archetype.isAllowPartial()) {
            outputDirectoryFile = new File(basedir);
        } else {
            outputDirectoryFile = new File(basedir, artifactId);

            // TODO temporarily allow partial generation, remove it later
            if (!archetype.isAllowPartial() &&
                    outputDirectoryFile.exists() &&
                    outputDirectoryFile.listFiles().length > 0) {
                throw new ArchetypeTemplateProcessingException(
                        outputDirectoryFile.getName() + " already exists - please run from a clean directory");
            }

            outputDirectoryFile.mkdir();

        }

        String outputDirectory = outputDirectoryFile.getAbsolutePath();

        // ----------------------------------------------------------------------
        // Set up the Velocity context
        // ----------------------------------------------------------------------

        VelocityContext context = new VelocityContext();

        String packageName = (String) parameters.get("package");

        context.put("package", packageName);

        context.put("packagePath", StringUtils.replace(packageName, ".", "/"));

        for (Iterator iterator = parameters.keySet().iterator(); iterator.hasNext();) {
            String key = (String) iterator.next();

            Object value = parameters.get(key);

            context.put(key, value);
        }

        //add in the specified system properties
        //if this were a mojo could have set the settings using the ${settings} expression. Since it is not need to get it from the settings builder


        boolean inInteractiveMode = false;
        try {
            inInteractiveMode = settingsBuilder.buildSettings().getInteractiveMode().booleanValue();
        } catch (Exception ie) {
            throw new ArchetypeTemplateProcessingException("unable to read settings ", ie);
        }
        if (inInteractiveMode) {
            getLogger().info("Please enter the values for the following archetype variables:");
        }


        final List variables = archetype.getVariables();
        processVariables(variables.iterator(), context, inInteractiveMode);


        // ---------------------------------------------------------------------
        // Get Logger and display all parameters used
        // ---------------------------------------------------------------------
        if (getLogger().isInfoEnabled()) {
            Object[] keys = context.getKeys();
            if (keys.length > 0) {
                getLogger().info("----------------------------------------------------------------------------");

                getLogger().info("Using following parameters for creating Archetype: " + archetypeArtifactId + ":" +
                        archetypeVersion);

                getLogger().info("----------------------------------------------------------------------------");

                for (int i = 0; i < keys.length; i++) {

                    String parameterName = (String) keys[i];

                    Object parameterValue = context.get(parameterName);

                    getLogger().info("Parameter: " + parameterName + " = " + parameterValue);
                }
            } else {
                getLogger().info("No Parameters found for creating Archetype");
            }
        }

        // ----------------------------------------------------------------------
        // Extract the archetype to the chosen directory
        // ----------------------------------------------------------------------

        try {
            archetypeJarFile = new JarFile(archetypeArtifact.getFile());
            Enumeration entries = archetypeJarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = (JarEntry) entries.nextElement();
                String path = entry.getName();
                if (!path.startsWith(ARCHETYPE_RESOURCES) || path.endsWith(".vm")) {
                    continue;
                }

                File t = new File(outputDirectory, path.substring(19));
                if (entry.isDirectory()) {
                    // Assume directories are stored parents first then children.
                    getLogger().debug("Extracting directory: " + entry.getName() + " to " + t.getAbsolutePath());
                    t.mkdir();
                    continue;
                }

                getLogger().debug("Extracting file: " + entry.getName() + " to " + t.getAbsolutePath());
                t.createNewFile();
                IOUtil.copy(archetypeJarFile.getInputStream(entry), new FileOutputStream(t));
            }

            archetypeJarFile.close();

            //remove the archetype descriptor
            File t = new File(outputDirectory, ARCHETYPE_DESCRIPTOR);
            t.delete();
        } catch (IOException ioe) {
            throw new ArchetypeTemplateProcessingException("Error extracting archetype", ioe);
        }

        // ----------------------------------------------------------------------
        // Process the templates
        // ----------------------------------------------------------------------

        // use the out of the box codehaus velocity component that loads templates
        //from the class path
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            URL[] urls = new URL[1];
            urls[0] = archetypeArtifact.getFile().toURI().toURL();
            URLClassLoader archetypeJarLoader = new URLClassLoader(urls);

            Thread.currentThread().setContextClassLoader(archetypeJarLoader);
            for (Iterator i = archetype.getTemplates().iterator(); i.hasNext();) {
                final Template template = (Template) i.next();

                // Check the optional 'condition' property on the template.
                // If present and the variable it points to is 'true', then
                // continue processing. If it's false, then skip.
                // If condition is not specified, assume the template should
                // be processed.
                boolean shouldProcess = true;
                String condition = template.getDependsOnVar();
                String requiredValue=null;
                List options = new ArrayList();
                if (StringUtils.isNotEmpty(condition)) {
                    //Crappy logic processing -- for now
                    boolean not=false;
                    //Allow very simple matching logic to match templates against variable values
                    int x = condition.indexOf("!=");
                    getLogger().debug("Processing Condition : " + condition);                                        
                    if(x > -1) {
                        not=true;
                        requiredValue = condition.substring(x+2).trim();
                        options = getListOfValues(requiredValue);
                        condition = condition.substring(0, x).trim();
                    }
                    else {
                        x = condition.indexOf("=");
                        if(x > -1) {
                            requiredValue = condition.substring(x+1);
                            options = getListOfValues(requiredValue);
                            condition = condition.substring(0, x);
                        }
                    }
                    getLogger().debug("Not Expr: " + not);
                    getLogger().debug("Condition Value: '" + condition + "'");
                    getLogger().debug("Required Value: '" + requiredValue + "'");
                    final Variable var = (Variable) findVariable(condition, variables);
                    if (var != null) {
                        final String strValue = (String) context.get(var.getName());
                        getLogger().debug("Variable Value is: '" + strValue + "'");
                        if(requiredValue==null)
                        {
                            if (!Boolean.valueOf(strValue).booleanValue()) {
                                shouldProcess = false;
                            }
                        } else {
                            if(!options.contains(strValue))
                            {
                                shouldProcess = false;
                            }
                        }

                    } else {
                        getLogger().debug("Variable Value is: null");                                                
                        shouldProcess=false;
                    }
                    if(not) {
                        shouldProcess = !shouldProcess;
                    }
                }

                if (shouldProcess) {
                    processTemplate(template, outputDirectory, context);
                } else {
                    getLogger().debug("Condition not met, skipping " + template.getOutput());
                }
            }

        }
        catch (MalformedURLException mfe) {
            throw new ArchetypeTemplateProcessingException("Error loading archetype resources into the classpath", mfe);
        }
        finally {
            Thread.currentThread().setContextClassLoader(old);
        }

        // ----------------------------------------------------------------------
        // Log message on Archetype creation
        // ----------------------------------------------------------------------
        if (getLogger().isInfoEnabled()) {
            getLogger().info("Archetype created in dir: " + outputDirectory);
        }

    }

    protected void processVariables(Iterator variables, VelocityContext context, final boolean interactiveMode) throws ArchetypeTemplateProcessingException
    {
        while (variables.hasNext()) {
            Variable var = (Variable) variables.next();
            String val = System.getProperty(var.getName(), var.getDefvalue());

            if (interactiveMode) {

                    StringBuffer message = new StringBuffer();
                    message.append(var.getName()).append(": ")
                            .append(NEW_LINE)
                            .append(StringUtils.repeat("*", MESSAGE_LINE_LENGTH))
                            .append(NEW_LINE)
                            .append(NEW_LINE)
                            .append(StringUtils.center(var.getDescription(), MESSAGE_LINE_LENGTH))
                            .append(NEW_LINE)
                            .append(StringUtils.leftPad("[default: " + val + "]", MESSAGE_LINE_LENGTH))
                            .append(NEW_LINE)
                            .append(StringUtils.repeat("*", MESSAGE_LINE_LENGTH));
                    getLogger().info(message.toString());
                    try {
                        String answer = inputHandler.readLine();
                        if (!StringUtils.isEmpty(answer)) {
                            val = answer;
                        }
                    } catch (IOException ie) {
                        throw new ArchetypeTemplateProcessingException(ie);
                    }
                    context.put(var.getName(), val);
            }
            else
            {
                context.put(var.getName(), val);
            }

            if(val.toLowerCase().equals("false") || val.toLowerCase().equals("n"))
            {
                if(var.getVariables() !=null)
                {
                    //keep processing the variables picking up the default values
                    processVariables(var.getVariables().iterator(), context, false);

                }
            } else if(var.getVariables() !=null)
            {
                //keep processing the variables picking up the default values
                processVariables(var.getVariables().iterator(), context, true);

            }
        }

    }

    protected List getListOfValues(String s) {
        List options = new ArrayList();
        for (StringTokenizer stringTokenizer = new StringTokenizer(s, "|"); stringTokenizer.hasMoreTokens();)
        {
            options.add(stringTokenizer.nextToken());
        }
        return options;
    }

    protected void processTemplate (Template template, String outputDirectory, VelocityContext context)
            throws ArchetypeTemplateProcessingException {
        File outFile;


        try {
            StringWriter wout = new StringWriter();

            velocity.getEngine().evaluate(context, wout, "output value", template.getOutput());
            outFile = new File(outputDirectory, wout.toString());
            getLogger().debug(outFile.getAbsolutePath());
            FileUtils.forceMkdir(outFile.getParentFile());
            getLogger().debug("Created directory: " + outFile.getParentFile() + ", Dir exists = " + outFile.getParentFile().exists());

        } catch (Exception e) {
            e.printStackTrace();
            throw new ArchetypeTemplateProcessingException("error evaluating output file name " + template.getOutput(), e);
        }


        Writer writer = null;
        try {
            getLogger().info("Processing Template: " + template.getFile());
            String templateLocation = ARCHETYPE_RESOURCES + "/" + template.getFile();

            writer = new FileWriter(outFile);
            velocity.getEngine().mergeTemplate(templateLocation, context, writer);
            writer.flush();

        } catch (Exception e) {
            throw new ArchetypeTemplateProcessingException("Error merging velocity templates", e);
        } finally {
            IOUtil.close(writer);
            getLogger().info("Written Template to: " + outFile + ", file exists = " + outFile.exists());
        }

        // Delete archetype-originated folders in case the output path is also templated.
        // Otherwise, there will be a processed folder AND the original folder.
        try {
            final File templateFile = new File(outputDirectory, template.getFile());
            final String templateDir = FileUtils.dirname(templateFile.getCanonicalPath());
            final String outputDir = FileUtils.dirname(outFile.getCanonicalPath());
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("TemplateDir=" + templateDir);
                getLogger().debug("OutputDir=" + outputDir);
            }
            if (!outputDir.startsWith(templateDir)) {
                getLogger().debug("Deleting Template Dir:" + templateDir);
                FileUtils.forceDelete(templateDir);
            }
        } catch (IOException e) {
            throw new ArchetypeTemplateProcessingException("Failed to cleanup the working dir.", e);
        }
    }


    /**
     * Find the  variable.
     * @param variableName name
     * @param variables all variables of the artifact
     * @return variable value or null of not found
     */
    protected Object findVariable (String variableName, List variables) {

        for (int i = 0; i < variables.size(); i++) {
            Variable var = (Variable) variables.get(i);
            if (variableName.equals(var.getName())) {
                return var;
            } else if(var.getVariables()!=null) {
                Object o = findVariable(variableName, var.getVariables());
                if(o!=null) {
                    return o;
                }
            }
        }

        return null;
    }

}



