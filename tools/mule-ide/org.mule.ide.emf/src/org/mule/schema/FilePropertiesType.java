/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema;

import org.eclipse.emf.ecore.EObject;

import org.eclipse.emf.ecore.util.FeatureMap;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>File Properties Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.FilePropertiesType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.FilePropertiesType#getLocation <em>Location</em>}</li>
 *   <li>{@link org.mule.schema.FilePropertiesType#isOverride <em>Override</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getFilePropertiesType()
 * @model extendedMetaData="name='file-propertiesType' kind='mixed'"
 * @generated
 */
public interface FilePropertiesType extends EObject {
	/**
	 * Returns the value of the '<em><b>Mixed</b></em>' attribute list.
	 * The list contents are of type {@link org.eclipse.emf.ecore.util.FeatureMap.Entry}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Mixed</em>' attribute list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Mixed</em>' attribute list.
	 * @see org.mule.schema.MulePackage#getFilePropertiesType_Mixed()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
	 *        extendedMetaData="kind='elementWildcard' name=':mixed'"
	 * @generated
	 */
	FeatureMap getMixed();

	/**
	 * Returns the value of the '<em><b>Location</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Location</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Location</em>' attribute.
	 * @see #setLocation(String)
	 * @see org.mule.schema.MulePackage#getFilePropertiesType_Location()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
	 *        extendedMetaData="kind='attribute' name='location' namespace='##targetNamespace'"
	 * @generated
	 */
	String getLocation();

	/**
	 * Sets the value of the '{@link org.mule.schema.FilePropertiesType#getLocation <em>Location</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Location</em>' attribute.
	 * @see #getLocation()
	 * @generated
	 */
	void setLocation(String value);

	/**
	 * Returns the value of the '<em><b>Override</b></em>' attribute.
	 * The default value is <code>"true"</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Override</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Override</em>' attribute.
	 * @see #isSetOverride()
	 * @see #unsetOverride()
	 * @see #setOverride(boolean)
	 * @see org.mule.schema.MulePackage#getFilePropertiesType_Override()
	 * @model default="true" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
	 *        extendedMetaData="kind='attribute' name='override' namespace='##targetNamespace'"
	 * @generated
	 */
	boolean isOverride();

	/**
	 * Sets the value of the '{@link org.mule.schema.FilePropertiesType#isOverride <em>Override</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Override</em>' attribute.
	 * @see #isSetOverride()
	 * @see #unsetOverride()
	 * @see #isOverride()
	 * @generated
	 */
	void setOverride(boolean value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.FilePropertiesType#isOverride <em>Override</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetOverride()
	 * @see #isOverride()
	 * @see #setOverride(boolean)
	 * @generated
	 */
	void unsetOverride();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.FilePropertiesType#isOverride <em>Override</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Override</em>' attribute is set.
	 * @see #unsetOverride()
	 * @see #isOverride()
	 * @see #setOverride(boolean)
	 * @generated
	 */
	boolean isSetOverride();

} // FilePropertiesType
