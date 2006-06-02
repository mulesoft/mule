package org.mule.ide.ui.preferences;

import org.mule.ide.ui.MulePlugin;

/**
 * Preferences for various Mule settings.
 * 
 * @author dadams
 */
public class MulePreferences {

	/**
	 * Gets the default location for an external Mule root folder.
	 * 
	 * @return the path or null if not set
	 */
	public static String getDefaultExternalMuleRoot() {
		return getStringPreference(IPreferenceConstants.EXTERNAL_MULE_ROOT);
	}

	/**
	 * Set the default location for an external Mule root folder.
	 * 
	 * @param root
	 */
	public static void setDefaultExternalMuleRoot(String root) {
		setStringPreference(IPreferenceConstants.EXTERNAL_MULE_ROOT, root);
	}

	/**
	 * Gets the default classpath type preference.
	 * 
	 * @return the preference value
	 */
	public static String getDefaultClasspathChoice() {
		return getStringPreference(IPreferenceConstants.MULE_CLASSPATH_TYPE);
	}

	/**
	 * Sets the default classpath type preference.
	 * 
	 * @param choice the preference value
	 */
	public static void setDefaultClasspathChoice(String choice) {
		setStringPreference(IPreferenceConstants.MULE_CLASSPATH_TYPE, choice);
	}

	/**
	 * Get a String preference value from the preference service.
	 * 
	 * @param key the preference key
	 * @return the value or null if not found
	 */
	protected static String getStringPreference(String key) {
		return MulePlugin.getDefault().getPreferenceStore().getString(key);
	}

	/**
	 * Sets a string preference in instance scope.
	 * 
	 * @param key the preference key
	 * @param value the preference value
	 */
	protected static void setStringPreference(String key, String value) {
		MulePlugin.getDefault().getPreferenceStore().setValue(key, value);
	}
}
