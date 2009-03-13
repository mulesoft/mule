package org.mule.tools.maven.archetype;

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
public class ModuleArchetypeMojo extends AbstractMojo
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
     * @parameter expression="${archetypeArtifactId}" default-value="mule-module-archetype"
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
     * @parameter expression="${artifactId}" alias="newArtifactId" default-value="my-mule-module"
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
