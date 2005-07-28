package org.mule.jbi;

import java.io.File;

import org.mule.jbi.framework.JbiContainerImpl;

public class ServerTest {

	public static void main(String[] args) throws Exception {
		JbiContainerImpl jbi = new JbiContainerImpl();
		jbi.setWorkingDir(new File("target/.mule-jbi"));
		jbi.initialize();
		jbi.start();
	}
	
}
