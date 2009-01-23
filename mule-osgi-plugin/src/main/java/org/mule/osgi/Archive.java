/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.osgi;

import java.util.Map;

public class Archive
{
    private String groupId;
    private String artifactId;
    private String version;
    private Map<String, String> instructions;
    private String classifier;

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId(String groupId)
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId(String artifactId)
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public Map<String, String> getInstructions()
    {
        return instructions;
    }

    public void setInstructions(Map<String, String> instructions)
    {
        this.instructions = instructions;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier(String classifier)
    {
        this.classifier = classifier;
    }

}
