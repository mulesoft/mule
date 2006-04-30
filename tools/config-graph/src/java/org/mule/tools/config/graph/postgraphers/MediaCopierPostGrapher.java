package org.mule.tools.config.graph.postgraphers;

import org.apache.commons.io.FileUtils;
import org.mule.tools.config.graph.components.PostGrapher;
import org.mule.tools.config.graph.config.GraphEnvironment;

import java.io.File;
import java.io.IOException;

public class MediaCopierPostGrapher implements PostGrapher {

    public String getStatusTitle() {

        return "Copy Media files (logo, css,...)";
    }

    public void postGrapher(GraphEnvironment env) {

        try {
            FileUtils.copyDirectory(new File("./src/resources/media/"),env.getConfig().getOutputDirectory());
        } catch (IOException e) {
            env.logError(e.getMessage(), e);

        }
    }

}
