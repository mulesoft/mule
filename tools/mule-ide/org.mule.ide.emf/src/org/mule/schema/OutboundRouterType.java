/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.eclipse.emf.ecore.util.FeatureMap;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Outbound Router Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.OutboundRouterType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.OutboundRouterType#getCatchAllStrategy <em>Catch All Strategy</em>}</li>
 *   <li>{@link org.mule.schema.OutboundRouterType#getRouter <em>Router</em>}</li>
 *   <li>{@link org.mule.schema.OutboundRouterType#isMatchAll <em>Match All</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getOutboundRouterType()
 * @model extendedMetaData="name='outbound-routerType' kind='mixed'"
 * @generated
 */
public interface OutboundRouterType extends EObject {
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
	 * @see org.mule.schema.MulePackage#getOutboundRouterType_Mixed()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
	 *        extendedMetaData="kind='elementWildcard' name=':mixed'"
	 * @generated
	 */
	FeatureMap getMixed();

	/**
	 * Returns the value of the '<em><b>Catch All Strategy</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Catch All Strategy</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Catch All Strategy</em>' containment reference.
	 * @see #setCatchAllStrategy(CatchAllStrategyType)
	 * @see org.mule.schema.MulePackage#getOutboundRouterType_CatchAllStrategy()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='catch-all-strategy' namespace='##targetNamespace'"
	 * @generated
	 */
	CatchAllStrategyType getCatchAllStrategy();

	/**
	 * Sets the value of the '{@link org.mule.schema.OutboundRouterType#getCatchAllStrategy <em>Catch All Strategy</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Catch All Strategy</em>' containment reference.
	 * @see #getCatchAllStrategy()
	 * @generated
	 */
	void setCatchAllStrategy(CatchAllStrategyType value);

	/**
	 * Returns the value of the '<em><b>Router</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.RouterType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Router</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Router</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getOutboundRouterType_Router()
	 * @model type="org.mule.schema.RouterType" containment="true" resolveProxies="false" required="true" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='router' namespace='##targetNamespace'"
	 * @generated
	 */
	EList getRouter();

	/**
	 * Returns the value of the '<em><b>Match All</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Match All</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Match All</em>' attribute.
	 * @see #isSetMatchAll()
	 * @see #unsetMatchAll()
	 * @see #setMatchAll(boolean)
	 * @see org.mule.schema.MulePackage#getOutboundRouterType_MatchAll()
	 * @model default="false" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
	 *        extendedMetaData="kind='attribute' name='matchAll' namespace='##targetNamespace'"
	 * @generated
	 */
	boolean isMatchAll();

	/**
	 * Sets the value of the '{@link org.mule.schema.OutboundRouterType#isMatchAll <em>Match All</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Match All</em>' attribute.
	 * @see #isSetMatchAll()
	 * @see #unsetMatchAll()
	 * @see #isMatchAll()
	 * @generated
	 */
	void setMatchAll(boolean value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.OutboundRouterType#isMatchAll <em>Match All</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetMatchAll()
	 * @see #isMatchAll()
	 * @see #setMatchAll(boolean)
	 * @generated
	 */
	void unsetMatchAll();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.OutboundRouterType#isMatchAll <em>Match All</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Match All</em>' attribute is set.
	 * @see #unsetMatchAll()
	 * @see #isMatchAll()
	 * @see #setMatchAll(boolean)
	 * @generated
	 */
	boolean isSetMatchAll();

} // OutboundRouterType
