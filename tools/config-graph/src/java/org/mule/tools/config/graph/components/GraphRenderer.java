package org.mule.tools.config.graph.components;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.out.GRAPHtoDOTtoGIF;
import org.mule.tools.config.graph.config.GraphEnvironment;
import org.mule.tools.config.graph.postrenderers.FileCleanerPostRenderer;
import org.mule.tools.config.graph.postrenderers.MuleDocPostRenderer;
import org.mule.tools.config.graph.util.DOTtoMAP;
import org.mule.util.EnvironmentHelper;
import org.mule.util.Utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class GraphRenderer {

	private GraphEnvironment env;
	private List postRenderers= new ArrayList();
	
	public GraphRenderer(GraphEnvironment env) throws Exception {
		this.env = env;
		postRenderers.add(new MuleDocPostRenderer(env));
		postRenderers.add(new FileCleanerPostRenderer());
		
	}

	public void saveGraph(Graph graph, String filename, File outFolder)
			throws IOException {
		// output graph to *.gif
		final String dotFileName = new File(outFolder, filename + ".dot").getAbsolutePath();
		final String mapFileName = new File(outFolder, filename + ".cmapx").getAbsolutePath();
		final String gifFileName = new File(outFolder, filename + ".gif").getAbsolutePath();
		
		
		final String exeFile = getSaveExecutable();
		env.log("Executing: " + exeFile);
		GRAPHtoDOTtoGIF.transform(graph, dotFileName, gifFileName, exeFile);
		env.log("generating MAP");
		DOTtoMAP.transform(exeFile, dotFileName, mapFileName, env);

		Map context = new HashMap();
        String map = Utility.fileToString(mapFileName);
        String path = env.getConfig().getOutputDirectory().getAbsolutePath() + File.separator;
		context.put("dotFileName", path + filename + ".dot");
		context.put("mapFileName", path + filename + ".cmapx");
		context.put("mapFile", map);
		context.put("gifFileName", filename + ".gif");
		context.put("htmlFileName", path + filename + ".html");
		context.put("outFolder", outFolder.getAbsolutePath());

		for (Iterator iter = postRenderers.iterator(); iter.hasNext();) {
			PostRenderer element = (PostRenderer) iter.next();
			element.postRender(env, context, graph);
		}
	}


	private String getSaveExecutable() throws FileNotFoundException {
		if (env.getConfig().getExecuteCommand() == null) {
			String osName = System.getProperty("os.name").toLowerCase();
			if (osName.startsWith("windows")) {
				File f = new File("win32/dot.exe");
                if(f.exists()) {

				    env.getConfig().setExecuteCommand(f.getAbsolutePath());
                } else {

                    Properties p = new EnvironmentHelper().getEnvProperties();
                    String home = p.getProperty("MULE_HOME");
                    if(home!=null) {
                        f = new File(home + "/tools/config-graph/win32/dot.exe");
                    }

                }
			} else {
				throw new UnsupportedOperationException(
						"Mule Graph currently only works on Windows");
			}
		}
		File f = new File(env.getConfig().getExecuteCommand());
		if (!f.exists()) {
			throw new FileNotFoundException(f.getAbsolutePath());
		}
		return env.getConfig().getExecuteCommand();
	}
}
