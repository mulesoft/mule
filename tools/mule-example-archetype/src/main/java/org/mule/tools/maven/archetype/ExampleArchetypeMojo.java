/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tools.maven.archetype;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.archetype.Archetype;
import org.apache.maven.archetype.ArchetypeDescriptorException;
import org.apache.maven.archetype.ArchetypeNotFoundException;
import org.apache.maven.archetype.ArchetypeTemplateProcessingException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Builds archetype containers.
 *
 * @goal create
 * @description The archetype creation goal looks for an archetype with a given newGroupId, newArtifactId, and
 * newVersion and retrieves it from the remote repository. Once the archetype is retrieve it is process against
 * a set of user parameters to create a working Maven project. This is a modified newVersion for bobber to support additional functionality.
 * @requiresProject false
 */
public class ExampleArchetypeMojo extends AbstractMojo
{
    /**
     * @parameter expression="${component.org.apache.maven.archetype.Archetype}"
     * @required
     */
    private Archetype archetype;

    /**
     * @parameter expression="${localRepository}"
     * @required
     */
    private ArtifactRepository localRepository;

    /**
     * @parameter expression="${archetypeGroupId}" default-value="org.mule.tools"
     * @required
     */
    private String archetypeGroupId;

    /**
     * @parameter expression="${archetypeArtifactId}" default-value="mule-example-archetype"
     * @required
     */
    private String archetypeArtifactId;

    /**
     * @parameter expression="${archetypeVersion}" default-value="${muleVersion}"
     * @required
     */
    private String archetypeVersion;

    /**
     * @parameter expression="${muleVersion}"
     * @required
     */
    private String muleVersion;

    /**
     * @parameter expression="${groupId}" alias="newGroupId" default-value="com.mycompany.mule"
     * @require
     */
    private String groupId;

    /**
     * @parameter expression="${artifactId}" alias="newArtifactId" default-value="my-mule-example"
     * @require
     */
    private String artifactId;

    /**
     * @parameter expression="${version}" alias="newVersion" default-value="1.0-SNAPSHOT"
     * @require
     */
    private String version;

    /** @parameter expression="${packageName}" alias="package" */
    private String packageName;

    /**
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     */
    private List remoteRepositories;

    public void execute()
            throws MojoExecutionException
    {

        // ----------------------------------------------------------------------
        // archetypeGroupId
        // archetypeArtifactId
        // archetypeVersion
        //
        // localRepository
        // remoteRepository
        // parameters
        // ----------------------------------------------------------------------

        String basedir = System.getProperty("user.dir");

        if (packageName == null)
        {
            getLog().info("Defaulting package to group ID: " + groupId);

            packageName = groupId;
        }

        // TODO: context mojo more appropriate?
        Map map = new HashMap();

        map.put("basedir", basedir);

        map.put("package", packageName);

        map.put("packageName", packageName);

        map.put("groupId", groupId);

        map.put("artifactId", artifactId);

        map.put("version", version);
        map.put("muleVersion", muleVersion);


        try
        {
            archetype.createArchetype(archetypeGroupId, archetypeArtifactId, archetypeVersion, localRepository, remoteRepositories, map);
        }
        catch (ArchetypeNotFoundException e)
        {
            throw new MojoExecutionException("Error creating from archetype", e);
        }
        catch (ArchetypeDescriptorException e)
        {
            throw new MojoExecutionException("Error creating from archetype", e);
        }
        catch (ArchetypeTemplateProcessingException e)
        {
            throw new MojoExecutionException("Error creating from archetype", e);
        }
    }


}
