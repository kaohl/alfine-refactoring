# Overview
This is one of three repositories developed for my [master's thesis](https://lup.lub.lu.se/student-papers/search/publication/9204484) in software engineering, where we explored the impact of Java refactoring on execution performance.

See also the [evaluation framework](https://github.com/kaohl/masters-thesis-software-evaluation) and the [build framework](https://github.com/kaohl/daivy).

> [!NOTE]
> Regarding citation: Please reference the thesis, which provides context for this work, and links to all repositories.

> [!NOTE]
> Please fork the repository for future work.

# Refactoring Framework
This is a Java refactoring framework for automated refactoring that I built as part of my master's in software engineering, where we explored the impact of Java refactoring on execution performance.

The framework is built as an Eclipse rich client platform application. Code assets and configuration is required to perform refactorings on a code base.

The refactoring framework can automatically make code selections for a set of refactoring types, and write these as partial (parameterized) "refactoring descriptors" into a cache on disk.

To apply a refactoring, we first select a refactoring descriptor, then apply arguments, then call the refactoring framework to apply the refactoring on the code base from which the original code selections were made.

Usage is best demonstrated by how the framework was used within the Java performance evaluation framework linked above.
