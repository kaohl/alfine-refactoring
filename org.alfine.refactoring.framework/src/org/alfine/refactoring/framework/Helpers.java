package org.alfine.refactoring.framework;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

public class Helpers {

	// TODO: Remove? There is an IJavaProject.getPackageFragments() method...
	
	public static class Fragments {
		private IPackageFragmentRoot root;

		public Fragments(IPackageFragmentRoot root) {
			this.root = root;
		}
		
		private Set<IPackageFragment> getPackageFragments(Predicate<IPackageFragment> filter) {
			Set<IPackageFragment> fragments = new TreeSet<>();
			try {
				Arrays.asList(root.getChildren()).stream()
				.filter(c -> c instanceof IPackageFragment)
				.map(IPackageFragment.class::cast)
				.filter(filter)
				.forEach(f -> {
					fragments.add(f);
					getPackageFragmentsRec(f, fragments, filter);
				});
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
			return fragments;
		}
		
		private void getPackageFragmentsRec(IPackageFragment fragment, Set<IPackageFragment> set, Predicate<IPackageFragment> filter) {
			try {

				Arrays.asList(fragment.getChildren()).stream()
				.filter(c -> c instanceof IPackageFragment)
				.map(IPackageFragment.class::cast)
				.filter(filter)
				.forEach(f -> {
					set.add((IPackageFragment) f);
					getPackageFragmentsRec((IPackageFragment)f, set, filter);
				});
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}

		public Set<IPackageFragment> getFilteredFragments(Predicate<IPackageFragment> filter) {
			return getPackageFragments(filter);
		}
	}
}
