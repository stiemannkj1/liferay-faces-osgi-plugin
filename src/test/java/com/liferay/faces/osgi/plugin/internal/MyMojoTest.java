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

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;

import com.liferay.faces.osgi.plugin.internal.LiferayFacesOSGiPluginMojo;


/**
 * @author  Kyle Stiemann
 */
public class MyMojoTest {
	@Rule
	public MojoRule rule = new MojoRule() {
			@Override
			protected void before() throws Throwable {
			}

			@Override
			protected void after() {
			}
		};

	/**
	 * @throws  Exception  if any
	 */
	@Test
	public void testSomething() throws Exception {
		File pom = new File("target/test-classes/project-to-test/");
		assertNotNull(pom);
		assertTrue(pom.exists());

		LiferayFacesOSGiPluginMojo myMojo = (LiferayFacesOSGiPluginMojo) rule.lookupConfiguredMojo(pom, "touch");
		assertNotNull(myMojo);
		myMojo.execute();

		File outputDirectory = (File) rule.getVariableValueFromObject(myMojo, "outputDirectory");
		assertNotNull(outputDirectory);
		assertTrue(outputDirectory.exists());

		File touch = new File(outputDirectory, "touch.txt");
		assertTrue(touch.exists());

	}

	/**
	 * Do not need the MojoRule.
	 */
	@WithoutMojo
	@Test
	public void testSomethingWhichDoesNotNeedTheMojoAndProbablyShouldBeExtractedIntoANewClassOfItsOwn() {
		assertTrue(true);
	}

}
