/**
 * Copyright (c) 2000-2019 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package com.liferay.faces.osgi.plugin.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.lang.model.SourceVersion;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.plugin.logging.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;


/**
 * @author  Kyle Stiemann
 */
/* package-private */ final class FacesXMLUtil {

	private FacesXMLUtil() {
		throw new AssertionError();
	}

	/* package-private */ static Set<String> getClassNames(Set<File> facesJars, Log log) throws ParserConfigurationException,
		XPathException {

		Set<String> classNames = new HashSet<String>();

		if (!facesJars.isEmpty()) {

			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			XPathFactory xpathFactory = XPathFactory.newInstance();
			XPath xPath = xpathFactory.newXPath();
			XPathExpression xPathExpression = xPath.compile("//*[count(./*) = 0][normalize-space(text())]");

			for (File facesJar : facesJars) {

				JarFile facesJarFile = null;

				try {
					facesJarFile = new JarFile(facesJar);
				}
				catch (IOException e) {

					CloseableUtil.close(facesJarFile);
					log.error("Failed to convert " + facesJar.getName() +
						" into JarFile. Unable to add imports from this Faces Jar.", e);

					continue;
				}

				Enumeration<JarEntry> entries = facesJarFile.entries();

				while (entries.hasMoreElements()) {

					JarEntry jarEntry = entries.nextElement();
					String name = jarEntry.getName();
					boolean facesConfig = LiferayFacesOSGiPluginMojo.META_INF_FACES_CONFIG_XML.equals(name) ||
						(name.startsWith(LiferayFacesOSGiPluginMojo.META_INF) &&
							name.endsWith("." + LiferayFacesOSGiPluginMojo.FACES_CONFIG_XML));

					if (facesConfig ||
							(name.startsWith(LiferayFacesOSGiPluginMojo.META_INF) && name.endsWith(".taglib.xml"))) {

						try {

							ClassLoader classLoader = null;

							if (!facesConfig) {
								classLoader = new URLClassLoader(new URL[] { facesJar.toURI().toURL() },
										FacesXMLUtil.class.getClassLoader());
							}

							classNames.addAll(getClassNames(facesJarFile, jarEntry, classLoader, documentBuilder,
									xPathExpression));
						}
						catch (IOException e) {
							log.error("Failed to read " + name + " from " + facesJar.getName() +
								". Unable to add imports from this file.", e);
						}
					}
				}
			}
		}

		return Collections.unmodifiableSet(classNames);
	}

	private static Set<String> getClassNames(JarFile facesJar, JarEntry xmlJarEntry, ClassLoader jarClassLoader,
		DocumentBuilder documentBuilder, XPathExpression xPathExpression) throws IOException {

		Set<String> classNames = new HashSet<String>();
		InputStream inputStream = facesJar.getInputStream(xmlJarEntry);

		try {

			Document document = documentBuilder.parse(inputStream);
			NodeList nodeList = (NodeList) xPathExpression.evaluate(document, XPathConstants.NODESET);
			int nodeListLength = nodeList.getLength();

			for (int i = 0; i < nodeListLength; i++) {

				Node node = nodeList.item(i);
				String textContent = node.getTextContent();

				// Remove Generic Data
				textContent = textContent.replaceAll("[<][\\S\\s]*[>]", "");

				if (textContent.contains(".") && !SourceVersion.isKeyword(textContent) &&
						SourceVersion.isName(textContent)) {

					try {

						if (jarClassLoader != null) {
							Class.forName(textContent, false, jarClassLoader);
						}

						classNames.add(textContent);
					}
					catch (ClassNotFoundException e) {
						// Do nothing.
					}
				}
			}

			return Collections.unmodifiableSet(classNames);
		}
		catch (SAXException | XPathException e) {
			throw new IOException(e);
		}
		finally {
			CloseableUtil.close(inputStream);
		}
	}
}
