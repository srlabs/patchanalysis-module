package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

public class EncodedField {

    DexFile dexFile;
    int fieldIndex;
    int accessFlags;

    public EncodedField(DexFile dexFile, int fieldIndex, int accessFlags) {
        this.dexFile = dexFile;
        this.fieldIndex = fieldIndex;
        this.accessFlags = accessFlags;
    }

    public FieldIDItem getFieldIDItem() {
        return this.dexFile.getFieldIDItem(this.fieldIndex);
    }

    public String getFieldName() {
        return this.dexFile.getString(this.getFieldIDItem().nameIndex);
    }

    public String getSig() {
        FieldIDItem myFieldIDItem = this.getFieldIDItem();
        TypeDescriptor a = this.dexFile.getType(myFieldIDItem.typeIndex);
        String b = this.dexFile.getString(myFieldIDItem.nameIndex);

        return a + " " + b;
    }

    public int getAccessFlags() {
        return this.accessFlags;
    }

    public String toString() {
        FieldIDItem myFieldIDItem = this.getFieldIDItem();
        String s1 = new AccessFlagsField(this.accessFlags).toString();
        String s2 = this.dexFile.getType(myFieldIDItem.typeIndex).toString();
        String s3 = this.dexFile.getString(myFieldIDItem.nameIndex);
        return s1 + " " + s2 + " " + s3;
    }
}
