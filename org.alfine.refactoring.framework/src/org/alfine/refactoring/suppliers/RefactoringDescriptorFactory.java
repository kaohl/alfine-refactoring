package org.alfine.refactoring.suppliers;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.alfine.refactoring.processors.SimpleDescriptor;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;

public class RefactoringDescriptorFactory {

	private static Map<String, BiFunction<Map<String, String>, Map<String, String>, RefactoringDescriptor>> factories = new HashMap<>();

	static {
		factories.put(IJavaRefactorings.INLINE_CONSTANT , InlineConstantFieldDescriptor::new);
		factories.put(IJavaRefactorings.EXTRACT_CONSTANT, ExtractConstantFieldDescriptor::new);

		factories.put(IJavaRefactorings.INLINE_METHOD   , InlineMethodDescriptor::new);
		factories.put(IJavaRefactorings.EXTRACT_METHOD  , ExtractMethodDescriptor::new);

		factories.put(IJavaRefactorings.RENAME_FIELD         , RenameFieldDescriptor::new);
		factories.put(IJavaRefactorings.RENAME_METHOD        , RenameMethodDescriptor::new);
		factories.put(IJavaRefactorings.RENAME_LOCAL_VARIABLE, RenameLocalVariableDescriptor::new);
		factories.put(IJavaRefactorings.RENAME_TYPE          , RenameTypeDescriptor::new);
		factories.put(IJavaRefactorings.RENAME_TYPE_PARAMETER, RenameTypeParameterDescriptor::new);
	}

	public static RefactoringDescriptor get(String descriptor) {
		JsonReader jsonReader = Json.createReader(new StringReader(descriptor));
		JsonObject object     = jsonReader.readObject();
		JsonObject args       = object.getJsonObject("args");
		JsonObject meta       = object.getJsonObject("meta");

		Map<String, String> argsMap = args.entrySet().stream().collect(
			Collectors.toMap(Entry::getKey, e -> ((JsonString)e.getValue()).getString())
		);
		Map<String, String> metaMap = meta.entrySet().stream().collect(
			Collectors.toMap(Entry::getKey, e -> ((JsonString)e.getValue()).getString())
		);
		return factories.get(metaMap.get(RefactoringDescriptor.ID_NAME)).apply(argsMap, metaMap);
	}

	public static SimpleDescriptor getSimple(String descriptor) {
		JsonReader jsonReader = Json.createReader(new StringReader(descriptor));
		JsonObject object     = jsonReader.readObject();
		JsonObject args       = object.getJsonObject("args");
		JsonObject meta       = object.getJsonObject("meta");

		Map<String, String> argsMap = args.entrySet().stream().collect(
			Collectors.toMap(Entry::getKey, e -> ((JsonString)e.getValue()).getString())
		);
		Map<String, String> metaMap = meta.entrySet().stream().collect(
			Collectors.toMap(Entry::getKey, e -> ((JsonString)e.getValue()).getString())
		);
		return new SimpleDescriptor(metaMap.get(RefactoringDescriptor.ID_NAME), argsMap);
	}
}
