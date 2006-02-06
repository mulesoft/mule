package org.mule.ide.core.samples;

/**
 * Holds data for creating a config set.
 * 
 * @author dadams
 */
public class ConfigSet {

	/** Config set name */
	private String name;

	/** Config file path */
	private String configPath;

	/** Config file display name */
	private String configName;

	public String getConfigName() {
		return configName;
	}

	public void setConfigName(String configName) {
		this.configName = configName;
	}

	public String getConfigPath() {
		return configPath;
	}

	public void setConfigPath(String configPath) {
		this.configPath = configPath;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
