package org.alfine.refactoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.alfine.refactoring.suppliers.ExtractConstantFieldDescriptor;
import org.alfine.refactoring.suppliers.ExtractMethodDescriptor;
import org.alfine.refactoring.suppliers.InlineConstantFieldDescriptor;
import org.alfine.refactoring.suppliers.InlineMethodDescriptor;
import org.alfine.refactoring.suppliers.RefactoringDescriptor;
import org.alfine.refactoring.suppliers.RefactoringDescriptorFactory;
import org.alfine.refactoring.suppliers.RenameFieldDescriptor;
import org.alfine.refactoring.suppliers.RenameLocalVariableDescriptor;
import org.alfine.refactoring.suppliers.RenameMethodDescriptor;
import org.alfine.refactoring.suppliers.RenameTypeDescriptor;
import org.junit.jupiter.api.Test;

public class RefactoringDescriptorSerializationTest {

	private void check_equal_after_serialization(RefactoringDescriptor a) {
		RefactoringDescriptor b = RefactoringDescriptorFactory.get(a.toString());
		assertEquals(a, b);
		assertEquals(a.getCacheLine(), b.getCacheLine());
	}

	@Test
	public void test_serialize_default_inline_constant_descriptor() {
		check_equal_after_serialization(new InlineConstantFieldDescriptor());
	}

	@Test
	public void test_serialize_default_extract_constant_descriptor() {
		check_equal_after_serialization(new ExtractConstantFieldDescriptor());
	}

	@Test
	public void test_serialize_default_inline_method_descriptor() {
		check_equal_after_serialization(new InlineMethodDescriptor());
	}

	@Test
	public void test_serialize_default_extract_method_descriptor() {
		check_equal_after_serialization(new ExtractMethodDescriptor());
	}

	@Test
	public void test_serialize_default_rename_field_descriptor() {
		check_equal_after_serialization(new RenameFieldDescriptor());
	}

	@Test
	public void test_serialize_default_rename_method_descriptor() {
		check_equal_after_serialization(new RenameMethodDescriptor());
	}

	@Test
	public void test_serialize_default_rename_local_variable_descriptor() {
		check_equal_after_serialization(new RenameLocalVariableDescriptor());
	}

	@Test
	public void test_serialize_default_rename_type_descriptor() {
		check_equal_after_serialization(new RenameTypeDescriptor());
	}

	@Test
	public void test_serialize_default_renam_type_parameter_descriptor() {
		check_equal_after_serialization(new RenameTypeDescriptor());
	}
}
