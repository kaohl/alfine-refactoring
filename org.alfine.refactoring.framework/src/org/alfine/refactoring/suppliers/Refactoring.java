package org.alfine.refactoring.suppliers;

import java.io.IOException;
import java.io.OutputStream;

import org.alfine.refactoring.processors.RefactoringProcessor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.Change;
// import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Refactoring {
	private RefactoringDescriptor descriptor;

	public Refactoring(RefactoringDescriptor descriptor) {
		this.descriptor = descriptor;
	}
	
	public RefactoringDescriptor getRefactoringDescriptor() {
		return this.descriptor;
	}
	
	private org.eclipse.ltk.core.refactoring.Refactoring createRefactoring(OutputStream stdout) {
		
		JavaRefactoringDescriptor descriptor = getRefactoringDescriptor().getDescriptor();

		RefactoringStatus status = descriptor.validateDescriptor();
		
		writeStatusToOutputStream("[Descriptor validation status]", status, stdout);

		if (status.hasError()) {
			System.err.println("Invalid refactoring descriptor.");
			return null;
		}

		org.eclipse.ltk.core.refactoring.Refactoring refactoring = null;

		try {
			RefactoringContext ctx    = null;

			status = new RefactoringStatus();

			ctx = descriptor.createRefactoringContext(status);
			
			writeStatusToOutputStream("[Context validation status]", status, stdout);

			if (status.hasError()) {
				System.out.println("Invalid refactoring context.");
				return null;
			}

			refactoring = ctx.getRefactoring();

		} catch (CoreException e) {
			e.printStackTrace();
		}

		return refactoring;
	}

	private void writeStatusToOutputStream(String header, RefactoringStatus status, OutputStream out) {
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

	private String errorCodeAsString(int code) {
		switch (code) {
		case RefactoringStatus.OK:      return "OK";
		case RefactoringStatus.INFO:    return "INFO";
		case RefactoringStatus.WARNING: return "WARNING";
		case RefactoringStatus.ERROR:   return "ERROR";
		case RefactoringStatus.FATAL:   return "FATAL";
		default:                        return "UNKNOWN_ERROR_CODE";
		}
	}

	public boolean apply(OutputStream stdout) {
		
		Logger logger = LoggerFactory.getLogger(RefactoringProcessor.class);
		logger.info("RefactoringProcessor::applyRefactoring()");

		org.eclipse.ltk.core.refactoring.Refactoring refactoring = null;
		refactoring = createRefactoring(stdout);
		
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
				logger.error("Refactoring could not be applied: Invalid refactoring.");
				return false;
			}

			Change change = null;

			try {
				change = refactoring.createChange(new NullProgressMonitor());

				if (change == null) {
					logger.error("Refactoring could not be applied: Change produced by refactoring is null.");
					return false;
				}
				
				logger.info("ChangeDescriptor: " + change.getDescriptor().toString());
				
			} catch (NullPointerException e) {
				e.printStackTrace();
				return false; // Fail.
			}

			try {

				Object o = change.perform(new NullProgressMonitor());

				if (o == null) {
					return false; // Failed to apply change.
				}

			} finally {
				change.dispose();
			}

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;

		/*
		
		
		
		
		boolean result = false;
	
		try {
			
			RefactoringStatus status = refactoring.checkAllConditions(new NullProgressMonitor());

			if (status.hasError()) {
				throw new Exception("Refactoring status has errors: " + status);
			}

			Change change = refactoring.createChange(new NullProgressMonitor());

			try {

				change.initializeValidationData(new NullProgressMonitor());

				if (!change.isEnabled()) {
					// log("Change is not enabled: " + change);
					throw new Exception("Change is not enabled: "  + change);
				}

				RefactoringStatus valid = change.isValid(new NullProgressMonitor());

				if (valid.hasError()) {
					// log("Change validity has errors: " + valid);
					throw new Exception("Change validity has errors: " + valid);
				}

				Object o = change.perform(new NullProgressMonitor());

				if (o == null) {
					// log("Failed to apply change!");
					throw new Exception("Failed to apply change.");
				}

				result = true;

			} finally {
				change.dispose();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
		 */
	}
}
