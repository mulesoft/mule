package org.mule.jbi.management;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Timer;
import java.util.TimerTask;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.jbi.JbiContainer;
import org.mule.jbi.util.IOUtils;

public class AutoInstallService implements AutoInstallServiceMBean {

	private int pollingFrequency = 60000;
	private Timer timer;
	private Log logger = LogFactory.getLog(getClass());

	public AutoInstallService() {
		this.timer = new Timer(true);
	}
	
	public void start() {
		this.timer.schedule(new TimerTask() { 
			public void run() {
				AutoInstallService.this.poll();
			}
		}, 0, pollingFrequency);
	}
	
	public void stop() {
		this.timer.cancel();
	}
	
	public void poll() {
		File wrkDir = JbiContainer.Factory.getInstance().getWorkingDir();
		File insDir = Directories.getAutoInstallDir(wrkDir);
		File[] files = insDir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				  name = name.toLowerCase();
				  return name.endsWith(".zip") || name.endsWith(".jar");
				}
		});
		for (int i = 0; i < files.length; i++) {
			install(files[i]);
		}
	}
	
	public void install(File f) {
		JbiContainer container = JbiContainer.Factory.getInstance();
		File wrkDir = container.getWorkingDir();
		File proDir = Directories.getAutoInstallProcessedDir(wrkDir);
		File fp;
		try {
			IOUtils.createDirs(proDir);
			fp = getRenamedFile(f, proDir);
			if (!f.renameTo(fp)) {
				throw new IOException();
			}
		} catch (IOException e) {
			logger.info("Could not move file " + f + " for auto installation");
			return;
		}
		ObjectName service = null;
		ObjectName installer = null;
		MBeanServer server = container.getMBeanServer();
		try {
			service = container.createMBeanName(null, "service", "install");
			installer = (ObjectName) server.invoke(service, "loadNewInstaller", new Object[] { fp.toURL().toString() }, new String[] { String.class.getName() });
			server.invoke(installer, "install", null, null);
			new File(fp.getAbsolutePath() + ".success").createNewFile();
		} catch (Throwable t) {
			try {
				File out = new File(fp.getAbsolutePath() + ".failed");
				FileOutputStream fos = new FileOutputStream(out);
				t.printStackTrace(new PrintStream(fos));
				fos.close();
			} catch (IOException e) {
				logger.info("Could not write result file for " + f, e);
			}
		} finally {
			if (service != null && installer != null) {
				try {
					String name = installer.getKeyProperty("component");
					server.invoke(service, "unloadInstaller", new Object[] { name, Boolean.FALSE }, new String[] { String.class.getName(), boolean.class.getName() });
				} catch (Exception e) {
					logger.info("Could not unload installer", e);
				}
			}
		}
	}
	
	public File getRenamedFile(File f, File d) {
		File fp = new File(d, f.getName());
		if (fp.exists() || new File(fp.getAbsolutePath() + ".failed").exists() ||
						   new File(fp.getAbsolutePath() + ".success").exists()) {
			String name = fp.getName().substring(0, fp.getName().lastIndexOf('.'));
			String ext  = fp.getName().substring(fp.getName().lastIndexOf('.') + 1);
			int counter = 0;
			do {
				fp = new File(d, name + "." + counter + "." + ext);
			} while (fp.exists() || 
					 new File(fp.getAbsolutePath() + ".failed").exists() ||
					 new File(fp.getAbsolutePath() + ".success").exists());
		}
		return fp;
	}

	public int getPollingFrequency() {
		return pollingFrequency;
	}

	public void setPollingFrequency(int pollingFrequency) {
		this.pollingFrequency = pollingFrequency;
	}
	
}
