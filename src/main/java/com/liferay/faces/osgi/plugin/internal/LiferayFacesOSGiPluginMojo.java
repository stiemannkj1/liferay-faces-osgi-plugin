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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.xml.bind.DatatypeConverter;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.util.xml.Xpp3Dom;


/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "liferay-faces-osgi-plugin", defaultPhase = LifecyclePhase.PACKAGE)
public class LiferayFacesOSGiPluginMojo extends AbstractMojo {

	// Private Constants
	private static final String META_INF = "META-INF/";
	private static final String META_INF_SERVICES = META_INF + "services/";
	private static final String FACES_CONFIG_XML = "faces-config.xml";
	private static final String META_INF_FACES_CONFIG_XML = META_INF + FACES_CONFIG_XML;
	private static final String PLUGIN_ARTIFACT_ID = "com.liferay.faces.osgi.plugin";
	private static final String PROJECT_BUILD_DIRECTORY_PROPERTY = "${project.build.directory}";
	private static final String MD5SUM_TOKEN = "{MD5SUM}";
	private static final String GENERATED_JAR_FILE_NAME_TEMPLATE = PLUGIN_ARTIFACT_ID + ".generated-" + MD5SUM_TOKEN +
		".jar";
	private static final String SERVLET_CONTAINER_INITIALIZER_FILE_PATH = META_INF_SERVICES +
		"javax.servlet.ServletContainerInitializer";

	// Private Data Members
	@Parameter(defaultValue = PROJECT_BUILD_DIRECTORY_PROPERTY, required = true)
	private File outputDirectory;

	@Parameter(defaultValue = "${project}", required = true)
	private MavenProject project;

