/*
 * $Id:  $
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;

/**
 * Build a Mule application archive.
 *
 * @execute lifecycle="mule-package" phase="package"
 * @goal mule
 * @requiresDependencyResolution runtime
 */
public class MuleMojo extends AbstractMuleMojo
{

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;


    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * Directory containing the classes.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File classesDirectory;

    /**
     * Directory containing the app resources.
     *
     * @parameter expression="${basedir}/src/main/app"
     * @required
     */
    private File appDirectory;

    /**
     * Whether a JAR file will be created for the classes in the app. Using this optional configuration
     * parameter will make the generated classes to be archived into a jar file
     * and the classes directory will then be excluded from the app.
     *
     * @parameter expression="${archiveClasses}" default-value="false"
     */
    private boolean archiveClasses;


    /**
     * @component
     */
    private ArtifactHandlerManager artifactHandlerManager;


    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {

        final File app = new File(this.outputDirectory, this.finalName + ".zip");
        try
        {
            createMuleApp(app);
        }
        catch (ArchiverException e)
        {
            throw new MojoExecutionException("Exception creating the Mule App", e);
        }

        this.projectHelper.attachArtifact(this.project, "zip", app);
    }

    protected void createMuleApp(final File app) throws MojoExecutionException, ArchiverException
    {
        final MuleArchiver archiver = new MuleArchiver();
        final File muleConfig = new File(appDirectory, "mule-config.xml");
        if (!muleConfig.exists())
        {
            final String message = "No muleConfig.xml available at: " + muleConfig;
            getLog().error(message);
            throw new MojoExecutionException(message);
        }

        archiver.addResources(appDirectory);

        if (!this.archiveClasses)
        {
            getLog().info("Copying classes directly");
            archiver.addClasses(this.classesDirectory, null, null);
        }
        else
        {
            getLog().info("Copying classes as a jar");

            final JarArchiver jarArchiver = new JarArchiver();
            jarArchiver.addDirectory(this.classesDirectory, null, null);
            final File jar = new File(this.outputDirectory, this.finalName + ".jar");
            jarArchiver.setDestFile(jar);
            try
            {
                jarArchiver.createArchive();
                archiver.addLib(jar);
            }
            catch (IOException e)
            {
                final String message = "Cannot create project jar";
                getLog().error(message, e);
                throw new MojoExecutionException(message, e);
            }
        }

        for (final Artifact artifact : (Set<Artifact>) this.project.getArtifacts())
        {
            if (Artifact.SCOPE_COMPILE.equals(artifact.getScope()) || Artifact.SCOPE_RUNTIME.equals(artifact.getScope()))
            {
                getLog().info("Adding <" + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() + "> as a lib");
                archiver.addLib(artifact.getFile());
            }
        }

        archiver.setDestFile(app);

        try
        {
            app.delete();
            archiver.createArchive();
        }
        catch (IOException e)
        {
            getLog().error("Cannot create archive", e);
        }
    }

}