package org.alfine.refactoring.framework.launch;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CommandLineArguments {
	
	public enum RefactoringType {
		NONE,
		RENAME,
		INLINE_METHOD,
		EXTRACT_METHOD,
		INLINE_CONSTANT,
		UNKNOWN
	}

	public RefactoringType typeStringToType(String typeString) {		
		switch (typeString) {
		case "none":            return RefactoringType.NONE;
		case "rename":          return RefactoringType.RENAME;
		case "inline-method":   return RefactoringType.INLINE_METHOD;
		case "inline-constant": return RefactoringType.INLINE_CONSTANT;
		case "extract-method":  return RefactoringType.EXTRACT_METHOD;
		default:                return RefactoringType.UNKNOWN;
		}
	}

	private CommandLine cmd;

	public boolean getVerbose() {
		return cmd.hasOption("verbose");
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

	public RefactoringType getRefactoring() {
		return typeStringToType(cmd.hasOption("type") ? cmd.getOptionValue("type") : "none");
	}

	public long getSeed() {
		return cmd.hasOption("seed") ? Long.parseLong(cmd.getOptionValue("seed")) : 0;
	}

	public int getOffset() {
		return cmd.hasOption("offset") ? Integer.parseInt(cmd.getOptionValue("offset")) : 0;
	}

	public int getDrop() {
		return cmd.hasOption("drop") ? Integer.parseInt(cmd.getOptionValue("drop")) : 0;
	}

	public int getLimit() {
		return cmd.hasOption("limit") ? Integer.parseInt(cmd.getOptionValue("limit")) : 0;
	}

	public int getLength() {
		return cmd.hasOption("length") ? Integer.parseInt(cmd.getOptionValue("length")) : 0;
	}

	public boolean getFixed() {
		return hasOption("fixed");
	}

	public int getShuffleSeed() {
		return cmd.hasOption("shuffle") ? Integer.parseInt(cmd.getOptionValue("shuffle")) : 0;
	}

	public boolean hasOption(String opt) {
		return cmd.hasOption(opt);
	}

	public CommandLineArguments(String[] args) {

		for (int i = 0; i < args.length; ++i) {
			System.out.println("arg[" + i + "] = " + args[i]);
		}

		Options options = new Options();

		Option verbose = new Option("v", "verbose", false, "verbose execution");
        verbose.setRequired(false);
        options.addOption(verbose);

        Option srcFolder = new Option("j", "src", true, "source (jar) archives folder");
        srcFolder.setRequired(true);
        options.addOption(srcFolder);

        Option libFolder = new Option("b", "lib", true, "binary (jar) archives folder");
        libFolder.setRequired(true);
        options.addOption(libFolder);

        Option outputFolder = new Option("u", "out", true, "output (jar) source archives folder");
        outputFolder.setRequired(true);
        options.addOption(outputFolder);

        Option refactoring = new Option("t", "type", true, "refactoring type");
        refactoring.setRequired(true);
        options.addOption(refactoring);

        Option seed = new Option("s", "seed", true, "seed for pseudo random number generator");
        seed.setRequired(false);  // Default to zero.
        options.addOption(seed);

        Option offset = new Option("o", "offset", true, "initial offset for pseudo random number generator");
        offset.setRequired(false); // Default to zero.
        options.addOption(offset);

        Option drop = new Option("d", "drop", true, "drop the n first refactorings in supplier stream");
        drop.setRequired(false); // Default to zero.
        options.addOption(drop);

        Option limit = new Option("m", "limit", true, "abort after n failed refactorings (not compatible with 'limit' option)");
        limit.setRequired(false); // Default to zero.
        options.addOption(limit);

        Option length = new Option("l", "length", true, "rename refactoring max symbol length");
        length.setRequired(false); // Default to zero (which makes the program crash).
        options.addOption(length);

        Option fixed = new Option("f", "fixed", false, "always generate symbols of set max length.");
        fixed.setRequired(false); // Default to zero (which makes the program crash).
        options.addOption(fixed);

        Option shuffle = new Option("x", "shuffle", true, "rename refactoring max symbol length");
        shuffle.setRequired(false); // Default to zero (which makes the program crash).
        options.addOption(shuffle);

        try {
            CommandLineParser parser = new BasicParser();
            this.cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("refactoring framework", options);
        }
	}
}
