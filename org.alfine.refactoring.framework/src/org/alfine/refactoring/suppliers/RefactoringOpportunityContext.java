package org.alfine.refactoring.suppliers;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.alfine.utils.DigestUtil;

public abstract class RefactoringOpportunityContext {
	public RefactoringOpportunityContext() {}
	public abstract Path getContextPath();

	public static Path getMethodContext(String qNameWithSignature) {
		// NOTE: We assume here there are no methods in the context of the specified method.
		//       If we are going to handle multiple methods we need to send in
		//       all parts as an array of strings to avoid detailed parsing.
		int      index = qNameWithSignature.indexOf("(");
		if (index == -1) {
			return null;
		}
		String   q     = qNameWithSignature.substring(0, index);
		String   s     = qNameWithSignature.substring(index);
		String[] qs    = q.split("\\.");
		qs[qs.length - 1] = qs[qs.length - 1] + "-" + DigestUtil.md5(s);
		return Paths.get(String.join("/", qs));
	}
}
