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
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.liferay.faces.osgi.plugin.internal.a.TestClassA;
import com.liferay.faces.osgi.plugin.internal.b.TestClassB;


/**
 * @author  Kyle Stiemann
 */
public final class UtilsTest {

	private static Set<String> getExpectedImportedFacesPackages(Set<Class<?>> importedClasses) {

		ImportedFacesPackages importedFacesPackages = new ImportedFacesPackages();
		Set<String> defaultImportedFacesPackges = importedFacesPackages.getImportedPackages();
		Set<String> expectedImportedFacesPackages = new LinkedHashSet<String>(defaultImportedFacesPackges);

		for (Class<?> importedClass : importedClasses) {
			expectedImportedFacesPackages.add(importedClass.getPackage().getName());
		}

		return Collections.unmodifiableSet(expectedImportedFacesPackages);
	}

	private static <T> Set<T> unmodifiableSet(T... t) {
		return unmodifiableSet(Collections.emptySet(), t);
	}

	private static <T> Set<T> unmodifiableSet(Set<T> set, T... t) {

		LinkedHashSet<T> linkedHashSet = new LinkedHashSet<T>(set);
		linkedHashSet.addAll(Arrays.asList(t));

		return Collections.unmodifiableSet(linkedHashSet);
	}

	@Test
	public final void testImportedFacesPackagessClassUtil() throws IOException, LinkageError,
		ReflectiveOperationException, SecurityException {

		Set<Class<?>> importedClasses = unmodifiableSet(TestClassA.class, TestClassB.class);
		Set<String> expectedImportedFacesPackages = getExpectedImportedFacesPackages(importedClasses);
		ModifiedImportFacesPackagesClassLoader modifiedImportFacesPackagesClassLoader =
			new ModifiedImportFacesPackagesClassLoader(UtilsTest.class.getClassLoader());
		Set<String> modifiedImportedPackages = modifiedImportFacesPackagesClassLoader.getModifiedImportedPackages(
				importedClasses);

		Assert.assertEquals(expectedImportedFacesPackages, modifiedImportedPackages);
	}

	@Test
	public final void testJarGeneratorUtil() throws IOException, NoSuchAlgorithmException,
		ReflectiveOperationException {

		Path temporaryDirectory = null;

		try {

			Set<Class<?>> importedClasses = unmodifiableSet(TestClassA.class, TestClassB.class);
			Set<String> expectedImportedFacesPackages = getExpectedImportedFacesPackages(importedClasses);
			temporaryDirectory = Files.createTempDirectory("com.liferay.faces.osgi.plugin-");

			Set<String> importedClassesAsString = importedClasses.stream().map((clazz) -> { return clazz.getName(); })
				.collect(Collectors.toSet());
			JarGeneratorUtil.generateImportJar(temporaryDirectory.toFile(),
				unmodifiableSet("com.liferay.faces.Initializer"), importedClassesAsString);

			Path generatedJarPath = Files.find(temporaryDirectory, 10,
					(path, basicFileAttributes) -> {

						String fileName = path.toFile().getName();

						return fileName.startsWith(JarGeneratorUtil.PLUGIN_ARTIFACT_ID) && fileName.endsWith(".jar");
					}).findFirst().get();
			ModifiedImportFacesPackagesClassLoader modifiedImportFacesPackagesClassLoader =
				new ModifiedImportFacesPackagesClassLoader(generatedJarPath);
			Set<String> modifiedImportedPackages = modifiedImportFacesPackagesClassLoader.getModifiedImportedPackages(
					importedClasses);
			Assert.assertEquals(expectedImportedFacesPackages, modifiedImportedPackages);
		}
		finally {

			Files.walk(temporaryDirectory).map(Path::toFile).sorted(Comparator.reverseOrder()).forEach(File::delete);
		}
	}

	private static final class ModifiedImportFacesPackagesClassLoader extends URLClassLoader {

		private ModifiedImportFacesPackagesClassLoader(Path generatedJarPath) throws IOException {
			super(new URL[] { generatedJarPath.toUri().toURL() }, null);
		}

		private ModifiedImportFacesPackagesClassLoader(ClassLoader parent) {
			super(new URL[] {}, parent);
		}

		private Set<String> getModifiedImportedPackages(Set<Class<?>> importedClasses) throws IOException,
			ReflectiveOperationException, SecurityException {

			Set<String> additionalImportedClassPackages = new LinkedHashSet<String>();

			for (Class<?> importedClass : importedClasses) {

				byte[] classBytes = ClassFileUtil.getClassBytes(importedClass);
				reinitializeClassForCurrentClassLoader(importedClass, classBytes);
				additionalImportedClassPackages.add(importedClass.getName());
			}

			additionalImportedClassPackages = Collections.unmodifiableSet(additionalImportedClassPackages);

			byte[] classBytes = ImportedFacesPackagesByteCodeUtil.getModifiedByteCode(additionalImportedClassPackages);
			Class<?> modifiedImportedFacesPackagesClass = reinitializeClassForCurrentClassLoader(
					ImportedFacesPackages.class, classBytes);

			Method getImportedPackagesMethod = modifiedImportedFacesPackagesClass.getMethod("getImportedPackages");
			Object modifiedImportedFacesPackagesInstance = modifiedImportedFacesPackagesClass.newInstance();

			return (Set<String>) getImportedPackagesMethod.invoke(modifiedImportedFacesPackagesInstance);
		}

		private Class<?> reinitializeClassForCurrentClassLoader(Class<?> clazz, byte[] classBytes) throws IOException {

			Package package_ = clazz.getPackage();
			String packageName = package_.getName();

			if (getPackage(packageName) == null) {

				String specificationTitle = package_.getSpecificationTitle();
				String specificationVersion = package_.getSpecificationVersion();
				String specificationVendor = package_.getSpecificationVendor();
				String implementationTitle = package_.getImplementationTitle();
				String implementationVersion = package_.getImplementationVersion();
				String implementationVendor = package_.getImplementationVendor();
				definePackage(packageName, specificationTitle, specificationVersion, specificationVendor,
					implementationTitle, implementationVersion, implementationVendor, null);
			}

			Class<?> reinitializedClass = defineClass(clazz.getName(), classBytes, 0, classBytes.length);
			resolveClass(reinitializedClass);

			return reinitializedClass;
		}
	}
}
