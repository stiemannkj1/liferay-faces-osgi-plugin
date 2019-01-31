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
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;


/**
 * @author  Kyle Stiemann
 */
@Mojo(
	name = "generate-package-import-jar", defaultPhase = LifecyclePhase.PREPARE_PACKAGE,
	requiresDependencyCollection = ResolutionScope.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE
)
public final class LiferayFacesOSGiPluginMojo extends AbstractMojo {

	// Package-Private Constants
	/* package-private */ static final String META_INF = "META-INF/";
	/* package-private */ static final String META_INF_SERVICES = META_INF + "services/";
	/* package-private */ static final String FACES_CONFIG_XML = "faces-config.xml";
	/* package-private */ static final String META_INF_FACES_CONFIG_XML = META_INF + FACES_CONFIG_XML;
	/* package-private */ static final String PLUGIN_ARTIFACT_ID = "com.liferay.faces.osgi.plugin";
	/* package-private */ static final String PROJECT_BUILD_DIRECTORY_PROPERTY = "${project.build.directory}";
	/* package-private */ static final String SERVLET_CONTAINER_INITIALIZER_FILE_PATH = META_INF_SERVICES +
		"javax.servlet.ServletContainerInitializer";

	// Private Data Members
	@Parameter(defaultValue = "${project.build.finalName}", required = true)
	private String buildFinalName;

	@Parameter(defaultValue = PROJECT_BUILD_DIRECTORY_PROPERTY, required = true)
	private File outputDirectory;

	@Parameter(defaultValue = "${project}", required = true)
	private MavenProject project;

	@Override
	public final void execute() throws MojoExecutionException {

		Log log = getLog();
		Set<JarFile> facesJars = new HashSet<JarFile>();
		Set<Artifact> artifacts = project.getArtifacts();

		for (Artifact artifact : artifacts) {

			String scope = artifact.getScope();
			String type = artifact.getType();

			if (Artifact.SCOPE_PROVIDED.equalsIgnoreCase(scope) && "jar".equalsIgnoreCase(type)) {

				try {
					facesJars.add(new JarFile(artifact.getFile()));
				}
				catch (IOException e) {
					log.error("Failed to convert " + artifact.getGroupId() + ":" + artifact.getArtifactId() +
						" into JarFile. Unable to add imports from this Faces Jar.", e);
				}
			}
		}

		// Add JAR to maven-war-plugin config
		String warFolderName = buildFinalName.replaceFirst(".war$", "");
		String warLibDirectoryPath = JarGeneratorUtil.getFilePath(outputDirectory.toString(), warFolderName, "WEB-INF",
				"lib");
		File warLibDirectory = new File(warLibDirectoryPath);

		try {

			Set<String> defaultServletContainerInitializerClassNames = ServletContainerInitializerUtil
				.getDefaultClassNames();
			Set<String> importedClasses = FacesXMLUtil.getClassNames(facesJars, log);
			JarGeneratorUtil.generateImportJar(warLibDirectory, defaultServletContainerInitializerClassNames,
				importedClasses);
		}
		catch (IOException | ParserConfigurationException | XPathException e) {
			throw new MojoExecutionException("Failed to generate import JAR.", e);
		}
	}
}
