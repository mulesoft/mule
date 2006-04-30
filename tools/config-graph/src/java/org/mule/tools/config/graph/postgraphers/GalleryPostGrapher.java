package org.mule.tools.config.graph.postgraphers;

import org.mule.tools.config.graph.config.GraphEnvironment;

import java.io.File;

public class GalleryPostGrapher extends AbstractIndexer{

    public static final String DEFAULT_MULE_GALLERY_TEMPLATE = "./src/resources/template/gallery-index.vm";

    private String template;

    public GalleryPostGrapher(GraphEnvironment env) throws Exception {
        super(env);
        template = env.getProperties().getProperty("muleGalleryTemplate");
        if(template==null)  {
            template = DEFAULT_MULE_GALLERY_TEMPLATE;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.tools.config.graph.PostGrapher#postGrapher(org.mule.tools.config.graph.GraphConfig)
     */
    public void postGrapher(GraphEnvironment env) {
        File[] htmlFiles = getFiles(env.getConfig(),".gif");
        String targetFile = env.getConfig().applyWorkingDirectory(env.getProperty("muleGalleryOutputName", "gallery.html"));
        doRendering(env, htmlFiles, template, targetFile);

    }
    public String getStatusTitle() {
        return "Generating Gallery page";
    }
}
