package org.mule.ide.core.server;

import java.io.Serializable;

import org.mule.MuleServer;

/**
 * A command issued to a MuleServerController.
 * 
 * @author Derek Adams
 */
public abstract class MuleCommand implements Serializable {

	/** The unique command id */
	private String id;

	/** A description of the command */
	private String description;

	public MuleCommand(String id, String description) {
		setId(id);
		setDescription(description);
	}

	/**
	 * Execute the command on a MuleServer instance.
	 * 
	 * @param server the server instance
	 */
	public abstract void executeOn(MuleServer server);

	/**
	 * @return Returns the description.
	 */
	protected String getDescription() {
		return description;
	}

	/**
	 * @param description The description to set.
	 */
	protected void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return Returns the id.
	 */
	protected String getId() {
		return id;
	}

	/**
	 * @param id The id to set.
	 */
	protected void setId(String id) {
		this.id = id;
	}
}