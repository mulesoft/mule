package org.mule.tools.config.graph.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;
import org.mule.tools.config.graph.MuleGrapher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Vector;

public class MuleGrapherTask extends MatchingTask {
	private Vector filesets = new Vector();

	private File outputdir;

	private Vector generatedFiles = new Vector();

	public void setOutputdir(File outputdir) {
		this.outputdir = outputdir;
	}

	/**
	 * Adds a set of files to copy.
	 * 
	 * @param set
	 *            a set of files to copy
	 */
	public void addFileset(FileSet set) {
		filesets.addElement(set);
	}

	public void execute() throws BuildException {

		assertOutputDir();

		assertFileSets();

		// deal with the filesets
		for (int i = 0; i < filesets.size(); i++) {
			FileSet fs = (FileSet) filesets.elementAt(i);
			DirectoryScanner ds = null;
			ds = fs.getDirectoryScanner(getProject());
			String[] srcFiles = ds.getIncludedFiles();
			for (int j = 0; j < srcFiles.length; j++) {
				processMuleConfig(fs, srcFiles, j);
			}
		}

		generateGallery();
	}

	private void generateGallery() {
		FileOutputStream out;
		PrintStream p;
		try {
			out = new FileOutputStream(outputdir.getAbsoluteFile()
					+ File.separator + "index.html");
			p = new PrintStream(out);
			p.println("<html> <body>");
			for (Iterator iter = generatedFiles.iterator(); iter.hasNext();) {
				String file = (String) iter.next();
				p.println("<img src=\"" + file + ".gif\" alt=\"" + file
						+ "\" title=\"" + file + "\"" + "\"/>");
				p.println("<br/>");
			}
			p.println("</body></html> ");
			p.close();
		} catch (Exception e) {
			System.err.println("Error writing to file");
		}
	}

	private void processMuleConfig(FileSet fs, String[] srcFiles, int j) {

		log("Processing : " + srcFiles[j]);

		MuleGrapher grapher = new MuleGrapher(fs.getDir(getProject())
				+ File.separator + srcFiles[j]);
		try {
			grapher.generateGraph(outputdir.getAbsolutePath(), srcFiles[j]);
			generatedFiles.add(srcFiles[j]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void assertFileSets() {
		if (filesets == null || filesets.isEmpty())
			throw new BuildException(
					"empty fileset : you must at least provide one valid file");
	}

	private void assertOutputDir() {
		if (outputdir == null)
			throw new BuildException("outputdir is mandatory");
		if (!(outputdir.exists()))
			throw new BuildException("outputdir doesn't exist :"
					+ outputdir.getAbsolutePath());
		if (!(outputdir.isDirectory()))
			throw new BuildException("outputdir must be a valid directory : "
					+ outputdir.getAbsolutePath());
	}
}
