package org.alfine.refactoring;

import static org.junit.jupiter.api.Assertions.*;

import org.alfine.refactoring.framework.launch.CommandLineArguments;
import org.junit.jupiter.api.Test;

class CommandLineArgumentsTest {

	@Test
	void test() {
		String[] args = new String[] {
				"--cache" , "oppcache",
				"--lib"   , "assets/lib",
				"--src"   , "assets/src",
				"--report", "",
				"--out"   , "output",
				"--type"  , "rename"
		};
		CommandLineArguments arguments = new CommandLineArguments(args);

		assertEquals("oppcache", arguments.getCacheFolder());
		assertEquals("assets/lib", arguments.getLibFolder());
		assertEquals("assets/src", arguments.getSrcFolder());
		assertEquals("output", arguments.getOutputFolder());
	}

}