	public void execute() throws MojoExecutionException {

		Set<String> servletConainerIntializerClasses = new LinkedHashSet<String>();
		InputStream inputStream = null;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;

		try {

			inputStream = LiferayFacesOSGiPluginMojo.class.getResourceAsStream("/" +
					SERVLET_CONTAINER_INITIALIZER_FILE_PATH);
			inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
			bufferedReader = new BufferedReader(inputStreamReader);

			bufferedReader.lines().filter((String line) -> { return !line.trim().isEmpty(); }).forEach((line) -> {
				servletConainerIntializerClasses.add(line);
			});
		}
		catch (UncheckedIOException e) {
			throw new MojoExecutionException("TODO", e);
		}
		finally {

			CloseableUtil.close(bufferedReader);
			CloseableUtil.close(inputStreamReader);
			CloseableUtil.close(inputStream);
		}

		Set<String> importedClasses = new HashSet<String>();
		importedClasses.addAll(servletConainerIntializerClasses);

		Set<Artifact> artifacts = project.getArtifacts();

		for (Artifact artifact : artifacts) {

			String scope = artifact.getScope();
			String type = artifact.getType();

			if (Artifact.SCOPE_PROVIDED.equalsIgnoreCase(scope) && "jar".equalsIgnoreCase(type)) {

				try {

					File artifactFile = artifact.getFile();
					JarFile artifactJarFile = new JarFile(artifactFile);
					Enumeration<JarEntry> entries = artifactJarFile.entries();

					while (entries.hasMoreElements()) {

						JarEntry jarEntry = entries.nextElement();
						String name = jarEntry.getName();
						boolean facesConfig = META_INF_FACES_CONFIG_XML.equals(name) ||
							(name.startsWith(META_INF) && name.endsWith("." + FACES_CONFIG_XML));

						if (facesConfig || (name.startsWith(META_INF) && name.endsWith(".taglib.xml"))) {

							inputStream = artifactJarFile.getInputStream(jarEntry);

							if (facesConfig) {
								// TODO add imports
							}
							else {
								// TODO add imports
							}
						}
					}
				}
				catch (IOException e) {
					getLog().error("TODO", e);
				}
				finally {
					CloseableUtil.close(inputStream);
				}
			}
		}

		// TODO
		// Use ASM java to modify ImportedFacesPackages.class to add all imports in a static initializer
		Manifest manifest = new Manifest();
		Attributes mainAttributes = manifest.getMainAttributes();
		mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");

		File generatedJarOutputDir = new File(outputDirectory, PLUGIN_ARTIFACT_ID);
		generatedJarOutputDir.mkdirs();

		File generatedJarFile = new File(generatedJarOutputDir, GENERATED_JAR_FILE_NAME_TEMPLATE);
		FileOutputStream fileOutputStream = null;
		JarOutputStream jarOutputStream = null;
		String generatedJarFileName;

		try {

			fileOutputStream = new FileOutputStream(generatedJarFile);
			jarOutputStream = new JarOutputStream(fileOutputStream, manifest);

			String importedFacesPackagesDirectoryName = ImportedFacesPackages.class.getPackage().getName();
			importedFacesPackagesDirectoryName = importedFacesPackagesDirectoryName.replace(".", "/") + "/";
			jarOutputStream.putNextEntry(new JarEntry(importedFacesPackagesDirectoryName));
			jarOutputStream.closeEntry();
			jarOutputStream.putNextEntry(new JarEntry(META_INF_SERVICES));
			jarOutputStream.closeEntry();
			jarOutputStream.putNextEntry(new JarEntry(SERVLET_CONTAINER_INITIALIZER_FILE_PATH));

			PrintWriter printWriter = new PrintWriter(jarOutputStream);

			for (String servletContainerIntializerClass : servletConainerIntializerClasses) {
				printWriter.println(servletContainerIntializerClass);
			}

			printWriter.println();
			jarOutputStream.closeEntry();

			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			byte[] bytes = Files.readAllBytes(generatedJarFile.toPath());
			String generatedJarMD5Sum = DatatypeConverter.printHexBinary(messageDigest.digest(bytes));
			generatedJarFileName = GENERATED_JAR_FILE_NAME_TEMPLATE.replace(MD5SUM_TOKEN, generatedJarMD5Sum);

			boolean renamedFile = generatedJarFile.renameTo(new File(generatedJarOutputDir, generatedJarFileName));

			if (!renamedFile) {
				throw new IOException("TODO");
			}
		}
		catch (IOException | NoSuchAlgorithmException e) {
			throw new MojoExecutionException("TODO", e);
		}
		finally {

			CloseableUtil.close(jarOutputStream);
			CloseableUtil.close(fileOutputStream);
		}

		// Add JAR to maven-war-plugin config

		//J-
		/*
		<webResource>
			<!-- ...your config here... -->
			<resource>
				<targetPath>WEB-INF/lib</targetPath>
				<directory>${project.build.directory}/com.liferay.faces.osgi.plugin</directory>
				<includes>
					<include>com.liferay.faces.osgi.plugin.generate-${generated.jar.md5}.jar</include>
				</includes>
			</resource>
		</webResource>
		*/
		//J+
		Plugin plugin = project.getPlugin("org.apache.maven.plugins:maven-war-plugin");
		Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
		Xpp3Dom webResources = configuration.getChild("webResources");

		if (webResources == null) {

			webResources = new Xpp3Dom("webResources");
			configuration.addChild(webResources);
		}

		Xpp3Dom resource = new Xpp3Dom("resource");
		webResources.addChild(resource);

		Xpp3Dom targetPath = new Xpp3Dom("targetPath");
		targetPath.setValue("WEB-INF/lib");
		resource.addChild(targetPath);

		Xpp3Dom directory = new Xpp3Dom("directory");
		directory.setValue(PROJECT_BUILD_DIRECTORY_PROPERTY + "/" + PLUGIN_ARTIFACT_ID);
		resource.addChild(directory);

		Xpp3Dom includes = new Xpp3Dom("includes");
		resource.addChild(includes);

		Xpp3Dom xpp3Dom = new Xpp3Dom("include");
		xpp3Dom.setValue(generatedJarFileName);
		// Add modified ImportedFacesPackages.class and ServletContainerInitializer to JAR Add JAR to WEB-INF/lib of
		// WAR.
	}
}
