package org.alfine.refactoring.processors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import org.alfine.refactoring.suppliers.Refactoring;
import org.alfine.refactoring.suppliers.RefactoringSupplier;
import org.alfine.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefactoringProcessor {
	
	private final RefactoringSupplier refactoringSupplier;
	private final ResultTracker       resultTracker;
	private final Path                refactoringReportOutputFolder;

	public RefactoringProcessor(RefactoringSupplier supplier, ResultTracker resultTracker, Path refactoringReportOutputFolder) {
		this.refactoringSupplier           = supplier;
		this.resultTracker                 = resultTracker;
		this.refactoringReportOutputFolder = refactoringReportOutputFolder;
	}

	public boolean processSupply(int drop, int limit) {

		// Apply one refactoring at a time until one is successful.
		
		Logger logger = LoggerFactory.getLogger(RefactoringProcessor.class);
		
		boolean               success     = false;
		Supplier<Refactoring> supplier    = null;
		Refactoring           refactoring = null;

		logger.info("Processing supply...\n\t drop = " + drop + ",\n\t limit = " + limit + "");

		supplier = refactoringSupplier.getSupplier();

		int i = drop;

		while ((i > 0) && (refactoring = supplier.get()) != null) {
			--i;
			
			logger.info("dropping refactoring: " + refactoring);
		}

		if (!(limit > 0)) {
			logger.info("Limit (`--limit') is set to zero. No attempts will be made.");
		}

		for (int attempt = 0; (!success && (attempt < limit)); ++attempt) {
			
			logger.info("Refactoring attempt " + attempt);
			
			// Example use: drop n limit 1 to select and try the (n + 1)th refactoring only.

			if ((refactoring = supplier.get()) != null) {

				String cacheLine = refactoring.getRefactoringDescriptor().getCacheLine();

				// Ignore opportunities that have failed in a previous run.

				if (!resultTracker.isKnownToFail(cacheLine)) {
					try (ByteArrayOutputStream out = new ByteArrayOutputStream(512)) {
						success = refactoring.apply(out);
						try {
							out.write("-----------------------------------------------------------".getBytes());
							out.write(System.getProperty("line.separator").getBytes());
							IOUtils.appendToFile(
									this.refactoringReportOutputFolder.resolve("refactoring-output.txt"),
									out.toByteArray());
						} catch (Exception e) {
							e.printStackTrace();
						}
					} catch (IOException e) {
						logger.error("");
					}
					resultTracker.put(cacheLine, success);
					
					if (success) {
						try (OutputStream out = Files.newOutputStream(Paths.get("cache-line.txt"))) {
							out.write(cacheLine.getBytes());
							out.write(System.getProperties().get("line.separator").toString().getBytes());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}				
			} else {

				int nbrAvailable = drop + attempt - i;

				logger.info(
					"Out of refactorings ("
					+ "attempt=" + attempt
					+ ", drop=" + drop
					+ ", limit=" + limit
					+ "; number available was " + nbrAvailable + ".");

				if (drop > 0) {
					logger.info("You could try reducing the value of the 'drop' option.");
				}
				
				break;
			}
		}

		this.resultTracker.update();
		
		if (success) {
			logger.info("One or more refactorings were applied.");
		} else {
			logger.info("No refactorings were applied.");
		}

		return success;
	}
	
//	public static boolean applyRefactoring(Refactoring refactoring) {
//
//		Logger logger = LoggerFactory.getLogger(RefactoringProcessor.class);
//		logger.info("RefactoringProcessor::applyRefactoring()");
//
//		try {
//
//			RefactoringStatus status = refactoring.checkAllConditions(new NullProgressMonitor());
//
//			if (status.hasEntries()) {
//				StringBuilder sb = new StringBuilder();
//				int i = 0;
//				for (RefactoringStatusEntry e : status.getEntries()) {
//					// System.out.printf("RefactoringStatusEntry[%d] = %s\n", i++, e);
//					sb.append("RefactoringStatusEntry");
//					sb.append("[").append(i++).append("]");
//					sb.append("(").append("problem code =" + e.getCode()).append(")");
//					sb.append(System.getProperty("line.separator"));
//					sb.append(e.getMessage());
//				}
//				logger.info(sb.toString());
//			}
//			if (status.hasFatalError() || status.hasError()) {
//				logger.error("Refactoring could not be applied: The status has FatalError or Error.");
//				return false;
//			}
//
//			Change change = null;
//
//			try {
//				change = refactoring.createChange(new NullProgressMonitor());
//
//				if (change == null) {
//					logger.error("Refactoring could not be applied: Change produced by refactoring is null.");
//					return false;
//				}
//				
//				logger.info("ChangeDescriptor: " + change.getDescriptor().toString());
//				
//			} catch (NullPointerException e) {
//				e.printStackTrace();
//				return false; // Fail.
//			}
//
//			try {
//
//				Object o = change.perform(new NullProgressMonitor());
//
//				if (o == null) {
//					return false; // Failed to apply change.
//				}
//
//			} finally {
//				change.dispose();
//			}
//
//		} catch (Exception e) {
//			e.printStackTrace();
//			return false;
//		}
//
//		return true;
//
//		/*
//		
//		
//		
//		
//		boolean result = false;
//	
//		try {
//			
//			RefactoringStatus status = refactoring.checkAllConditions(new NullProgressMonitor());
//
//			if (status.hasError()) {
//				throw new Exception("Refactoring status has errors: " + status);
//			}
//
//			Change change = refactoring.createChange(new NullProgressMonitor());
//
//			try {
//
//				change.initializeValidationData(new NullProgressMonitor());
//
//				if (!change.isEnabled()) {
//					// log("Change is not enabled: " + change);
//					throw new Exception("Change is not enabled: "  + change);
//				}
//
//				RefactoringStatus valid = change.isValid(new NullProgressMonitor());
//
//				if (valid.hasError()) {
//					// log("Change validity has errors: " + valid);
//					throw new Exception("Change validity has errors: " + valid);
//				}
//
//				Object o = change.perform(new NullProgressMonitor());
//
//				if (o == null) {
//					// log("Failed to apply change!");
//					throw new Exception("Failed to apply change.");
//				}
//
//				result = true;
//
//			} finally {
//				change.dispose();
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return result;
//		 */
//	}

}
