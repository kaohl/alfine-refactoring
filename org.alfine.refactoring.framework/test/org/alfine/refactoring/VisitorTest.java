package org.alfine.refactoring;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Stream;

import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.framework.WorkspaceConfiguration;
import org.alfine.refactoring.framework.launch.CommandLineArguments;
import org.alfine.refactoring.suppliers.HotMethodRefactoringSupplier;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.JavaCore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*	
	Create a "JUnit Plug-in Test" launch configuration and open
	the "Main" tab, then uncheck the "clear" option for the
	workspace. The "Main" tab also shows where the workspace is
	created: ${workspace_loc}/../junit-workspace

	There are two strategies for test resources:
	1) Pre-populate the workspace folder with an assets folder, or
	2) generate required files as part of a test.

	Preferably, we can set up multiple assets folders (one per bm)
	and then specify which one to use before each test. This way,
	we can explore the benchmark projects using unit tests while
	building the framework.

 	A test starts with only the assets folder as input and then deploy
 	a set of java projects based on assets files. Java projects generated
 	as part of a test are deleted afterwards so that the next test can
 	start from clean slate.
 	
 	ATTENTION
 	Generate a workspace with the fresh=true flag. Then comment
 	out the beforeEach and beforeAfter methods to not clean, and
 	set the fresh flag to false to not have to recreate the
 	workspace from zip files on every run.
 */

class VisitorTest {

	Logger logger = LoggerFactory.getLogger(VisitorTest.class);
	
	private static Path getLocation() {
		return Paths.get(Platform.getInstanceLocation().getURL().getFile());
	}

	private static Path getMethodsConfigPath(String bm) {
		return getLocation().resolve("assets/" + bm + "/src/methods.config");
	}

	private static WorkspaceConfiguration getDefaultWorkspaceConfiguration(String bm, String[] args) {
		CommandLineArguments arguments = new CommandLineArguments(args);

		// Set default JavaCore compiler compliance. (TODO: Evaluate whether this is needed now that we set the compiler compliance on all projects.)
		Hashtable<String, String> options = JavaCore.getDefaultOptions();
		JavaCore.setComplianceOptions(arguments.getCompilerComplianceVersion(), options);
		JavaCore.setOptions(options);

		Path location = getLocation();
		Path src      = location.resolve("assets/" + bm + "/src");
		Path lib      = location.resolve("assets/" + bm + "/lib");
		return new WorkspaceConfiguration(
			arguments,
			getLocation(),
			src,
			lib
		);
	}

	private static void configureMethods(String bm, List<String> methods) throws IOException {
		Files.deleteIfExists(getMethodsConfigPath(bm));
		Files.write(getMethodsConfigPath(bm), methods, StandardOpenOption.CREATE_NEW);
	}

	/*
	@BeforeEach
	void setup() throws Exception {
		Path loc = getLocation();
		Path src = loc.resolve("assets/src");
		Path lib = loc.resolve("assets/lib");
		String newline = System.getProperty("line.sep");
		try (OutputStream out = Files.newOutputStream(src.resolve("workspace.config"), StandardOpenOption.CREATE)) {
			StringBuilder sb = new StringBuilder();
			sb.append("test {" + newline);
			sb.append("  exp test.jar");
			sb.append("}" + newline);
			sb.append(b);
		} catch (Exception e) {
			
		}
	}
	*/

	@BeforeEach
	void beforeEach() {
		clean();
	}

	@AfterEach
	void afterEach() {
		//clean();
	}

