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

	Preferrably, we can set up multiple assets folders (one per bm)
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

	private static Path getMethodsConfigPath() {
		return getLocation().resolve("assets/src/methods.config");
	}

	private static WorkspaceConfiguration getDefaultWorkspaceConfiguration() {
		Path location = getLocation();
		Path src      = location.resolve("assets/src");
		Path lib      = location.resolve("assets/lib");
		return new WorkspaceConfiguration(
			getLocation(),
			src,
			lib,
			// TODO: Generate configuration per test case?
			src.resolve("workspace.config"),
			src.resolve("variable.config"),
			src.resolve("packages.config"),
			src.resolve("units.config"),
			src.resolve("methods.config")
		);
	}

	private static void configureMethods(List<String> methods) throws IOException {
		Files.deleteIfExists(getMethodsConfigPath());
		Files.write(getMethodsConfigPath(), methods, StandardOpenOption.CREATE_NEW);
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
		//clean();
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

	private Workspace getWorkspace() {
		Hashtable<String, String> options = JavaCore.getDefaultOptions();
		JavaCore.setComplianceOptions("1.8", options);
		JavaCore.setOptions(options);

		// ATTENTION: Projects won't compile without 'rt.jar' on the classpath.
		//
		// TODO: Is there a better way to do this?
		//       How do I get eclipse to pick up the SDK used to launch the workbench by default?
		//
		// Maybe have a look at classpath containers here:
		//
		//     https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.jdt.doc.isv%2Fguide%2Fjdt_int.htm

		Workspace.RT = "/home/pddp/.sdkman/candidates/java/8.0.432-tem/jre/lib/rt.jar";

		Path location = getLocation();
		logger.info("Location: {}", location);

		Path src   = location.resolve("assets/src");
		Path lib   = location.resolve("assets/lib");
		Path out   = location.resolve("output");
		Path cache = location.resolve("oppcache");

		Workspace workspace = new Workspace(
			getDefaultWorkspaceConfiguration(),
			src,
			lib,
			out,
			false,  /* If true, workspace is set up and refactoring opportunities written to file. */
			cache  /* `RefactoringDescriptor` cache in workspace. */
		);
		return workspace;
	}

	@Test
	void test() throws IOException {

		configureMethods(Arrays.asList(
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

		Workspace workspace = getWorkspace();

//		Collection<IPackageFragment> fragments = workspace.getFragments(fragment -> true);
//		for (IPackageFragment fragment : fragments) {
//			System.out.println("FRAGMENT " + fragment.getElementName());
//		}

		HotMethodRefactoringSupplier supplier = new HotMethodRefactoringSupplier(workspace);
		supplier.cacheOpportunities();
//		assertEquals(1, supplier.getHotMethods().size());
		// supplier.cacheOpportunities();
	}

}
