package org.alfine.refactoring.framework.launch;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CommandLineArguments {

	private CommandLine cmd;

	public boolean getVerbose() {
		return cmd.hasOption("verbose");
	}

	public String getCacheFolder() {
		return cmd.getOptionValue("cache");
	}

	public String getSrcFolder() {
	    return cmd.getOptionValue("src");
	}

	public String getLibFolder() {
		return cmd.getOptionValue("lib");
	}

	public String getOutputFolder() {
	    return cmd.getOptionValue("out");
	}

	public String getRefactoringOutputReportFolder() {
		return cmd.getOptionValue("report");
	}

	public boolean getPrepare() {
		return cmd.hasOption("prepare");
	}

	public String getCompilerComplianceVersion() {
		return cmd.hasOption("compliance") ? cmd.getOptionValue("compliance") : null;
	}

	public String getRefactoringDescriptor() {
		return cmd.hasOption("descriptor") ? cmd.getOptionValue("descriptor") : null;
	}

	public boolean hasOption(String opt) {
		return cmd.hasOption(opt);
	}

	public CommandLineArguments(String[] args) {

		Options options = new Options();

		Option verbose = new Option("v", "verbose", false, "verbose execution");
        verbose.setRequired(false);
        options.addOption(verbose);

		Option prepare = new Option("p", "prepare", false, "prepare workspace and cache refactoring opportunities");
        prepare.setRequired(false);
        options.addOption(prepare);

        Option compliance = new Option("k", "compliance", true, "Compiler compliance of created java projects (see JavaCore.VERSION_<version>).");
        compliance.setRequired(false);
        options.addOption(compliance);

        Option line = new Option("d", "descriptor", true, "Refactoring opportunity cache line");
        line.setRequired(false);
        options.addOption(line);

        Option cacheFolder = new Option("c", "cache", true, "cache folder where project specific refactoring opportunity cache files are stored");
        cacheFolder.setRequired(true);
        options.addOption(cacheFolder);

        Option srcFolder = new Option("s", "src", true, "source (jar) archives folder");
        srcFolder.setRequired(true);
        options.addOption(srcFolder);

        Option libFolder = new Option("l", "lib", true, "binary (jar) archives folder");
        libFolder.setRequired(true);
        options.addOption(libFolder);

        Option outputFolder = new Option("o", "out", true, "output (jar) source archives folder");
        outputFolder.setRequired(true);
        options.addOption(outputFolder);

        Option reportFolder = new Option("r", "report", true, "refactoring report output folder");
        reportFolder.setRequired(false);
        options.addOption(reportFolder);

        try {
            CommandLineParser parser = new BasicParser();
            this.cmd = parser.parse(options, args);
            if (getVerbose()) {
            	printArgs(args);
            }
        } catch (ParseException e) {
        	printArgs(args);
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("refactoring framework", options);
            System.exit(0);
        }
	}

	private static void printArgs(String[] args) {
    	System.out.println("*** Command-line arguments");
		for (int i = 0; i < args.length; ++i) {
			System.out.println("arg[" + i + "] = " + args[i]);
		}
		System.out.println("***");
	}
}
