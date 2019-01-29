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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.xml.bind.DatatypeConverter;


/**
 * @author  Kyle Stiemann
 */
/* package-private */ final class JarGeneratorUtil {

	// Package-Private Constants
	/* package-private */ static final String PLUGIN_ARTIFACT_ID = "com.liferay.faces.osgi.plugin";

	// Private Constants
	private static final String MD5SUM_TOKEN = "{MD5SUM}";
	private static final String GENERATED_JAR_FILE_NAME_TEMPLATE = LiferayFacesOSGiPluginMojo.PLUGIN_ARTIFACT_ID +
		".generated-" + MD5SUM_TOKEN + ".jar";

	private JarGeneratorUtil() {
		throw new AssertionError();
	}

	/* package-private */ static void generateImportJar(File outputDirectory, Set<String> servletContainerInitializerClasses,
		Set<String> additionalClassesToImport) throws IOException {

		Manifest manifest = new Manifest();
		Attributes mainAttributes = manifest.getMainAttributes();
		mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		outputDirectory.mkdirs();

		File generatedJarFile = new File(outputDirectory, GENERATED_JAR_FILE_NAME_TEMPLATE);
		FileOutputStream fileOutputStream = null;
		JarOutputStream jarOutputStream = null;

		try {

			fileOutputStream = new FileOutputStream(generatedJarFile);
			jarOutputStream = new JarOutputStream(fileOutputStream, manifest);

			String importedFacesPackagesDirectoryName = ImportedFacesPackages.class.getPackage().getName();
			importedFacesPackagesDirectoryName = importedFacesPackagesDirectoryName.replace(".", "/") + "/";
			jarOutputStream.putNextEntry(new JarEntry(
					importedFacesPackagesDirectoryName + ClassFileUtil.getClassFileName(ImportedFacesPackages.class)));

			Set<String> classesToImport = new LinkedHashSet<String>(servletContainerInitializerClasses);
			classesToImport.addAll(additionalClassesToImport);

			byte[] byteCode = ImportedFacesPackagesByteCodeUtil.getModifiedByteCode(classesToImport);
			jarOutputStream.write(byteCode);

			jarOutputStream.closeEntry();
			jarOutputStream.putNextEntry(new JarEntry(
					LiferayFacesOSGiPluginMojo.SERVLET_CONTAINER_INITIALIZER_FILE_PATH));

			PrintWriter printWriter = new PrintWriter(jarOutputStream);

			for (String servletContainerIntializerClass : servletContainerInitializerClasses) {
				printWriter.println(servletContainerIntializerClass);
			}

			printWriter.flush();
			jarOutputStream.closeEntry();

			MessageDigest messageDigest;

			try {
				messageDigest = MessageDigest.getInstance("MD5");
			}
			catch (NoSuchAlgorithmException e) {
				throw new IOException("Unable to generate jar due to the following error:", e);
			}

			Path generatedJarFilePath = generatedJarFile.toPath();

			byte[] bytes = Files.readAllBytes(generatedJarFilePath);
			String generatedJarMD5Sum = DatatypeConverter.printHexBinary(messageDigest.digest(bytes));
			String generatedJarMD5FileName = GENERATED_JAR_FILE_NAME_TEMPLATE.replace(MD5SUM_TOKEN, generatedJarMD5Sum);
			Path generatedJarMD5FilePath = new File(outputDirectory, generatedJarMD5FileName).toPath();
			Files.move(generatedJarFilePath, generatedJarMD5FilePath, StandardCopyOption.REPLACE_EXISTING);
		}
		finally {

			CloseableUtil.close(jarOutputStream);
			CloseableUtil.close(fileOutputStream);
		}
	}

	/* package-private */ static String getFilePath(String... dirNames) {

		StringBuilder stringBuilder = new StringBuilder();

		for (String dirName : dirNames) {

			stringBuilder.append(dirName);

			if (!dirName.endsWith(File.separator)) {
				stringBuilder.append(File.separator);
			}
		}

		return stringBuilder.toString();
	}
}
