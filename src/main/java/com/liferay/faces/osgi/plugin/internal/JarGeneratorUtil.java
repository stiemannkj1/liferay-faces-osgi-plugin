/**
 * Copyright (c) 2000-2019 Liferay, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.liferay.faces.osgi.plugin.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author Kyle Stiemann
 */
/* package-private */ final class JarGeneratorUtil {

	// Package-Private Constants
	/* package-private */ static final String PLUGIN_ARTIFACT_ID = "com.liferay.faces.osgi.plugin";

	// Private Constants
	private static final String META_INF = "META-INF/";
	private static final String META_INF_SERVICES = META_INF + "services/";
	private static final String MD5SUM_TOKEN = "{MD5SUM}";
	private static final String GENERATED_JAR_FILE_NAME_TEMPLATE = PLUGIN_ARTIFACT_ID + ".generated-" + MD5SUM_TOKEN +
		".jar";
	private static final String SERVLET_CONTAINER_INITIALIZER_FILE_PATH = META_INF_SERVICES +
		"javax.servlet.ServletContainerInitializer";

	private JarGeneratorUtil() {
		throw new AssertionError();
	}

	/* package-private */ static void generateImportJar(File outputDirectory,
		Set<String> servletContainerInitializerClasses, Set<String> additionalPackagesToImport) throws IOException,
		NoSuchAlgorithmException {

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
			jarOutputStream.putNextEntry(new JarEntry(importedFacesPackagesDirectoryName +
				ImportedFacesPackagesClassByteCodeUtil.IMPORTED_FACES_PACKAGES_CLASS_FILE));

			byte[] byteCode = ImportedFacesPackagesClassByteCodeUtil.getByteCode(additionalPackagesToImport);
			jarOutputStream.write(byteCode);

			jarOutputStream.closeEntry();
			jarOutputStream.putNextEntry(new JarEntry(SERVLET_CONTAINER_INITIALIZER_FILE_PATH));

			PrintWriter printWriter = new PrintWriter(jarOutputStream);

			for (String servletContainerIntializerClass : servletContainerInitializerClasses) {
				printWriter.println(servletContainerIntializerClass);
			}

			printWriter.flush();
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
		finally {

			CloseableUtil.close(jarOutputStream);
			CloseableUtil.close(fileOutputStream);
		}
	}
}