	void clean() {
		Path loc = getLocation();
		try (Stream<Path> paths = Files.list(loc)) {
			paths.forEach(path -> {
				if (!(path.endsWith("assets") || path.endsWith(".metadata"))) {
					logger.debug("Cleanup: {}", path);
					try {
						IWorkspace     workspace     = ResourcesPlugin.getWorkspace();
						IWorkspaceRoot workspaceRoot = workspace.getRoot();
						IProject       project       = workspaceRoot.getProject(path.getFileName().toString());
						project.delete(true, true, null);
					} catch (Exception e) {
						try {
							if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
								FileUtils.deleteDirectory(path.toFile());
							} else {
								Files.delete(path);
							}
						} catch (Exception e2) {
							logger.error(String.format("Exception thrown, trying to delete project: {}", path), e);
							logger.error(String.format("Exception thrown, trying to delete file: {}", path), e2);
						}
					}
				}
			});
		} catch (Exception e) {
			logger.error("Exception during workspace cleanup.", e);
		}
	}

	private Workspace getWorkspace(String bm, String compliance) {
		String[] args = new String[] {
			"--cache"     , "oppcache",
			"--compliance", compliance,
			"--src"       , "assets/" + bm + "/src",
			"--lib"       , "assets/" + bm + "/lib",
			"--out"       , "output",     // Not used here (yet).
			"--report"    , "report"      // Not used here (yet).
		};

		// TODO: This path will always be the top-level folder of the eclipse data folder.
		Path location = getLocation();
		logger.info("Location: {}", location);

		// TODO: These should be derived from WorkspaceConfiguration.
		Path src   = location.resolve("assets/" + bm + "/src");
		Path lib   = location.resolve("assets/" + bm + "/lib");
		Path out   = location.resolve("output");
		Path cache = location.resolve("oppcache");

		Workspace workspace = new Workspace(
			getDefaultWorkspaceConfiguration(bm, args),
			src,
			lib,
			out,
			true,  /* If true, workspace is set up and refactoring opportunities written to file. */
			cache  /* `RefactoringDescriptor` cache in workspace. */
		);
		return workspace;
	}

	@Test
	void testLuSearch() throws Exception {
		configureMethods("lusearch", Arrays.asList(
			"org.apache.lucene.util.compress.LZ4.decompress(DataInput, int, byte[], int)",
			"org.apache.lucene.codecs.lucene90.compressing.Lucene90CompressingStoredFieldsReader$BlockState.doReset(int)",
			"org.apache.lucene.codecs.lucene90.compressing.StoredFieldsInts.readInts8(IndexInput, int, long[], int)",
			"org.apache.lucene.codecs.lucene90.blocktree.SegmentTermsEnum.seekExact(BytesRef)",
			"org.apache.lucene.store.ByteBufferIndexInput.readByte()",
			"org.apache.lucene.queryparser.classic.QueryParserTokenManager.jjMoveNfa_2(int, int)",
			"org.apache.lucene.codecs.lucene90.blocktree.SegmentTermsEnumFrame.scanToTermNonLeaf(BytesRef, boolean)",
			"org.apache.lucene.search.IndexSearcher.search(Weight, CollectorManager, Collector)",
			"org.apache.lucene.codecs.lucene90.blocktree.SegmentTermsEnumFrame.decodeMetaData()",
			"org.apache.lucene.index.SegmentCoreReaders.notifyCoreClosedListeners()",
			"org.apache.lucene.store.ByteBufferGuard.getShort(ByteBuffer)",
			"org.apache.lucene.backward_codecs.lucene90.PForUtil.decodeAndPrefixSum(DataInput, long, long[])",
			"org.apache.lucene.document.DocumentStoredFieldVisitor.needsField(FieldInfo)",
			"org.apache.lucene.queryparser.classic.QueryParserTokenManager.jjCheckNAdd(int)",
			"org.apache.lucene.document.Document.get(String)",
			"org.apache.lucene.document.FieldType.pointDimensionCount()",
			"org.apache.lucene.backward_codecs.lucene90.PForUtil.prefixSum32(long[], long)",
			"org.apache.lucene.search.IndexSearcher.searchAfter(ScoreDoc, Query, int)",
			"org.apache.lucene.util.fst.BitTableUtil.readByte(FST$BytesReader)",
			"org.apache.lucene.store.ByteBufferIndexInput.readShort()",
			"org.apache.lucene.store.DataInput.readVLong(boolean)",
			"org.apache.lucene.queryparser.classic.QueryParser.Clause(String)",
			"org.apache.lucene.backward_codecs.lucene90.ForUtil.<init>()",
			"org.apache.lucene.store.ByteArrayDataInput.getPosition()",
			"org.apache.lucene.analysis.TokenStream.<init>(AttributeSource)",
			"org.apache.lucene.search.HitQueue.lessThan(Object, Object)",
			"org.apache.lucene.backward_codecs.lucene90.PForUtil.expand32(long[])",
			"org.apache.lucene.search.Weight.bulkScorer(LeafReaderContext)",
			"org.apache.lucene.queryparser.classic.QueryParser.Query(String)",
			"org.apache.lucene.analysis.TokenFilter.end()",
			"org.apache.lucene.analysis.standard.StandardTokenizer.setMaxTokenLength(int)",
			"org.apache.lucene.util.QueryBuilder.analyzeTerm(String, TokenStream)",
			"org.apache.lucene.search.TermQuery$TermWeight$2.get(long)",
			"org.apache.lucene.analysis.tokenattributes.PackedTokenAttributeImpl.clone()",
			"org.apache.lucene.search.IndexSearcher.getSlices()",
			"org.apache.lucene.search.TermQuery$TermWeight.<init>(TermQuery, IndexSearcher, ScoreMode, float, TermStates)",
			"org.apache.lucene.index.TermStates.register(TermState, int, int, long)",
			"org.apache.lucene.util.fst.FST.findTargetArc(int, FST$Arc, FST$Arc, FST$BytesReader)",
			"org.apache.lucene.search.TopDocs.mergeAux(Sort, int, int, TopDocs[], Comparator)",
			"org.apache.lucene.search.TopDocs$ScoreMergeSortQueue.<init>(TopDocs[], Comparator)",
			"org.apache.lucene.search.TopScoreDocCollector.newTopDocs(ScoreDoc[], int)",
			"org.apache.lucene.codecs.lucene90.blocktree.FieldReader.getMin()",
			"org.apache.lucene.search.IndexSearcher.termStatistics(Term, int, long)",
			"org.apache.lucene.search.TopScoreDocCollectorManager.newCollector()",
			"org.apache.lucene.search.LeafSimScorer.getNormValue(int)",
			"org.apache.lucene.search.IndexSearcher.collectionStatistics(String)",
			"org.apache.lucene.codecs.lucene90.blocktree.SegmentTermsEnum.<init>(FieldReader)",
			"org.apache.lucene.util.AttributeSource.<init>(AttributeSource)",
			"org.apache.lucene.search.TermQuery.createWeight(IndexSearcher, ScoreMode, float)",
			"org.apache.lucene.util.packed.DirectMonotonicReader.getBounds(long)",
			"org.apache.lucene.util.ArrayUtil.copyOfSubArray(byte[], int, int)",
			"org.apache.lucene.util.VirtualMethod.compareImplementationDistance(Class, VirtualMethod, VirtualMethod)",
			"org.apache.lucene.codecs.lucene90.blocktree.SegmentTermsEnumFrame.scanToTermLeaf(BytesRef, boolean)",
			"org.apache.lucene.search.IndexSearcher.search(List, Weight, Collector)",
			"org.apache.lucene.util.QueryBuilder.createFieldQuery(TokenStream, BooleanClause$Occur, String, boolean, int)",
			"org.apache.lucene.backward_codecs.lucene90.Lucene90PostingsReader$BlockDocsEnum.<init>(Lucene90PostingsReader, FieldInfo)",
			"org.apache.lucene.codecs.lucene90.compressing.Lucene90CompressingStoredFieldsReader$BlockState.document(int)",
			"org.apache.lucene.search.TopDocsCollector.populateResults(ScoreDoc[], int)",
			"org.apache.lucene.document.DocumentStoredFieldVisitor.stringField(FieldInfo, String)",
			"org.apache.lucene.util.BytesRefBuilder.copyBytes(BytesRef)",
			"org.apache.lucene.codecs.lucene90.compressing.Lucene90CompressingStoredFieldsReader.document(int, StoredFieldVisitor)",
			"org.apache.lucene.search.TaskExecutor$TaskGroup.<init>(Collection)",
			"org.apache.lucene.analysis.tokenattributes.CharTermAttributeImpl.copyBuffer(char[], int, int)",
			"org.apache.lucene.util.PriorityQueue.downHeap(int)",
			"org.apache.lucene.document.FieldType.<init>(IndexableFieldType)",
			"org.apache.lucene.store.DataInput.readVInt()",
			"org.apache.lucene.util.compress.LowercaseAsciiCompression.decompress(DataInput, byte[], int)",
			"org.apache.lucene.backward_codecs.lucene90.Lucene90PostingsFormat$IntBlockTermState.<init>()",
			"org.apache.lucene.store.DataInput.readString()",
			"org.apache.lucene.search.TaskExecutor$TaskGroup.invokeAll(Executor)",
			"org.apache.lucene.analysis.standard.StandardTokenizerImpl.zzUnpackcmap_blocks(String, int, int[])",
			"org.apache.lucene.store.ByteBufferGuard.ensureValid()",
			"org.apache.lucene.util.RamUsageEstimator.<clinit>()"
		));
		Workspace workspace = getWorkspace("lusearch", "11");

		HotMethodRefactoringSupplier supplier = new HotMethodRefactoringSupplier(workspace);
		supplier.cacheOpportunities();
	}

	@Test
	public void testLuIndex() throws Exception {
		configureMethods("luindex", Arrays.asList(
			"" // TODO
		));

		Workspace workspace = getWorkspace("luindex", "11");

		HotMethodRefactoringSupplier supplier = new HotMethodRefactoringSupplier(workspace);
		supplier.cacheOpportunities();
	}

	@Test
	void testBatik() throws Exception {
		configureMethods("batik", Arrays.asList(
			"org.apache.batik.ext.awt.image.codec.png.PNGEncodeParam.filterRow(byte[], byte[], byte[][], int, int)",
			"org.apache.batik.ext.awt.image.codec.png.PNGImageEncoder.encodePass(OutputStream, Raster, int, int, int, int)",
			"org.apache.batik.css.engine.CSSEngine.getCSSParentNode(Node)",
			"org.apache.batik.parser.NumberParser.parseFloat()",
			"org.apache.batik.css.engine.CSSEngine.getParentCSSStylableElement(Element)",
			"org.apache.batik.ext.awt.image.GraphicsUtil.copyData_INT_PACK(Raster, WritableRaster)",
			"org.apache.batik.gvt.AbstractGraphicsNode.invalidateGeometryCache()",
			"org.apache.batik.dom.AbstractElement$NamedNodeHashMap.get(String, String)",
			"org.apache.batik.gvt.CompositeGraphicsNode.getTransformedPrimitiveBounds(AffineTransform)",
			"org.apache.batik.gvt.AbstractGraphicsNode.getTransformedBounds(AffineTransform)",
			"org.apache.batik.css.engine.CSSEngine.getCascadedStyleMap(CSSStylableElement, String)",
			"org.apache.batik.dom.util.SAXDocumentFactory.startElement(String, String, String, Attributes)",
			"org.apache.batik.ext.awt.image.codec.png.CRC.updateCRC(int, byte[], int, int)",
			"org.apache.batik.bridge.CSSUtilities.getComputedStyle(Element, int)",
			"org.apache.batik.css.parser.Scanner.nextChar()",
			"org.apache.batik.css.parser.Scanner.nextToken()",
			"org.apache.batik.util.io.StringNormalizingReader.read()",
			"org.apache.batik.dom.AbstractElement.invalidateElementsByTagName(Node)",
			"org.apache.batik.dom.svg.AbstractSVGList.getItemImpl(int)",
			"org.apache.batik.css.engine.CSSEngine.getComputedStyle(CSSStylableElement, String, int)",
			"org.apache.batik.gvt.CompositeGraphicsNode.add(Object)",
			"org.apache.batik.dom.svg.SVGAnimatedPathDataSupport.handlePathSegList(SVGPathSegList, PathHandler)",
			"org.apache.batik.css.engine.CSSEngine.getCSSNextSibling(Node)",
			"org.apache.batik.ext.awt.image.codec.png.PNGImageEncoder.clamp(int, int)",
			"org.apache.batik.dom.AbstractElement.getAttributeNS(String, String)",
			"org.apache.batik.bridge.GVTBuilder.buildComposite(BridgeContext, Element, CompositeGraphicsNode)",
			"org.apache.batik.css.parser.Scanner.endGap()",
			"org.apache.batik.util.DoublyIndexedTable.put(Object, Object, Object)",
			"org.apache.batik.dom.AbstractElement.getAttributeNodeNS(String, String)",
			"org.apache.batik.dom.AbstractElement$NamedNodeHashMap.rehash()",
			"org.apache.batik.ext.awt.RadialGradientPaintContext.fixedPointSimplestCaseNonCyclicFillRaster(int[], int, int, int, int, int, int)",
			"org.apache.batik.css.engine.StyleMap.isComputed(int)",
			"org.apache.batik.bridge.AbstractGraphicsNodeBridge.associateSVGContext(BridgeContext, Element, GraphicsNode)",
			"org.apache.batik.anim.dom.SVGOMElement.getCascadedXMLBase(Node)",
			"org.apache.batik.css.engine.StyleMap.putValue(int, Value)",
			"org.apache.batik.css.engine.CSSEngine.addMatchingRules(List, StyleSheet, Element, String)",
			"org.apache.batik.parser.NumberParser.buildFloat(int, int)",
			"org.apache.batik.util.ParsedURLDefaultProtocolHandler.constructParsedURLData(URL)",
			"org.apache.batik.parser.AbstractParser.<init>()",
			"org.apache.batik.bridge.AbstractGraphicsNodeBridge.computeTransform(SVGTransformable, BridgeContext)",
			"org.apache.batik.bridge.GVTBuilder.buildGraphicsNode(BridgeContext, Element, CompositeGraphicsNode)",
			"org.apache.batik.anim.dom.AbstractSVGAnimatedValue.<init>(AbstractElement, String, String)",
			"org.apache.batik.dom.util.DOMUtilities.getPrefix(String)",
			"org.apache.batik.anim.dom.SVGOMElement.createLiveAnimatedString(String, String)",
			"org.apache.batik.bridge.AbstractGraphicsNodeBridge.disposeTree(Node, boolean)",
			"org.apache.batik.gvt.CompositeGraphicsNode.getPrimitiveBounds()",
			"org.apache.batik.parser.LengthParser.parseLength()",
			"org.apache.batik.css.engine.StyleMap.getValue(int)",
			"org.apache.batik.anim.dom.SVGStylableElement.getOverrideStyleDeclarationProvider()",
			"org.apache.batik.dom.AbstractDocument.importNode(Node, boolean, boolean)",
			"org.apache.batik.css.parser.Parser.parseStyleDeclaration(boolean)",
			"org.apache.batik.bridge.CSSUtilities.getCSSEngine(Element)",
			"org.apache.batik.util.ParsedURLData.<init>(URL)",
			"org.apache.batik.util.ParsedURL.getHandler(String)",
			"org.apache.batik.ext.awt.geom.ExtendedGeneralPath.computeArc(double, double, double, double, double, boolean, boolean, double, double)",
			"org.apache.batik.gvt.CompositeGraphicsNode.ensureCapacity(int)",
			"org.apache.batik.bridge.PaintServer.convertMarkers(Element, ShapeNode, BridgeContext)",
			"org.apache.batik.gvt.CompositeGraphicsNode.invalidateGeometryCache()",
			"org.apache.batik.dom.AbstractElement$NamedNodeHashMap.item(int)",
			"org.apache.batik.gvt.CompositeGraphicsNode.primitivePaint(Graphics2D)",
			"org.apache.batik.ext.awt.MultipleGradientPaintContext.calculateSingleArrayGradient(Color[], Color[], float)",
			"org.apache.batik.ext.awt.MultipleGradientPaintContext.interpolate(int, int, int[])",
			"org.apache.batik.css.engine.sac.CSSElementSelector.match(Element, String)",
			"org.apache.batik.css.engine.value.LengthManager.computeValue(CSSStylableElement, String, CSSEngine, int, StyleMap, Value)",
			"org.apache.batik.css.parser.Scanner.clearBuffer()",
			"org.apache.batik.parser.PathParser.skipCommaSpaces2()",
			"org.apache.batik.css.engine.StringIntMap.get(String)",
			"org.apache.batik.bridge.GVTBuilder.build(BridgeContext, Element)",
			"org.apache.batik.dom.util.DOMUtilities.isValidName(String)",
			"org.apache.batik.bridge.PaintServer.convertFillAndStroke(Element, ShapeNode, BridgeContext)",
			"org.apache.batik.dom.AbstractNode.getPrefix()",
			"org.apache.batik.gvt.AbstractGraphicsNode.fireGraphicsNodeChangeCompleted()",
			"org.apache.batik.css.engine.value.AbstractColorManager.createColorComponent(LexicalUnit)",
			"org.apache.batik.dom.svg.AbstractSVGPathSegList.getItem(int)",
			"org.apache.batik.bridge.SVGUseElementBridge.buildCompositeGraphicsNode(BridgeContext, Element, CompositeGraphicsNode)",
			"org.apache.batik.css.parser.Scanner.numberUnit(boolean)",
			"org.apache.batik.css.engine.value.svg.SVGPaintManager.createValue(LexicalUnit, CSSEngine)",
			"org.apache.batik.parser.PathParser.parsea()",
			"org.apache.batik.util.ParsedURL.sameFile(ParsedURL)",
			"org.apache.batik.parser.TransformListParser.parseTranslate()",
			"org.apache.batik.util.HaltingThread.hasBeenHalted()",
			"org.apache.batik.css.parser.ScannerUtilities.isCSSNameCharacter(char)",
			"org.apache.batik.bridge.URIResolver.getNode(String, Element)",
			"org.apache.batik.gvt.CompositeShapePainter.addShapePainter(ShapePainter)",
			"org.apache.batik.bridge.CSSUtilities.convertShapeRendering(Element, RenderingHints)",
			"org.apache.batik.bridge.AbstractGraphicsNodeBridge.createGraphicsNode(BridgeContext, Element)",
			"org.apache.batik.util.ParsedURLDefaultProtocolHandler.parseURL(ParsedURL, String)",
			"org.apache.batik.anim.dom.SVGOMAnimatedTransformList$BaseSVGTransformList.getValueAsString()",
			"org.apache.batik.css.parser.ScannerUtilities.isCSSHexadecimalCharacter(char)",
			"org.apache.batik.css.parser.CSSLexicalUnit.<init>(short, LexicalUnit)",
			"org.apache.batik.css.engine.value.AbstractColorManager.createValue(LexicalUnit, CSSEngine)",
			"org.apache.batik.anim.dom.SVGOMAnimatedTransformList$BaseSVGTransformList.revalidate()",
			"org.apache.batik.css.engine.value.AbstractColorManager.createRGBColor(Value, Value, Value)",
			"org.apache.batik.css.engine.StyleMap.<init>(int)",
			"org.apache.batik.parser.PathParser.parsec()",
			"org.apache.batik.bridge.SVGShapeElementBridge.createGraphicsNode(BridgeContext, Element)",
			"org.apache.batik.dom.AbstractNode.getXblParentNode()",
			"org.apache.batik.parser.PathParser.doParse()",
			"org.apache.batik.bridge.PaintServer.convertColor(Value, float)",
			"org.apache.batik.ext.awt.geom.ExtendedGeneralPath.makeRoom(int)",
			"org.apache.batik.bridge.GVTBuilder.handleGenericBridges(BridgeContext, Element)",
			"org.apache.batik.dom.AbstractNode.getXblFirstChild()",
			"org.apache.batik.css.engine.CSSEngine.findStyleSheetNodes(Node)",
			"org.apache.batik.util.DoublyIndexedTable.get(Object, Object)",
			"org.apache.batik.anim.dom.SVGOMElement.createLiveAnimatedNumber(String, String, float, boolean)",
			"org.apache.batik.dom.AbstractParentNode.appendChild(Node)",
			"org.apache.batik.anim.dom.SVGOMDocument.createAttributeNS(String, String)",
			"org.apache.batik.anim.dom.SVGOMDocument.createElementNS(String, String)",
			"org.apache.batik.anim.dom.SVGOMElement.createLiveAnimatedBoolean(String, String, boolean)",
			"org.apache.batik.dom.AbstractCharacterData.setNodeValue(String)",
			"org.apache.batik.anim.dom.AbstractElement.<init>(String, AbstractDocument)"
		));

		Workspace workspace = getWorkspace("batik", "1.8");

//		Collection<IPackageFragment> fragments = workspace.getFragments(fragment -> true);
//		for (IPackageFragment fragment : fragments) {
//			System.out.println("FRAGMENT " + fragment.getElementName());
//		}

		HotMethodRefactoringSupplier supplier = new HotMethodRefactoringSupplier(workspace);
		supplier.cacheOpportunities();
//		assertEquals(1, supplier.getHotMethods().size());
		// supplier.cacheOpportunities();
	}

	@Test
	public void testJaCoP() throws Exception {
		configureMethods("jacop", Arrays.asList(
			"org.jacop.constraints.cumulative.CumulativeBasic.sweepPruning(Store)",
			"org.jacop.constraints.netflow.simplex.NetworkSimplex.dualPivot(Arc)",
			"org.jacop.fz.ParserTokenManager.jjMoveNfa_0(int, int)",
			"org.jacop.core.IntVar.max()",
			"org.jacop.core.IntVar.min()",
			"org.jacop.core.Store.addChanged(Var, int, int)",
			"org.jacop.constraints.netflow.simplex.Arc.isInCut(boolean)",
			"org.jacop.util.SimpleHashSet.add(Object)",
			"org.jacop.constraints.netflow.simplex.Node.computePotentials()",
			"org.jacop.fz.Parser.jj_scan_token(int)",
			"org.jacop.util.SimpleHashSet.hash(Object)",
			"org.jacop.constraints.netflow.simplex.Arc.longCost()",
			"org.jacop.constraints.netflow.simplex.Arc.reducedCost()",
			"org.jacop.fz.ParserTokenManager.jjCheckNAdd(int)",
			"org.jacop.util.QueueForward.queueForward(int, Var)",
			"org.jacop.constraints.netflow.simplex.Arc.tail()",
			"org.jacop.constraints.netflow.simplex.NetworkSimplex.cost(long)",
			"org.jacop.core.IntervalDomain.max()",
			"org.jacop.util.SimpleHashSet.removeFirst()",
			"org.jacop.fz.SimpleCharStream.UpdateLineColumn(char)",
			"org.jacop.constraints.netflow.simplex.NetworkSimplex.augmentFlow(Node, Node, int)",
			"org.jacop.constraints.netflow.simplex.Node.markTree(boolean)",
			"org.jacop.fz.SimpleCharStream.readChar()",
			"org.jacop.core.Store.consistency()",
			"org.jacop.constraints.netflow.simplex.Danzig.next()",
			"org.jacop.core.IntervalDomain.min()",
			"org.jacop.constraints.netflow.simplex.NetworkSimplex.updateTree(Arc, Arc)",
			"org.jacop.core.Store.removeLevel(int)",
			"org.jacop.core.IntDomain.putModelConstraint(int, Var, Constraint, int)",
			"org.jacop.constraints.netflow.simplex.NetworkSimplex.removeArc(Arc)",
			"org.jacop.constraints.netflow.simplex.NetworkSimplex.decrementDegree(Node)",
			"org.jacop.constraints.Reified.consistency(Store)",
			"org.jacop.constraints.netflow.simplex.Node.predecessorOnThread()",
			"org.jacop.fz.ParserTokenManager.getNextToken()",
			"org.jacop.fz.Parser.jj_ntk()",
			"org.jacop.core.IntDomain.removeModelConstraint(int, Var, Constraint)",
			"org.jacop.fz.JJTParserState.popNode()",
			"org.jacop.fz.Parser.jj_2_2(int)",
			"org.jacop.constraints.cumulative.CumulativeBasic.updateTasksRes(Store)",
			"org.jacop.fz.Parser.jj_consume_token(int)",
			"org.jacop.constraints.cumulative.Task.exists()",
			"org.jacop.constraints.Sum.consistency(Store)",
			"org.jacop.util.SimpleHashSet$Entry.add(Object)",
			"org.jacop.core.BoundDomain.inValue(int, IntVar, int)",
			"org.jacop.fz.SimpleNode.jjtAddChild(Node, int)",
			"org.jacop.fz.Parser.jj_save(int, int)",
			"org.jacop.constraints.cumulative.Cumulative.filterZeroTasks(TaskView[])",
			"org.jacop.constraints.cumulative.CumulativeUnary.notLast(Store, ThetaTree, TaskView[], TaskView[], TaskView[])",
			"org.jacop.fz.ParserTokenManager.jjMoveStringLiteralDfa0_0()",
			"org.jacop.constraints.cumulative.Task.maxNonZero()",
			"org.jacop.constraints.cumulative.ThetaTree.computeNode(int)",
			"org.jacop.core.Store.recordBooleanChange(BooleanVar)",
			"org.jacop.constraints.netflow.Pruning.analyzeArc(Arc, int)",
			"org.jacop.constraints.netflow.ArcCompanion.compareTo(ArcCompanion)",
			"org.jacop.constraints.Constraint.removeConstraint()",
			"org.jacop.constraints.cumulative.ThetaLambdaUnaryTree.buildTree(TaskView[])",
			"org.jacop.constraints.cumulative.ThetaTree.initTree(TaskView[])",
			"org.jacop.constraints.binpacking.Binpacking.consistency(Store)",
			"org.jacop.constraints.netflow.simplex.NetworkSimplex.networkSimplex(int)",
			"org.jacop.constraints.cumulative.CumulativeUnary.detectable(Store, ThetaTree, TaskView[], TaskView[], TaskView[])",
			"org.jacop.fz.ParserTokenManager.jjCheckNAddStates(int, int)",
			"org.jacop.constraints.cumulative.ThetaLambdaUnaryTree.computeNodeVals(int)",
			"org.jacop.fz.JJTParserState.closeNodeScope(Node, boolean)",
			"org.jacop.constraints.netflow.simplex.Node.lca(Node)",
			"org.jacop.constraints.binpacking.Binpacking.lbBins(int[], int, int)",
			"org.jacop.constraints.netflow.simplex.Node.rightMostLeaf()",
			"org.jacop.core.Store.addChanged(Constraint)",
			"org.jacop.constraints.SumInt.computeInit()",
			"org.jacop.constraints.cumulative.TaskNormalView.lct()",
			"org.jacop.constraints.cumulative.ThetaTree.<init>()",
			"org.jacop.constraints.Reified.queueVariable(int, Var)",
			"org.jacop.constraints.netflow.ArcCompanion.compareTo(Object)",
			"org.jacop.constraints.XorBool.notConsistency(Store)",
			"org.jacop.core.SmallDenseDomain.min()",
			"org.jacop.core.TimeStamp.update(Object)",
			"org.jacop.constraints.netflow.Pruning$PercentStrategy.init()",
			"org.jacop.fz.Token.newToken(int, String)",
			"org.jacop.constraints.cumulative.TaskReversedView.est()",
			"org.jacop.constraints.cumulative.ThetaTree.addLeave(int)",
			"org.jacop.constraints.cumulative.Cumulative.lambda$new$0(TaskView, TaskView)",
			"org.jacop.constraints.cumulative.Tree.notExist(int)",
			"org.jacop.fz.Tables.removeAliasFromSearch()",
			"org.jacop.core.IntervalDomain.singleton()",
			"org.jacop.constraints.netflow.simplex.Arc.addFlow(int)",
			"org.jacop.constraints.netflow.Pruning.analyze(int)",
			"org.jacop.core.SmallDenseDomain.in(int, Var, int, int)",
			"org.jacop.constraints.SumBool.consistency(Store)",
			"org.jacop.core.IntervalDomain.isIntersecting(IntDomain)",
			"org.jacop.fz.ParserTokenManager.jjFillToken()",
			"org.jacop.constraints.cumulative.Tree.parent(int)",
			"org.jacop.fz.SimpleCharStream.BeginToken()",
			"org.jacop.core.IntVar.singleton()",
			"org.jacop.fz.ASTAnnExpr.<init>(int)",
			"org.jacop.fz.JJTParserState.openNodeScope(Node)",
			"org.jacop.util.SimpleHashSet$Entry.<init>(SimpleHashSet, Object)",
			"org.jacop.fz.OutputArrayAnnotation.contains(Var)",
			"org.jacop.fz.Parser.scalar_flat_expr()",
			"org.jacop.constraints.cumulative.TaskNormalView.ect()",
			"org.jacop.constraints.cumulative.CumulativeUnary.edgeFindPhase(Store, TaskView[])",
			"org.jacop.core.IntervalBasedBacktrackableManager.removeLevel(int)",
			"org.jacop.util.SparseSet.addMember(int)",
			"org.jacop.constraints.cumulative.CumulativeBasic.lambda$new$0(CumulativeBasic$Event, CumulativeBasic$Event)",
			"org.jacop.constraints.SumBool.prune(byte)",
			"org.jacop.fz.constraints.Support.aliasConstraints()",
			"org.jacop.fz.Parser.constraint_items()",
			"org.jacop.fz.SimpleCharStream.GetImage()",
			"org.jacop.fz.VariablesParameters.generateVariables(SimpleNode, Tables, Store)",
			"org.jacop.constraints.netflow.simplex.NetworkSimplex.addArcWithFlow(Arc)",
			"org.jacop.constraints.SumInt.pruneLtEq(long)",
			"org.jacop.constraints.cumulative.CumulativeUnary.notFirstNotLastPhase(Store, TaskView[])",
			"org.jacop.fz.constraints.ComparisonConstraints.int_comparison_reif(int, SimpleNode)",
			"org.jacop.core.BooleanVar.dom()",
			"org.jacop.core.IntervalDomain.inMax(int, Var, int)",
			"org.jacop.constraints.netflow.Pruning.analyzeArcHelper(Arc, int)",
			"org.jacop.core.IntVar.singleton(int)",
			"org.jacop.constraints.netflow.simplex.NetworkSimplex.primalStep(Arc)",
			"org.jacop.fz.Parser.annotation()",
			"org.jacop.constraints.cumulative.ThetaTree.ect(int)",
			"org.jacop.constraints.cumulative.CumulativeUnary.detectablePhase(Store, TaskView[])",
			"org.jacop.constraints.cumulative.CumulativeBasic$Event.date()",
			"org.jacop.constraints.cumulative.CumulativeBasic$Event.task()",
			"org.jacop.core.BoundDomain.min()",
			"org.jacop.core.IntervalDomain.contains(int)",
			"org.jacop.search.DepthFirstSearch.label(int)",
			"org.jacop.constraints.netflow.NetworkFlow.consistency(Store)",
			"org.jacop.constraints.netflow.NetworkFlow.updateGraph()",
			"org.jacop.constraints.cumulative.ThetaLambdaUnaryTree.computeLeaveVals(int)",
			"org.jacop.constraints.XeqY.notSatisfied()",
			"org.jacop.constraints.Count.consistency(Store)",
			"org.jacop.fz.SimpleCharStream.getEndColumn()",
			"org.jacop.fz.ParserTokenManager.jjMoveStringLiteralDfa1_0(long)",
			"org.jacop.core.IntervalDomain.<init>(int)",
			"org.jacop.constraints.netflow.Network.backtrack()",
			"org.jacop.core.SmallDenseDomain.previousValue(int)",
			"org.jacop.fz.Parser.flat_expr()",
			"org.jacop.fz.SimpleCharStream.getBeginLine()",
			"org.jacop.constraints.cumulative.Tree.plus(int, int)",
			"org.jacop.constraints.cumulative.CumulativeBasic$Event.<init>(int, TaskView, int, int)",
			"org.jacop.constraints.cumulative.ThetaTree.enableNode(int)",
			"org.jacop.core.IntervalDomain.getSize()",
			"org.jacop.constraints.SumBool.entailed(byte)",
			"org.jacop.fz.JJTParserState.pushNode(Node)",
			"org.jacop.fz.Parser.var_decl_item()",
			"org.jacop.core.BooleanVar.singleton()",
			"org.jacop.core.TimeStamp.value()",
			"org.jacop.core.TimeStamp.ensureCapacity(int)",
			"org.jacop.constraints.netflow.simplex.Arc.getCompanion()",
			"org.jacop.core.IntervalBasedBacktrackableManager.addChanged(int)",
			"org.jacop.constraints.netflow.Pruning$PercentStrategy.next()",
			"org.jacop.core.TimeStamp.removeLevel(int)",
			"org.jacop.util.SimpleHashSet.indexFor(int, int)",
			"org.jacop.util.QueueForward.<init>(Collection, Collection)",
			"org.jacop.fz.Constraints.generateAllConstraints(SimpleNode)",
			"org.jacop.fz.Parser.jj_3R_15()",
			"org.jacop.fz.Constraints.generateAlias(SimpleNode)",
			"org.jacop.fz.Parser.var_decl_items()",
			"org.jacop.fz.Parser.ann_expr()",
			"org.jacop.constraints.ChannelReif.consistency(Store)",
			"org.jacop.core.SmallDenseDomain.adaptMin()",
			"org.jacop.core.SmallDenseDomain.getSize(long)",
			"org.jacop.constraints.netflow.Pruning.pruneNodesWithSmallDegree()",
			"org.jacop.constraints.netflow.Network.modified(ArcCompanion)",
			"org.jacop.search.SimpleSelect.getChoiceVariable(int)",
			"org.jacop.constraints.Constraint.impose(Store)",
			"org.jacop.constraints.netflow.simplex.NetworkSimplex.treeSwap(Node, Node, Node)",
			"org.jacop.constraints.cumulative.TaskReversedView.lct()",
			"org.jacop.constraints.cumulative.CumulativeUnary.lambda$new$2(TaskView, TaskView)",
			"org.jacop.constraints.cumulative.ThetaTree.addNode(int)",
			"org.jacop.constraints.cumulative.CumulativeBasic$Event.type()",
			"org.jacop.core.BoundDomain.inMax(int, Var, int)",
			"org.jacop.fz.constraints.Support.parseAnnotations(SimpleNode)",
			"org.jacop.constraints.AndBoolVector.consistency(Store)",
			"org.jacop.fz.Constraints.generateConstraint(SimpleNode)",
			"org.jacop.fz.VariablesParameters.getType(SimpleNode)",
			"org.jacop.core.IntervalDomain.singleton(int)",
			"org.jacop.core.IntervalDomain.in(int, Var, IntDomain)",
			"org.jacop.core.IntervalDomain.inMin(int, Var, int)",
			"org.jacop.core.IntervalDomain.inComplement(int, Var, int)",
			"org.jacop.constraints.SumInt.pruneGtEq(long)",
			"org.jacop.core.Store.recordChange(Var)",
			"org.jacop.constraints.CountBounds.consistency(Store)",
			"org.jacop.fz.Parser.non_array_flat_exprs()",
			"org.jacop.constraints.cumulative.ThetaNode.assignValues()",
			"org.jacop.constraints.cumulative.Tree.right(int)",
			"org.jacop.constraints.cumulative.ThetaLambdaUnaryTree.addToThetaInit(int)",
			"org.jacop.constraints.cumulative.TaskReversedView.lst()",
			"org.jacop.constraints.cumulative.CumulativeUnary.consistency(Store)",
			"org.jacop.constraints.cumulative.Cumulative.lambda$new$1(TaskView, TaskView)",
			"org.jacop.constraints.XlteqY.satisfied()",
			"org.jacop.constraints.XeqY.satisfied()",
			"org.jacop.fz.Tables.getVariable(String)",
			"org.jacop.fz.ParserTokenManager.jjMoveStringLiteralDfa3_0(long, long)",
			"org.jacop.fz.SimpleCharStream.backup(int)",
			"org.jacop.constraints.XlteqY.consistency(Store)",
			"org.jacop.fz.ParserTokenManager.jjStopAtPos(int, int)",
			"org.jacop.fz.Parser.array_literal()",
			"org.jacop.fz.ParserTokenManager.jjMoveStringLiteralDfa2_0(long, long)",
			"org.jacop.fz.ParserTokenManager.jjStartNfa_0(int, long)",
			"org.jacop.fz.Parser.jj_2_10(int)",
			"org.jacop.fz.ParserTokenManager.jjStopStringLiteralDfa_0(int, long)",
			"org.jacop.fz.SimpleNode.jjtGetChild(int)",
			"org.jacop.fz.Parser.jj_2_9(int)",
			"org.jacop.fz.Parser.non_array_flat_expr()",
			"org.jacop.core.SmallDenseDomain.max()",
			"org.jacop.constraints.Constraint.long2int(long)",
			"org.jacop.constraints.XeqY.consistency(Store)",
			"org.jacop.core.IntervalDomain.inShift(int, Var, IntDomain, int)",
			"org.jacop.core.SmallDenseDomain.inMax(int, Var, int)",
			"org.jacop.constraints.SumInt.propagate(int)",
			"org.jacop.constraints.netflow.Network.increaseLevel()",
			"org.jacop.constraints.cumulative.ThetaLambdaUnaryTree.removeFromLambda(int)",
			"org.jacop.constraints.cumulative.Tree.left(int)",
			"org.jacop.constraints.cumulative.CumulativeUnary.lambda$new$1(TaskView, TaskView)",
			"org.jacop.core.BoundDomain.singleton()",
			"org.jacop.constraints.PrimitiveConstraint.getNestedPruningEvent(Var, boolean)",
			"org.jacop.fz.Parser.annotations()",
			"org.jacop.constraints.Constraint.lambda$impose$5(Var)",
			"org.jacop.constraints.ChannelReif.<init>(IntVar, Map)",
			"org.jacop.core.SmallDenseDomain.isIntersecting(IntDomain)",
			"org.jacop.fz.Parser.jj_2_1(int)",
			"org.jacop.core.BooleanVar.<init>(Store, String, BoundDomain)",
			"org.jacop.core.Store.putVariable(Var)",
			"org.jacop.constraints.netflow.Network.restore(ArcCompanion)",
			"org.jacop.core.SmallDenseDomain.inComplement(int, Var, int)",
			"org.jacop.core.TimeStamp.addLast(Object, int)",
			"org.jacop.constraints.netflow.NetworkFlow.queueVariable(int, Var)",
			"org.jacop.core.BoundDomain.in(int, Var, int, int)",
			"org.jacop.constraints.netflow.simplex.NetworkSimplex.incrementDegree(Node, Arc)",
			"org.jacop.constraints.netflow.Network.cost(long)",
			"org.jacop.core.IntervalDomain.in(int, Var, int, int)",
			"org.jacop.fz.Parser.flat_exprs()",
			"org.jacop.fz.SimpleNode.jjtSetParent(Node)",
			"org.jacop.constraints.cumulative.ThetaTree.updateTree(int)",
			"org.jacop.constraints.cumulative.ThetaLambdaUnaryTree.moveToLambda(int)",
			"org.jacop.constraints.cumulative.TaskNormalView.lst()",
			"org.jacop.constraints.cumulative.ThetaLambdaUnaryTree.<init>()",
			"org.jacop.constraints.cumulative.CumulativeBasic.profileProp(Store)",
			"org.jacop.constraints.cumulative.CumulativeBasic$Event.value()",
			"org.jacop.core.IntVar.removeConstraint(Constraint)",
			"org.jacop.constraints.XplusClteqZ.notSatisfied()",
			"org.jacop.constraints.DecomposedConstraint.checkInputForNullness(String[], Object[][])",
			"org.jacop.fz.constraints.Support.getVarArray(SimpleNode)",
			"org.jacop.fz.SimpleCharStream.getBeginColumn()",
			"org.jacop.fz.Parser.jj_3_1()",
			"org.jacop.constraints.Not.queueVariable(int, Var)",
			"org.jacop.util.SimpleHashSet.transfer(SimpleHashSet$Entry[])",
			"org.jacop.fz.constraints.BoolConstraints.clause_generation(SimpleNode)",
			"org.jacop.fz.constraints.GlobalConstraints.gen_jacop_count(SimpleNode)",
			"org.jacop.fz.Parser.constraint_item()",
			"org.jacop.fz.Parser.jj_2_3(int)",
			"org.jacop.fz.Parser.jj_3_9()",
			"org.jacop.fz.Parser.scalar_ti_expr_tail()",
			"org.jacop.constraints.netflow.Pruning.pruneArc(int, int, boolean, ArcCompanion)",
			"org.jacop.constraints.XmulYeqZ.consistency(Store)",
			"org.jacop.core.IntervalDomain.addDom(IntDomain)",
			"org.jacop.constraints.netflow.Network.remove(Arc)",
			"org.jacop.core.Store.getFirstChanged()",
			"org.jacop.constraints.netflow.ArcCompanion.processEvent(IntVar, MutableNetwork)",
			"org.jacop.constraints.Max.consistency(Store)",
			"org.jacop.fz.constraints.Support$3.consistency(Store)",
			"org.jacop.util.SimpleHashSet.isEmpty()",
			"org.jacop.core.IntervalBasedBacktrackableManager.setLevel(int)",
			"org.jacop.constraints.cumulative.ThetaLambdaUnaryTree.ectLambda()",
			"org.jacop.constraints.Min.consistency(Store)",
			"org.jacop.constraints.cumulative.CumulativeUnary.detectable(Store, TaskView[], TaskView[])",
			"org.jacop.constraints.cumulative.ThetaLambdaUnaryTree.ect()",
			"org.jacop.constraints.XplusClteqZ.satisfied()",
			"org.jacop.constraints.XlteqY.notSatisfied()",
			"org.jacop.constraints.AndBoolVector.swap(int, int)",
			"org.jacop.constraints.DecomposedConstraint.checkInputForNullness(String, Object[])",
			"org.jacop.fz.constraints.BoolConstraints.allVarOne(IntVar[])",
			"org.jacop.constraints.SumBool.filterAndOverflow(IntVar[])",
			"org.jacop.constraints.AndBoolVector.<init>(IntVar[], IntVar)",
			"org.jacop.fz.ASTVarDeclItem.getIdent()",
			"org.jacop.fz.Parser.bool_ti_expr_tail()",
			"org.jacop.fz.SimpleCharStream.getEndLine()",
			"org.jacop.fz.JJTParserState.nodeArity()",
			"org.jacop.fz.VariablesParameters.getAnnotations(SimpleNode, int)",
			"org.jacop.fz.Tables.addSearchVar(Var)",
			"org.jacop.fz.OutputArrayAnnotation.toString()",
			"org.jacop.core.IntDomain.getEventsInclusion(int)",
			"org.jacop.constraints.OrBoolSimple.consistency(Store)",
			"org.jacop.fz.Tables.isOutput(Var)",
			"org.jacop.constraints.XlteqC.<init>(IntVar, int)",
			"org.jacop.constraints.Reified.getConsistencyPruningEvent(Var)",
			"org.jacop.fz.constraints.Support$1.consistency(Store)",
			"org.jacop.constraints.XlteqY.<init>(IntVar, IntVar)",
			"org.jacop.fz.ParserTokenManager.jjMoveStringLiteralDfa8_0(long, long)",
			"org.jacop.fz.SimpleNode.<init>(int)",
			"org.jacop.fz.Parser.constraint_elem()",
			"org.jacop.fz.Parser.non_array_ti_expr_tail()",
			"org.jacop.fz.ASTAnnotation.setId(String)",
			"org.jacop.core.BoundDomain.singleton(int)",
			"org.jacop.core.SmallDenseDomain.inMin(int, Var, int)",
			"org.jacop.core.SmallDenseDomain.removeLevel(int, Var)",
			"org.jacop.constraints.XplusYeqZ.consistency(Store)",
			"org.jacop.core.SmallDenseDomain.noIntervals()",
			"org.jacop.core.IntVar.remove(int)",
			"org.jacop.constraints.netflow.ArcCompanion.changeMaxCapacity(int)",
			"org.jacop.constraints.netflow.ArcCompanion.changeCapacity(int, int)",
			"org.jacop.core.IntDomain.divIntBounds(int, int, int, int)",
			"org.jacop.core.Interval.<init>(int, int)",
			"org.jacop.core.IntervalDomain.unionAdapt(int, int)",
			"org.jacop.util.SparseSet.clear()",
			"org.jacop.core.IntVar.domainHasChanged(int)",
			"org.jacop.core.SmallDenseDomain.inValue(int, IntVar, int)",
			"org.jacop.core.SmallDenseDomain.contains(int)",
			"org.jacop.fz.constraints.Support.getVariable(ASTScalarFlatExpr)",
			"org.jacop.fz.ParserTokenManager.jjMoveStringLiteralDfa7_0(long, long)",
			"org.jacop.constraints.cumulative.Task.dur()",
			"org.jacop.constraints.cumulative.ThetaTree.clearNode(int)",
			"org.jacop.constraints.cumulative.TaskNormalView.updateEdgeFind(int, int)",
			"org.jacop.constraints.cumulative.TaskNormalView.updateNotFirstNotLast(int, int)",
			"org.jacop.constraints.Min.swap(int, int)",
			"org.jacop.constraints.ChannelReif.swap(int, int)",
			"org.jacop.search.InputOrderSelect.getChoiceValue()",
			"org.jacop.search.IndomainMin.indomain(Var)",
			"org.jacop.core.IntervalDomain.inValue(int, IntVar, int)",
			"org.jacop.core.BoundDomain.max()",
			"org.jacop.constraints.ValuePrecede.updateBeta()",
			"org.jacop.constraints.Max.swap(int, int)",
			"org.jacop.constraints.cumulative.TaskReversedView.ect()",
			"org.jacop.constraints.cumulative.Tree.exist(int)",
			"org.jacop.constraints.cumulative.CumulativePrimary.sweepPruning(Store)",
			"org.jacop.constraints.cumulative.CumulativeUnary.edgeFind(Store, TaskView[], TaskView[])",
			"org.jacop.constraints.cumulative.TaskNormalView.est()",
			"org.jacop.constraints.cumulative.CumulativeUnary.<init>(IntVar[], IntVar[], IntVar[], IntVar)",
			"org.jacop.constraints.Constraint.arguments()",
			"org.jacop.core.Store.setLevel(int)",
			"org.jacop.constraints.XplusYlteqZ.consistency(Store)",
			"org.jacop.search.SimpleSelect.getChoiceValue()",
			"org.jacop.search.SplitSelect.getChoiceConstraint(int)",
			"org.jacop.fz.constraints.LinearConstraints.int_lin_relation_reif(int, SimpleNode)",
			"org.jacop.constraints.Sum.<init>(IntVar[], IntVar)",
			"org.jacop.constraints.SumInt.<init>(Store, IntVar[], String, IntVar)",
			"org.jacop.fz.Tables.addVariable(String, IntVar)",
			"org.jacop.fz.Parser.int_literals()",
			"org.jacop.constraints.LinearInt.computeInit()",
			"org.jacop.search.InputOrderSelect.<init>(Store, Var[], Indomain)",
			"org.jacop.constraints.Constraint.lambda$impose$7(Store, Var)",
			"org.jacop.constraints.XorBool.<init>(IntVar[], IntVar)",
			"org.jacop.fz.constraints.BoolConstraints.atLeastOneVarOne(IntVar[])",
			"org.jacop.constraints.OrBoolVector.<init>(IntVar[], IntVar)",
			"org.jacop.fz.constraints.LinearConstraints.allWeightsOne(int[])",
			"org.jacop.constraints.Reified.<init>(PrimitiveConstraint, IntVar)",
			"org.jacop.constraints.Constraint.setScope(Var[])",
			"org.jacop.constraints.PrimitiveConstraint.<init>()",
			"org.jacop.fz.Options.useSat()",
			"org.jacop.fz.Tables.getVariableArray(String)",
			"org.jacop.fz.constraints.LinearConstraints.boolSum(IntVar[])",
			"org.jacop.constraints.DecomposedConstraint.<init>()",
			"org.jacop.core.IntervalDomain.intervalNo(int)",
			"org.jacop.fz.SimpleCharStream.FillBuff()",
			"org.jacop.fz.ParserTokenManager.jjStartNfaWithStates_0(int, int, int)",
			"org.jacop.fz.Parser.ident_anns()",
			"org.jacop.core.IntervalBasedBacktrackableManager.setSize(int)",
			"org.jacop.constraints.XlteqC.notConsistency(Store)",
			"org.jacop.core.IntDomain.isIntersecting(IntDomain)",
			"org.jacop.constraints.OrBoolVector.swap(int, int)",
			"org.jacop.fz.constraints.LinearConstraints.allWeightsMinusOne(int[])",
			"org.jacop.constraints.Reified.impose(Store)",
			"org.jacop.constraints.SumBool.<init>(Store, IntVar[], String, IntVar)",
			"org.jacop.constraints.AndBoolSimple.consistency(Store)",
			"org.jacop.fz.Tables.getAlias(IntVar)",
			"org.jacop.constraints.AndBool.filter(IntVar[])",
			"org.jacop.util.QueueForward.<init>(Constraint, Collection)",
			"org.jacop.constraints.OrBoolVector.notConsistency(Store)",
			"org.jacop.constraints.Not.getConsistencyPruningEvent(Var)",
			"org.jacop.core.Store.impose(Constraint)",
			"org.jacop.core.SmallDenseDomainValueEnumeration.nextElement()",
			"org.jacop.constraints.In.satisfied()",
			"org.jacop.constraints.OrBoolSimple.<init>(IntVar, IntVar, IntVar)",
			"org.jacop.fz.constraints.Support.unique(IntVar[])",
			"org.jacop.constraints.Constraint.getConsistencyPruningEvent(Var)",
			"org.jacop.fz.Parser.jj_3R_45()",
			"org.jacop.fz.Parser.jj_3R_18()",
			"org.jacop.fz.ParserTokenManager.jjMoveStringLiteralDfa4_0(long, long)",
			"org.jacop.fz.Parser.jj_3R_27()",
			"org.jacop.fz.Parser.jj_3R_19()",
			"org.jacop.fz.ParserTokenManager.jjMoveStringLiteralDfa5_0(long, long)",
			"org.jacop.fz.ASTScalarFlatExpr.setType(int)",
			"org.jacop.fz.ASTConstElem.<init>(int)",
			"org.jacop.fz.Token.<init>(int, String)",
			"org.jacop.fz.Parser.array_decl_tail(ASTVarDeclItem)",
			"org.jacop.fz.VariablesParameters.parseAnnExpr(SimpleNode, int)",
			"org.jacop.fz.Parser.jj_3_10()",
			"org.jacop.fz.Parser.jj_3R_16()",
			"org.jacop.core.SmallDenseDomain.<init>(int, int)",
			"org.jacop.fz.Parser.int_ti_expr_tail()",
			"org.jacop.fz.Parser.jj_3_3()",
			"org.jacop.core.SmallDenseDomain.<init>(int, long)",
			"org.jacop.constraints.netflow.ArcCompanion.restore(MutableNetwork)",
			"org.jacop.constraints.binpacking.Binpacking.getNumberBins(BinItem[])",
			"org.jacop.core.IntervalDomainValueEnumeration.<init>(IntervalDomain)",
			"org.jacop.constraints.SumInt.pruneMax(IntVar, long)",
			"org.jacop.core.SmallDenseDomain.toIntervalDomain()",
			"org.jacop.constraints.netflow.simplex.NetworkSimplex.addArc(Arc)",
			"org.jacop.constraints.netflow.ArcCompanion.setFlow(int)",
			"org.jacop.constraints.binpacking.Binpacking.sum(int[])",
			"org.jacop.core.IntervalDomain.removeLevel(int, Var)",
			"org.jacop.constraints.netflow.Pruning.xVarInMin(ArcCompanion, int)",
			"org.jacop.constraints.netflow.Network.needsUpdate(int)",
			"org.jacop.core.IntervalDomain.unionAdapt(Interval)",
			"org.jacop.core.IntDomain.multiplyInt(int, int)",
			"org.jacop.constraints.CountBounds.swap(int, int)",
			"org.jacop.constraints.netflow.Network.add(Arc)",
			"org.jacop.core.SmallDenseDomain.singleton()",
			"org.jacop.search.SimpleSolutionListener.recordSolution()",
			"org.jacop.core.IntVar.dom()",
			"org.jacop.fz.DefaultSearchVars.outputVars()",
			"org.jacop.fz.Tables.getConstant(int)",
			"org.jacop.fz.constraints.GlobalConstraints.gen_jacop_networkflow(SimpleNode)",
			"org.jacop.fz.ParserTokenManager.jjMoveStringLiteralDfa9_0(long, long)",
			"org.jacop.fz.ASTScalarFlatExpr.<init>(int)",
			"org.jacop.fz.Parser.jj_3R_17()"
		));

		Workspace workspace = getWorkspace("jacop", "1.8");

		HotMethodRefactoringSupplier supplier = new HotMethodRefactoringSupplier(workspace);
		supplier.cacheOpportunities();
	}

	@Test
	public void testXalan() throws Exception {
		configureMethods("xalan", Arrays.asList(
			"org.apache.xalan.templates.ElemApplyTemplates.transformSelectedNodes(TransformerImpl)",
			"org.apache.xalan.templates.ElemNumber.formatNumberList(TransformerImpl, long[], int)",
			"org.apache.xalan.processor.XSLTAttributeDef.getPrimativeClass(Object)",
			"org.apache.xalan.templates.ElemLiteralResult.execute(TransformerImpl)",
			"org.apache.xalan.transformer.TransformerImpl.transformToString(ElemTemplateElement)",
			"org.apache.xalan.templates.TemplateSubPatternAssociation.matchMode(QName)",
			"org.apache.xalan.templates.ElemNumber.getMatchingAncestors(XPathContext, int, boolean)",
			"org.apache.xalan.transformer.TransformerImpl.hasTraceListeners()",
			"org.apache.xalan.templates.TemplateList.getTemplateFast(XPathContext, int, int, QName, int, boolean, DTM)",
			"org.apache.xalan.transformer.TransformerImpl.popCurrentTemplateRuleIsNull()",
			"org.apache.xalan.templates.ElemTemplateElement.unexecuteNSDecls(TransformerImpl)",
			"org.apache.xalan.transformer.TransformerImpl.popCurrentMatched()",
			"org.apache.xalan.templates.ElemApplyTemplates.execute(TransformerImpl)",
			"org.apache.xalan.transformer.TransformerImpl.executeChildTemplates(ElemTemplateElement, boolean)",
			"org.apache.xalan.transformer.CountersTable.countNode(XPathContext, ElemNumber, int)",
			"org.apache.xalan.templates.StylesheetRoot$ComposeState.getQNameID(QName)",
			"org.apache.xalan.templates.ElemForEach.endCompose(StylesheetRoot)",
			"org.apache.xalan.templates.ElemTemplateElement.resolvePrefixTables()",
			"org.apache.xalan.templates.ElemTemplateElement.compose(StylesheetRoot)",
			"org.apache.xalan.templates.StylesheetRoot.composeTemplates(ElemTemplateElement)",
			"org.apache.xalan.templates.StylesheetRoot.recompose()",
			"org.apache.xalan.templates.StylesheetComposed.isAggregatedType()",
			"org.apache.xalan.templates.ElemTemplateElement.<init>()",
			"org.apache.xalan.processor.StylesheetHandler.startElement(String, String, String, Attributes)",
			"org.apache.xalan.processor.XSLTAttributeDef.setAttrValue(StylesheetHandler, String, String, String, String, ElemTemplateElement)",
			"org.apache.xalan.processor.StylesheetHandler.pushSpaceHandling(Attributes)",
			"org.apache.xalan.processor.StylesheetHandler.getProcessorFor(String, String, String)",
			"org.apache.xalan.processor.StylesheetHandler.flushCharacters()",
			"org.apache.xalan.processor.XSLTElementDef.getLastOrder()"
		));

		Workspace workspace = getWorkspace("xalan", "1.8");

		HotMethodRefactoringSupplier supplier = new HotMethodRefactoringSupplier(workspace);
		supplier.cacheOpportunities();
	}
}
