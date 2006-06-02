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
 * A representation of the model object '<em><b>Transformer Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.TransformerType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.TransformerType#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.mule.schema.TransformerType#getClassName <em>Class Name</em>}</li>
 *   <li>{@link org.mule.schema.TransformerType#isIgnoreBadInput <em>Ignore Bad Input</em>}</li>
 *   <li>{@link org.mule.schema.TransformerType#getName <em>Name</em>}</li>
 *   <li>{@link org.mule.schema.TransformerType#getRef <em>Ref</em>}</li>
 *   <li>{@link org.mule.schema.TransformerType#getReturnClass <em>Return Class</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getTransformerType()
 * @model extendedMetaData="name='transformerType' kind='mixed'"
 * @generated
 */
public interface TransformerType extends EObject {
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
	 * @see org.mule.schema.MulePackage#getTransformerType_Mixed()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
	 *        extendedMetaData="kind='elementWildcard' name=':mixed'"
	 * @generated
	 */
	FeatureMap getMixed();

	/**
	 * Returns the value of the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Properties</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Properties</em>' containment reference.
	 * @see #setProperties(PropertiesType)
	 * @see org.mule.schema.MulePackage#getTransformerType_Properties()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='properties' namespace='##targetNamespace'"
	 * @generated
	 */
	PropertiesType getProperties();

	/**
	 * Sets the value of the '{@link org.mule.schema.TransformerType#getProperties <em>Properties</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Properties</em>' containment reference.
	 * @see #getProperties()
	 * @generated
	 */
	void setProperties(PropertiesType value);

	/**
	 * Returns the value of the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Class Name</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Class Name</em>' attribute.
	 * @see #setClassName(String)
	 * @see org.mule.schema.MulePackage#getTransformerType_ClassName()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='className' namespace='##targetNamespace'"
	 * @generated
	 */
	String getClassName();

	/**
	 * Sets the value of the '{@link org.mule.schema.TransformerType#getClassName <em>Class Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Class Name</em>' attribute.
	 * @see #getClassName()
	 * @generated
	 */
	void setClassName(String value);

	/**
	 * Returns the value of the '<em><b>Ignore Bad Input</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Ignore Bad Input</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Ignore Bad Input</em>' attribute.
	 * @see #isSetIgnoreBadInput()
	 * @see #unsetIgnoreBadInput()
	 * @see #setIgnoreBadInput(boolean)
	 * @see org.mule.schema.MulePackage#getTransformerType_IgnoreBadInput()
	 * @model unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
	 *        extendedMetaData="kind='attribute' name='ignoreBadInput' namespace='##targetNamespace'"
	 * @generated
	 */
	boolean isIgnoreBadInput();

	/**
	 * Sets the value of the '{@link org.mule.schema.TransformerType#isIgnoreBadInput <em>Ignore Bad Input</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Ignore Bad Input</em>' attribute.
	 * @see #isSetIgnoreBadInput()
	 * @see #unsetIgnoreBadInput()
	 * @see #isIgnoreBadInput()
	 * @generated
	 */
	void setIgnoreBadInput(boolean value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.TransformerType#isIgnoreBadInput <em>Ignore Bad Input</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetIgnoreBadInput()
	 * @see #isIgnoreBadInput()
	 * @see #setIgnoreBadInput(boolean)
	 * @generated
	 */
	void unsetIgnoreBadInput();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.TransformerType#isIgnoreBadInput <em>Ignore Bad Input</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Ignore Bad Input</em>' attribute is set.
	 * @see #unsetIgnoreBadInput()
	 * @see #isIgnoreBadInput()
	 * @see #setIgnoreBadInput(boolean)
	 * @generated
	 */
	boolean isSetIgnoreBadInput();

	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Name</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see org.mule.schema.MulePackage#getTransformerType_Name()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
	 *        extendedMetaData="kind='attribute' name='name' namespace='##targetNamespace'"
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link org.mule.schema.TransformerType#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Ref</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Ref</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Ref</em>' attribute.
	 * @see #setRef(String)
	 * @see org.mule.schema.MulePackage#getTransformerType_Ref()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='ref' namespace='##targetNamespace'"
	 * @generated
	 */
	String getRef();

	/**
	 * Sets the value of the '{@link org.mule.schema.TransformerType#getRef <em>Ref</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Ref</em>' attribute.
	 * @see #getRef()
	 * @generated
	 */
	void setRef(String value);

	/**
	 * Returns the value of the '<em><b>Return Class</b></em>' attribute.
	 * The default value is <code>"java.lang.Object"</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Return Class</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Return Class</em>' attribute.
	 * @see #isSetReturnClass()
	 * @see #unsetReturnClass()
	 * @see #setReturnClass(String)
	 * @see org.mule.schema.MulePackage#getTransformerType_ReturnClass()
	 * @model default="java.lang.Object" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='returnClass' namespace='##targetNamespace'"
	 * @generated
	 */
	String getReturnClass();

	/**
	 * Sets the value of the '{@link org.mule.schema.TransformerType#getReturnClass <em>Return Class</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Return Class</em>' attribute.
	 * @see #isSetReturnClass()
	 * @see #unsetReturnClass()
	 * @see #getReturnClass()
	 * @generated
	 */
	void setReturnClass(String value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.TransformerType#getReturnClass <em>Return Class</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetReturnClass()
	 * @see #getReturnClass()
	 * @see #setReturnClass(String)
	 * @generated
	 */
	void unsetReturnClass();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.TransformerType#getReturnClass <em>Return Class</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Return Class</em>' attribute is set.
	 * @see #unsetReturnClass()
	 * @see #getReturnClass()
	 * @see #setReturnClass(String)
	 * @generated
	 */
	boolean isSetReturnClass();

} // TransformerType
