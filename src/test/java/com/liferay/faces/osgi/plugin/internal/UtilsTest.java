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

import org.junit.Assert;
import org.junit.Test;



/**
 * @author  Kyle Stiemann
 */
public final class UtilsTest {

	private static Set<String> getClassNames(Set<Class<?>> classes) {

		LinkedHashSet<String> classNames = new LinkedHashSet<String>();

		for (Class<?> clazz : classes) {
			classNames.add(clazz.getName());
		}

		return Collections.unmodifiableSet(classNames);
	}

	private static <T> Set<T> unmodifiableSet(T... t) {
		return unmodifiableSet(Collections.emptySet(), t);
	}

	private static <T> Set<T> unmodifiableSet(Set<T> set, T... t) {

		LinkedHashSet<T> linkedHashSet = new LinkedHashSet<T>(set);
		linkedHashSet.addAll(Arrays.asList(t));

		return Collections.unmodifiableSet(linkedHashSet);
	}

	private static Set<String> getExpectedImportedFacesPackages(Set<Class<?>> importedClasses) {

		ImportedFacesPackages importedFacesPackages = new ImportedFacesPackages();
		Set<String> defaultImportedFacesPackges = importedFacesPackages.getImportedPackages();
		Set<String> expectedImportedFacesPackages = new LinkedHashSet<String>(defaultImportedFacesPackges);
		expectedImportedFacesPackages.addAll(getImportedFacesPackages(importedClasses));

		return Collections.unmodifiableSet(expectedImportedFacesPackages);
	}

	private static Set<String> getImportedFacesPackages(Set<Class<?>> importedClasses) {

		Set<String> importedFacesPackages = new LinkedHashSet<String>();

		for (Class<?> importedClass : importedClasses) {
			importedFacesPackages.add(importedClass.getPackage().getName());
		}

		return Collections.unmodifiableSet(importedFacesPackages);
	}

	@Test
	public final void testImportedFacesPackagessClassUtil() throws IOException, LinkageError,
		ReflectiveOperationException, SecurityException {

		Set<Class<?>> importedClasses = unmodifiableSet(String.class, Integer.class, Boolean.class);
		Set<String> expectedImportedFacesPackages = getExpectedImportedFacesPackages(importedClasses);
		ModifiedImportedPackagesClassLoader modifiedImportedPackagesClassLoader =
			new ModifiedImportedPackagesClassLoader();
		Set<String> modifiedImportedPackages = modifiedImportedPackagesClassLoader.getModifiedImportedPackages(
			ImportedFacesPackagesClassByteCodeUtil.getByteCode(getClassNames(importedClasses)));

		Assert.assertEquals(expectedImportedFacesPackages, modifiedImportedPackages);
	}

	@Test
	public final void testJarGeneratorUtil() throws IOException, NoSuchAlgorithmException,
		ReflectiveOperationException {

		Path temporaryDirectory = null;

		try {

			Set<Class<?>> importedClasses = unmodifiableSet(String.class, Integer.class);
			Set<String> expectedImportedFacesPackages = getExpectedImportedFacesPackages(importedClasses);
			Set<String> importedFacesPackages = getImportedFacesPackages(importedClasses);
			temporaryDirectory = Files.createTempDirectory("com.liferay.faces.osgi.plugin-");
			JarGeneratorUtil.generateImportJar(temporaryDirectory.toFile(),
				unmodifiableSet("com.liferay.faces.Initializer"), importedFacesPackages);
			Path generatedJarPath = Files.find(temporaryDirectory, 10, (path, basicFileAttributes) -> {

				String fileName = path.toFile().getName();
				return fileName.startsWith(JarGeneratorUtil.PLUGIN_ARTIFACT_ID) &&
					fileName.endsWith(".jar");
			}).findFirst().get();
			URLClassLoader urlClassLoader = new ChildFirstURLClassLoader(new URL[]{ generatedJarPath.toUri().toURL() });
			Class<?> modifiedImportedFacesPackagesClass =
				urlClassLoader.loadClass(ImportedFacesPackages.class.getName());
			Set<String> modifiedImportedPackages =
				ModifiedImportedPackagesClassLoader.getModifiedImportedPackages(modifiedImportedFacesPackagesClass);
			Assert.assertEquals(expectedImportedFacesPackages, modifiedImportedPackages);
		}
		finally {

			Files.walk(temporaryDirectory)
                .map(Path::toFile)
                .sorted(Comparator.reverseOrder())
                .forEach(File::delete);
		}
	}

	private static final class ModifiedImportedPackagesClassLoader extends ClassLoader {

		private Set<String> getModifiedImportedPackages(byte[] modifiedImportedFacesPackagesClassBytes)
			throws LinkageError, ReflectiveOperationException, SecurityException {

			Class<?> modifiedImportedFacesPackagesClass = defineClass(ImportedFacesPackages.class.getName(),
					modifiedImportedFacesPackagesClassBytes, 0, modifiedImportedFacesPackagesClassBytes.length);
			resolveClass(modifiedImportedFacesPackagesClass);

			return getModifiedImportedPackages(modifiedImportedFacesPackagesClass);
		}

		private static Set<String> getModifiedImportedPackages(Class<?> modifiedImportedFacesPackagesClass)
			throws ReflectiveOperationException, SecurityException {

			Method getImportedPackagesMethod = modifiedImportedFacesPackagesClass.getMethod("getImportedPackages");
			Object modifiedImportedFacesPackagesInstance = modifiedImportedFacesPackagesClass.newInstance();

			return (Set<String>) getImportedPackagesMethod.invoke(modifiedImportedFacesPackagesInstance);
		}
	}

	private static final class ChildFirstURLClassLoader extends URLClassLoader {

		private ChildFirstURLClassLoader(URL[] urls) {
			super(urls);
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {

			Class<?> loadedClass = null;

			try {

				loadedClass = findClass(name);
				resolveClass(loadedClass);
			}
			catch (ClassNotFoundException e1) {

				try {
					loadedClass = super.loadClass(name);
				}
				catch (ClassNotFoundException e2) {

					ClassLoader systemClassLoader = getSystemClassLoader();
					loadedClass = systemClassLoader.loadClass(name);
				}
			}

			return loadedClass;
		}
	}
}
