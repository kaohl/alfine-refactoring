package org.alfine.refactoring.processors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;

import org.alfine.refactoring.suppliers.RefactoringDescriptorFactory;
import org.alfine.utils.IOUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringContext;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

public class Processor {
	private Processor() {} // Static class.

	private static JavaRefactoringDescriptor getDescriptor(String id, Map<String, String> args) {
//		System.out.println("Descriptor arguments:");
//		for (Entry<String, String> entry : args.entrySet()) {
//			System.out.println(String.format("  %s = %s", entry.getKey(), entry.getValue()));
//		}
		RefactoringContribution   contribution = RefactoringCore.getRefactoringContribution(id);
		JavaRefactoringDescriptor defaultInit  = (JavaRefactoringDescriptor)contribution.createDescriptor();
		JavaRefactoringDescriptor descriptor   = (JavaRefactoringDescriptor)contribution.createDescriptor(
			id,
			defaultInit.getProject(),
			defaultInit.getDescription(),
			defaultInit.getComment(),
			args,
			defaultInit.getFlags()
		);
		return descriptor;
	}

	private static void writeStatusToOutputStream(String header, RefactoringStatus status, OutputStream out) {
		try {
			out.write(header.getBytes());
			out.write(System.getProperty("line.separator").getBytes());
			for (RefactoringStatusEntry entry : status.getEntries()) {
				out.write(entry.toString().getBytes());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String errorCodeAsString(int code) {
		switch (code) {
		case RefactoringStatus.OK:      return "OK";
		case RefactoringStatus.INFO:    return "INFO";
		case RefactoringStatus.WARNING: return "WARNING";
		case RefactoringStatus.ERROR:   return "ERROR";
		case RefactoringStatus.FATAL:   return "FATAL";
		default:                        return "UNKNOWN_ERROR_CODE";
		}
	}

	private static org.eclipse.ltk.core.refactoring.Refactoring createRefactoring(String id, Map<String, String> args, OutputStream stdout) {
		JavaRefactoringDescriptor descriptor       = getDescriptor(id, args);
		RefactoringStatus         descriptorStatus = descriptor.validateDescriptor();

		writeStatusToOutputStream("[Descriptor validation status]", descriptorStatus, stdout);

		if (descriptorStatus.hasError()) {
			System.err.println("Invalid refactoring descriptor.");
			return null;
		}

		org.eclipse.ltk.core.refactoring.Refactoring refactoring = null;
		try {
			RefactoringStatus  contextStatus = new RefactoringStatus();
			RefactoringContext ctx           = descriptor.createRefactoringContext(contextStatus);
			writeStatusToOutputStream("[Context validation status]", contextStatus, stdout);
			if (contextStatus.hasError()) {
				System.err.println("Invalid refactoring context.");
				return null;
			}
			refactoring = ctx.getRefactoring();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return refactoring;
	}

	private static boolean createAndPerformChange(String id, Map<String, String> args, OutputStream stdout) {
		org.eclipse.ltk.core.refactoring.Refactoring refactoring = createRefactoring(id, args, stdout);		
		try {
			RefactoringStatus status = refactoring.checkAllConditions(new NullProgressMonitor());

			if (status.hasEntries()) {
				StringBuilder sb = new StringBuilder();
				int i = 0;
				for (RefactoringStatusEntry e : status.getEntries()) {
					sb.append("RefactoringStatusEntry");
					sb.append("[").append(i++).append("]");
					sb.append("(").append(errorCodeAsString(e.getCode())).append(")");
					sb.append(System.getProperty("line.separator"));
					sb.append(e.getMessage());
				}
				sb.append(System.getProperty("line.separator"));
				stdout.write(sb.toString().getBytes());
			}
			if (status.hasError()) {
				System.err.println("Refactoring could not be applied: Invalid refactoring.");
				return false;
			}

			Change change = null;
			try {
				change = refactoring.createChange(new NullProgressMonitor());
				if (change != null) {
					System.out.println("ChangeDescriptor: " + change.getDescriptor().toString());
					Object o = change.perform(new NullProgressMonitor());
					if (o == null) {
						return false; // Failed to apply change.
					}
				} else {
					System.err.println("Refactoring could not be applied: Change produced by refactoring is null.");
					return false;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return false; // Fail.
			} finally {
				if (change != null) {					
					change.dispose();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static boolean refactor(String descriptor, Path outputLocation) {
		SimpleDescriptor desc    = RefactoringDescriptorFactory.getSimple(descriptor);
		boolean          success = false;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream(512)) {
			success = createAndPerformChange(desc.getID(), desc.getArguments(), out);
			out.write("-----------------------------------------------------------".getBytes());
			out.write(System.getProperty("line.separator").getBytes());
			IOUtils.appendToFile(outputLocation.resolve("refactoring-output.txt"), out.toByteArray());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}
}
