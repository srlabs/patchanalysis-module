package de.srlabs.patchanalysis_module.analysis.java_basic_tests.bytecode.v2;

import java.util.*;

public class OpcodeList {

    public static final List<Map<String, Object>> opcodeItems = createStaticList();
    public static final Map<String, Class> opcodeClasses = createStaticMap();

    Map<Integer, Opcode> opcodes;
    Set<String> opcodeNamesDone;
    Set<String> instructionFormats;

    public OpcodeList() {
        opcodes = new HashMap<>();
        opcodeNamesDone = new HashSet<>();
        instructionFormats = new HashSet<>();
        for (Map<String, Object> opcodeItem : opcodeItems) {
            Opcode opcode = Opcode.create(opcodeItem);
            if (this.opcodes.containsKey(opcode.opcode)) {
                throw new RuntimeException("Opcode should not be part of opcodes:" + opcode.opcode);
            }
            this.opcodes.put(opcode.opcode, opcode);
            opcodeNamesDone.add(opcode.name);
        }

        // Make sure we don't have invalid opcodes in opcodeClasses
        for (String opcodeName : OpcodeList.opcodeClasses.keySet()) {
            if (!opcodeNamesDone.contains(opcodeName)) {
                throw new RuntimeException(String.format("Failed to find %s in opcodeClasses", opcodeName));
            }
        }
    }

    public Opcode get(int opcodeIdx) {
        return this.opcodes.get(opcodeIdx);
    }

    private static List<Map<String, Object>> createStaticList() {
        List<Map<String, Object>> result = new ArrayList<>();
        HashMap<String, Object> entry;

        // 0
        entry = new HashMap<>();
        entry.put("format", "k10x");
        entry.put("length", 1);
        entry.put("opcodeName", "NOP");
        entry.put("opcodeNum", 0);
        result.add(Collections.unmodifiableMap(entry));
        // 1
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "MOVE");
        entry.put("opcodeNum", 1);
        result.add(Collections.unmodifiableMap(entry));
        // 2
        entry = new HashMap<>();
        entry.put("format", "k22x");
        entry.put("length", 2);
        entry.put("opcodeName", "MOVE_FROM16");
        entry.put("opcodeNum", 2);
        result.add(Collections.unmodifiableMap(entry));
        // 3
        entry = new HashMap<>();
        entry.put("format", "k32x");
        entry.put("length", 3);
        entry.put("opcodeName", "MOVE_16");
        entry.put("opcodeNum", 3);
        result.add(Collections.unmodifiableMap(entry));
        // 4
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "MOVE_WIDE");
        entry.put("opcodeNum", 4);
        result.add(Collections.unmodifiableMap(entry));
        // 5
        entry = new HashMap<>();
        entry.put("format", "k22x");
        entry.put("length", 2);
        entry.put("opcodeName", "MOVE_WIDE_FROM16");
        entry.put("opcodeNum", 5);
        result.add(Collections.unmodifiableMap(entry));
        // 6
        entry = new HashMap<>();
        entry.put("format", "k32x");
        entry.put("length", 3);
        entry.put("opcodeName", "MOVE_WIDE_16");
        entry.put("opcodeNum", 6);
        result.add(Collections.unmodifiableMap(entry));
        // 7
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "MOVE_OBJECT");
        entry.put("opcodeNum", 7);
        result.add(Collections.unmodifiableMap(entry));
        // 8
        entry = new HashMap<>();
        entry.put("format", "k22x");
        entry.put("length", 2);
        entry.put("opcodeName", "MOVE_OBJECT_FROM16");
        entry.put("opcodeNum", 8);
        result.add(Collections.unmodifiableMap(entry));
        // 9
        entry = new HashMap<>();
        entry.put("format", "k32x");
        entry.put("length", 3);
        entry.put("opcodeName", "MOVE_OBJECT_16");
        entry.put("opcodeNum", 9);
        result.add(Collections.unmodifiableMap(entry));
        // 10
        entry = new HashMap<>();
        entry.put("format", "k11x");
        entry.put("length", 1);
        entry.put("opcodeName", "MOVE_RESULT");
        entry.put("opcodeNum", 10);
        result.add(Collections.unmodifiableMap(entry));
        // 11
        entry = new HashMap<>();
        entry.put("format", "k11x");
        entry.put("length", 1);
        entry.put("opcodeName", "MOVE_RESULT_WIDE");
        entry.put("opcodeNum", 11);
        result.add(Collections.unmodifiableMap(entry));
        // 12
        entry = new HashMap<>();
        entry.put("format", "k11x");
        entry.put("length", 1);
        entry.put("opcodeName", "MOVE_RESULT_OBJECT");
        entry.put("opcodeNum", 12);
        result.add(Collections.unmodifiableMap(entry));
        // 13
        entry = new HashMap<>();
        entry.put("format", "k11x");
        entry.put("length", 1);
        entry.put("opcodeName", "MOVE_EXCEPTION");
        entry.put("opcodeNum", 13);
        result.add(Collections.unmodifiableMap(entry));
        // 14
        entry = new HashMap<>();
        entry.put("format", "k10x");
        entry.put("length", 1);
        entry.put("opcodeName", "RETURN_VOID");
        entry.put("opcodeNum", 14);
        result.add(Collections.unmodifiableMap(entry));
        // 15
        entry = new HashMap<>();
        entry.put("format", "k11x");
        entry.put("length", 1);
        entry.put("opcodeName", "RETURN");
        entry.put("opcodeNum", 15);
        result.add(Collections.unmodifiableMap(entry));
        // 16
        entry = new HashMap<>();
        entry.put("format", "k11x");
        entry.put("length", 1);
        entry.put("opcodeName", "RETURN_WIDE");
        entry.put("opcodeNum", 16);
        result.add(Collections.unmodifiableMap(entry));
        // 17
        entry = new HashMap<>();
        entry.put("format", "k11x");
        entry.put("length", 1);
        entry.put("opcodeName", "RETURN_OBJECT");
        entry.put("opcodeNum", 17);
        result.add(Collections.unmodifiableMap(entry));
        // 18
        entry = new HashMap<>();
        entry.put("format", "k11n");
        entry.put("length", 1);
        entry.put("opcodeName", "CONST_4");
        entry.put("opcodeNum", 18);
        result.add(Collections.unmodifiableMap(entry));
        // 19
        entry = new HashMap<>();
        entry.put("format", "k21s");
        entry.put("length", 2);
        entry.put("opcodeName", "CONST_16");
        entry.put("opcodeNum", 19);
        result.add(Collections.unmodifiableMap(entry));
        // 20
        entry = new HashMap<>();
        entry.put("format", "k31i");
        entry.put("length", 3);
        entry.put("opcodeName", "CONST");
        entry.put("opcodeNum", 20);
        result.add(Collections.unmodifiableMap(entry));
        // 21
        entry = new HashMap<>();
        entry.put("format", "k21h");
        entry.put("length", 2);
        entry.put("opcodeName", "CONST_HIGH16");
        entry.put("opcodeNum", 21);
        result.add(Collections.unmodifiableMap(entry));
        // 22
        entry = new HashMap<>();
        entry.put("format", "k21s");
        entry.put("length", 2);
        entry.put("opcodeName", "CONST_WIDE_16");
        entry.put("opcodeNum", 22);
        result.add(Collections.unmodifiableMap(entry));
        // 23
        entry = new HashMap<>();
        entry.put("format", "k31i");
        entry.put("length", 3);
        entry.put("opcodeName", "CONST_WIDE_32");
        entry.put("opcodeNum", 23);
        result.add(Collections.unmodifiableMap(entry));
        // 24
        entry = new HashMap<>();
        entry.put("format", "k51l");
        entry.put("length", 5);
        entry.put("opcodeName", "CONST_WIDE");
        entry.put("opcodeNum", 24);
        result.add(Collections.unmodifiableMap(entry));
        // 25
        entry = new HashMap<>();
        entry.put("format", "k21h");
        entry.put("length", 2);
        entry.put("opcodeName", "CONST_WIDE_HIGH16");
        entry.put("opcodeNum", 25);
        result.add(Collections.unmodifiableMap(entry));
        // 26
        entry = new HashMap<>();
        entry.put("format", "k21c");
        entry.put("length", 2);
        entry.put("opcodeName", "CONST_STRING");
        entry.put("opcodeNum", 26);
        result.add(Collections.unmodifiableMap(entry));
        // 27
        entry = new HashMap<>();
        entry.put("format", "k31c");
        entry.put("length", 3);
        entry.put("opcodeName", "CONST_STRING_JUMBO");
        entry.put("opcodeNum", 27);
        result.add(Collections.unmodifiableMap(entry));
        // 28
        entry = new HashMap<>();
        entry.put("format", "k21c");
        entry.put("length", 2);
        entry.put("opcodeName", "CONST_CLASS");
        entry.put("opcodeNum", 28);
        result.add(Collections.unmodifiableMap(entry));
        // 29
        entry = new HashMap<>();
        entry.put("format", "k11x");
        entry.put("length", 1);
        entry.put("opcodeName", "MONITOR_ENTER");
        entry.put("opcodeNum", 29);
        result.add(Collections.unmodifiableMap(entry));
        // 30
        entry = new HashMap<>();
        entry.put("format", "k11x");
        entry.put("length", 1);
        entry.put("opcodeName", "MONITOR_EXIT");
        entry.put("opcodeNum", 30);
        result.add(Collections.unmodifiableMap(entry));
        // 31
        entry = new HashMap<>();
        entry.put("format", "k21c");
        entry.put("length", 2);
        entry.put("opcodeName", "CHECK_CAST");
        entry.put("opcodeNum", 31);
        result.add(Collections.unmodifiableMap(entry));
        // 32
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "INSTANCE_OF");
        entry.put("opcodeNum", 32);
        result.add(Collections.unmodifiableMap(entry));
        // 33
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "ARRAY_LENGTH");
        entry.put("opcodeNum", 33);
        result.add(Collections.unmodifiableMap(entry));
        // 34
        entry = new HashMap<>();
        entry.put("format", "k21c");
        entry.put("length", 2);
        entry.put("opcodeName", "NEW_INSTANCE");
        entry.put("opcodeNum", 34);
        result.add(Collections.unmodifiableMap(entry));
        // 35
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "NEW_ARRAY");
        entry.put("opcodeNum", 35);
        result.add(Collections.unmodifiableMap(entry));
        // 36
        entry = new HashMap<>();
        entry.put("format", "k35c");
        entry.put("length", 3);
        entry.put("opcodeName", "FILLED_NEW_ARRAY");
        entry.put("opcodeNum", 36);
        result.add(Collections.unmodifiableMap(entry));
        // 37
        entry = new HashMap<>();
        entry.put("format", "k3rc");
        entry.put("length", 3);
        entry.put("opcodeName", "FILLED_NEW_ARRAY_RANGE");
        entry.put("opcodeNum", 37);
        result.add(Collections.unmodifiableMap(entry));
        // 38
        entry = new HashMap<>();
        entry.put("format", "k31t");
        entry.put("length", 3);
        entry.put("opcodeName", "FILL_ARRAY_DATA");
        entry.put("opcodeNum", 38);
        result.add(Collections.unmodifiableMap(entry));
        // 39
        entry = new HashMap<>();
        entry.put("format", "k11x");
        entry.put("length", 1);
        entry.put("opcodeName", "THROW");
        entry.put("opcodeNum", 39);
        result.add(Collections.unmodifiableMap(entry));
        // 40
        entry = new HashMap<>();
        entry.put("format", "k10t");
        entry.put("length", 1);
        entry.put("opcodeName", "GOTO");
        entry.put("opcodeNum", 40);
        result.add(Collections.unmodifiableMap(entry));
        // 41
        entry = new HashMap<>();
        entry.put("format", "k20t");
        entry.put("length", 2);
        entry.put("opcodeName", "GOTO_16");
        entry.put("opcodeNum", 41);
        result.add(Collections.unmodifiableMap(entry));
        // 42
        entry = new HashMap<>();
        entry.put("format", "k30t");
        entry.put("length", 3);
        entry.put("opcodeName", "GOTO_32");
        entry.put("opcodeNum", 42);
        result.add(Collections.unmodifiableMap(entry));
        // 43
        entry = new HashMap<>();
        entry.put("format", "k31t");
        entry.put("length", 3);
        entry.put("opcodeName", "PACKED_SWITCH");
        entry.put("opcodeNum", 43);
        result.add(Collections.unmodifiableMap(entry));
        // 44
        entry = new HashMap<>();
        entry.put("format", "k31t");
        entry.put("length", 3);
        entry.put("opcodeName", "SPARSE_SWITCH");
        entry.put("opcodeNum", 44);
        result.add(Collections.unmodifiableMap(entry));
        // 45
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "CMPL_FLOAT");
        entry.put("opcodeNum", 45);
        result.add(Collections.unmodifiableMap(entry));
        // 46
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "CMPG_FLOAT");
        entry.put("opcodeNum", 46);
        result.add(Collections.unmodifiableMap(entry));
        // 47
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "CMPL_DOUBLE");
        entry.put("opcodeNum", 47);
        result.add(Collections.unmodifiableMap(entry));
        // 48
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "CMPG_DOUBLE");
        entry.put("opcodeNum", 48);
        result.add(Collections.unmodifiableMap(entry));
        // 49
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "CMP_LONG");
        entry.put("opcodeNum", 49);
        result.add(Collections.unmodifiableMap(entry));
        // 50
        entry = new HashMap<>();
        entry.put("format", "k22t");
        entry.put("length", 2);
        entry.put("opcodeName", "IF_EQ");
        entry.put("opcodeNum", 50);
        result.add(Collections.unmodifiableMap(entry));
        // 51
        entry = new HashMap<>();
        entry.put("format", "k22t");
        entry.put("length", 2);
        entry.put("opcodeName", "IF_NE");
        entry.put("opcodeNum", 51);
        result.add(Collections.unmodifiableMap(entry));
        // 52
        entry = new HashMap<>();
        entry.put("format", "k22t");
        entry.put("length", 2);
        entry.put("opcodeName", "IF_LT");
        entry.put("opcodeNum", 52);
        result.add(Collections.unmodifiableMap(entry));
        // 53
        entry = new HashMap<>();
        entry.put("format", "k22t");
        entry.put("length", 2);
        entry.put("opcodeName", "IF_GE");
        entry.put("opcodeNum", 53);
        result.add(Collections.unmodifiableMap(entry));
        // 54
        entry = new HashMap<>();
        entry.put("format", "k22t");
        entry.put("length", 2);
        entry.put("opcodeName", "IF_GT");
        entry.put("opcodeNum", 54);
        result.add(Collections.unmodifiableMap(entry));
        // 55
        entry = new HashMap<>();
        entry.put("format", "k22t");
        entry.put("length", 2);
        entry.put("opcodeName", "IF_LE");
        entry.put("opcodeNum", 55);
        result.add(Collections.unmodifiableMap(entry));
        // 56
        entry = new HashMap<>();
        entry.put("format", "k21t");
        entry.put("length", 2);
        entry.put("opcodeName", "IF_EQZ");
        entry.put("opcodeNum", 56);
        result.add(Collections.unmodifiableMap(entry));
        // 57
        entry = new HashMap<>();
        entry.put("format", "k21t");
        entry.put("length", 2);
        entry.put("opcodeName", "IF_NEZ");
        entry.put("opcodeNum", 57);
        result.add(Collections.unmodifiableMap(entry));
        // 58
        entry = new HashMap<>();
        entry.put("format", "k21t");
        entry.put("length", 2);
        entry.put("opcodeName", "IF_LTZ");
        entry.put("opcodeNum", 58);
        result.add(Collections.unmodifiableMap(entry));
        // 59
        entry = new HashMap<>();
        entry.put("format", "k21t");
        entry.put("length", 2);
        entry.put("opcodeName", "IF_GEZ");
        entry.put("opcodeNum", 59);
        result.add(Collections.unmodifiableMap(entry));
        // 60
        entry = new HashMap<>();
        entry.put("format", "k21t");
        entry.put("length", 2);
        entry.put("opcodeName", "IF_GTZ");
        entry.put("opcodeNum", 60);
        result.add(Collections.unmodifiableMap(entry));
        // 61
        entry = new HashMap<>();
        entry.put("format", "k21t");
        entry.put("length", 2);
        entry.put("opcodeName", "IF_LEZ");
        entry.put("opcodeNum", 61);
        result.add(Collections.unmodifiableMap(entry));
        // 62
        entry = new HashMap<>();
        entry.put("format", "k10x");
        entry.put("length", 1);
        entry.put("opcodeName", "UNUSED_3E");
        entry.put("opcodeNum", 62);
        result.add(Collections.unmodifiableMap(entry));
        // 63
        entry = new HashMap<>();
        entry.put("format", "k10x");
        entry.put("length", 1);
        entry.put("opcodeName", "UNUSED_3F");
        entry.put("opcodeNum", 63);
        result.add(Collections.unmodifiableMap(entry));
        // 64
        entry = new HashMap<>();
        entry.put("format", "k10x");
        entry.put("length", 1);
        entry.put("opcodeName", "UNUSED_40");
        entry.put("opcodeNum", 64);
        result.add(Collections.unmodifiableMap(entry));
        // 65
        entry = new HashMap<>();
        entry.put("format", "k10x");
        entry.put("length", 1);
        entry.put("opcodeName", "UNUSED_41");
        entry.put("opcodeNum", 65);
        result.add(Collections.unmodifiableMap(entry));
        // 66
        entry = new HashMap<>();
        entry.put("format", "k10x");
        entry.put("length", 1);
        entry.put("opcodeName", "UNUSED_42");
        entry.put("opcodeNum", 66);
        result.add(Collections.unmodifiableMap(entry));
        // 67
        entry = new HashMap<>();
        entry.put("format", "k10x");
        entry.put("length", 1);
        entry.put("opcodeName", "UNUSED_43");
        entry.put("opcodeNum", 67);
        result.add(Collections.unmodifiableMap(entry));
        // 68
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "AGET");
        entry.put("opcodeNum", 68);
        result.add(Collections.unmodifiableMap(entry));
        // 69
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "AGET_WIDE");
        entry.put("opcodeNum", 69);
        result.add(Collections.unmodifiableMap(entry));
        // 70
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "AGET_OBJECT");
        entry.put("opcodeNum", 70);
        result.add(Collections.unmodifiableMap(entry));
        // 71
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "AGET_BOOLEAN");
        entry.put("opcodeNum", 71);
        result.add(Collections.unmodifiableMap(entry));
        // 72
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "AGET_BYTE");
        entry.put("opcodeNum", 72);
        result.add(Collections.unmodifiableMap(entry));
        // 73
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "AGET_CHAR");
        entry.put("opcodeNum", 73);
        result.add(Collections.unmodifiableMap(entry));
        // 74
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "AGET_SHORT");
        entry.put("opcodeNum", 74);
        result.add(Collections.unmodifiableMap(entry));
        // 75
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "APUT");
        entry.put("opcodeNum", 75);
        result.add(Collections.unmodifiableMap(entry));
        // 76
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "APUT_WIDE");
        entry.put("opcodeNum", 76);
        result.add(Collections.unmodifiableMap(entry));
        // 77
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "APUT_OBJECT");
        entry.put("opcodeNum", 77);
        result.add(Collections.unmodifiableMap(entry));
        // 78
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "APUT_BOOLEAN");
        entry.put("opcodeNum", 78);
        result.add(Collections.unmodifiableMap(entry));
        // 79
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "APUT_BYTE");
        entry.put("opcodeNum", 79);
        result.add(Collections.unmodifiableMap(entry));
        // 80
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "APUT_CHAR");
        entry.put("opcodeNum", 80);
        result.add(Collections.unmodifiableMap(entry));
        // 81
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "APUT_SHORT");
        entry.put("opcodeNum", 81);
        result.add(Collections.unmodifiableMap(entry));
        // 82
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IGET");
        entry.put("opcodeNum", 82);
        result.add(Collections.unmodifiableMap(entry));
        // 83
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IGET_WIDE");
        entry.put("opcodeNum", 83);
        result.add(Collections.unmodifiableMap(entry));
        // 84
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IGET_OBJECT");
        entry.put("opcodeNum", 84);
        result.add(Collections.unmodifiableMap(entry));
        // 85
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IGET_BOOLEAN");
        entry.put("opcodeNum", 85);
        result.add(Collections.unmodifiableMap(entry));
        // 86
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IGET_BYTE");
        entry.put("opcodeNum", 86);
        result.add(Collections.unmodifiableMap(entry));
        // 87
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IGET_CHAR");
        entry.put("opcodeNum", 87);
        result.add(Collections.unmodifiableMap(entry));
        // 88
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IGET_SHORT");
        entry.put("opcodeNum", 88);
        result.add(Collections.unmodifiableMap(entry));
        // 89
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IPUT");
        entry.put("opcodeNum", 89);
        result.add(Collections.unmodifiableMap(entry));
        // 90
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IPUT_WIDE");
        entry.put("opcodeNum", 90);
        result.add(Collections.unmodifiableMap(entry));
        // 91
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IPUT_OBJECT");
        entry.put("opcodeNum", 91);
        result.add(Collections.unmodifiableMap(entry));
        // 92
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IPUT_BOOLEAN");
        entry.put("opcodeNum", 92);
        result.add(Collections.unmodifiableMap(entry));
        // 93
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IPUT_BYTE");
        entry.put("opcodeNum", 93);
        result.add(Collections.unmodifiableMap(entry));
        // 94
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IPUT_CHAR");
        entry.put("opcodeNum", 94);
        result.add(Collections.unmodifiableMap(entry));
        // 95
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IPUT_SHORT");
        entry.put("opcodeNum", 95);
        result.add(Collections.unmodifiableMap(entry));
        // 96
        entry = new HashMap<>();
        entry.put("format", "k21c");
        entry.put("length", 2);
        entry.put("opcodeName", "SGET");
        entry.put("opcodeNum", 96);
        result.add(Collections.unmodifiableMap(entry));
        // 97
        entry = new HashMap<>();
        entry.put("format", "k21c");
        entry.put("length", 2);
        entry.put("opcodeName", "SGET_WIDE");
        entry.put("opcodeNum", 97);
        result.add(Collections.unmodifiableMap(entry));
        // 98
        entry = new HashMap<>();
        entry.put("format", "k21c");
        entry.put("length", 2);
        entry.put("opcodeName", "SGET_OBJECT");
        entry.put("opcodeNum", 98);
        result.add(Collections.unmodifiableMap(entry));
        // 99
        entry = new HashMap<>();
        entry.put("format", "k21c");
        entry.put("length", 2);
        entry.put("opcodeName", "SGET_BOOLEAN");
        entry.put("opcodeNum", 99);
        result.add(Collections.unmodifiableMap(entry));
        // 100
        entry = new HashMap<>();
        entry.put("format", "k21c");
        entry.put("length", 2);
        entry.put("opcodeName", "SGET_BYTE");
        entry.put("opcodeNum", 100);
        result.add(Collections.unmodifiableMap(entry));
        // 101
        entry = new HashMap<>();
        entry.put("format", "k21c");
        entry.put("length", 2);
        entry.put("opcodeName", "SGET_CHAR");
        entry.put("opcodeNum", 101);
        result.add(Collections.unmodifiableMap(entry));
        // 102
        entry = new HashMap<>();
        entry.put("format", "k21c");
        entry.put("length", 2);
        entry.put("opcodeName", "SGET_SHORT");
        entry.put("opcodeNum", 102);
        result.add(Collections.unmodifiableMap(entry));
        // 103
        entry = new HashMap<>();
        entry.put("format", "k21c");
        entry.put("length", 2);
        entry.put("opcodeName", "SPUT");
        entry.put("opcodeNum", 103);
        result.add(Collections.unmodifiableMap(entry));
        // 104
        entry = new HashMap<>();
        entry.put("format", "k21c");
        entry.put("length", 2);
        entry.put("opcodeName", "SPUT_WIDE");
        entry.put("opcodeNum", 104);
        result.add(Collections.unmodifiableMap(entry));
        // 105
        entry = new HashMap<>();
        entry.put("format", "k21c");
        entry.put("length", 2);
        entry.put("opcodeName", "SPUT_OBJECT");
        entry.put("opcodeNum", 105);
        result.add(Collections.unmodifiableMap(entry));
        // 106
        entry = new HashMap<>();
        entry.put("format", "k21c");
        entry.put("length", 2);
        entry.put("opcodeName", "SPUT_BOOLEAN");
        entry.put("opcodeNum", 106);
        result.add(Collections.unmodifiableMap(entry));
        // 107
        entry = new HashMap<>();
        entry.put("format", "k21c");
        entry.put("length", 2);
        entry.put("opcodeName", "SPUT_BYTE");
        entry.put("opcodeNum", 107);
        result.add(Collections.unmodifiableMap(entry));
        // 108
        entry = new HashMap<>();
        entry.put("format", "k21c");
        entry.put("length", 2);
        entry.put("opcodeName", "SPUT_CHAR");
        entry.put("opcodeNum", 108);
        result.add(Collections.unmodifiableMap(entry));
        // 109
        entry = new HashMap<>();
        entry.put("format", "k21c");
        entry.put("length", 2);
        entry.put("opcodeName", "SPUT_SHORT");
        entry.put("opcodeNum", 109);
        result.add(Collections.unmodifiableMap(entry));
        // 110
        entry = new HashMap<>();
        entry.put("format", "k35c");
        entry.put("length", 3);
        entry.put("opcodeName", "INVOKE_VIRTUAL");
        entry.put("opcodeNum", 110);
        result.add(Collections.unmodifiableMap(entry));
        // 111
        entry = new HashMap<>();
        entry.put("format", "k35c");
        entry.put("length", 3);
        entry.put("opcodeName", "INVOKE_SUPER");
        entry.put("opcodeNum", 111);
        result.add(Collections.unmodifiableMap(entry));
        // 112
        entry = new HashMap<>();
        entry.put("format", "k35c");
        entry.put("length", 3);
        entry.put("opcodeName", "INVOKE_DIRECT");
        entry.put("opcodeNum", 112);
        result.add(Collections.unmodifiableMap(entry));
        // 113
        entry = new HashMap<>();
        entry.put("format", "k35c");
        entry.put("length", 3);
        entry.put("opcodeName", "INVOKE_STATIC");
        entry.put("opcodeNum", 113);
        result.add(Collections.unmodifiableMap(entry));
        // 114
        entry = new HashMap<>();
        entry.put("format", "k35c");
        entry.put("length", 3);
        entry.put("opcodeName", "INVOKE_INTERFACE");
        entry.put("opcodeNum", 114);
        result.add(Collections.unmodifiableMap(entry));
        // 115
        entry = new HashMap<>();
        entry.put("format", "k10x");
        entry.put("length", 1);
        entry.put("opcodeName", "RETURN_VOID_NO_BARRIER");
        entry.put("opcodeNum", 115);
        result.add(Collections.unmodifiableMap(entry));
        // 116
        entry = new HashMap<>();
        entry.put("format", "k3rc");
        entry.put("length", 3);
        entry.put("opcodeName", "INVOKE_VIRTUAL_RANGE");
        entry.put("opcodeNum", 116);
        result.add(Collections.unmodifiableMap(entry));
        // 117
        entry = new HashMap<>();
        entry.put("format", "k3rc");
        entry.put("length", 3);
        entry.put("opcodeName", "INVOKE_SUPER_RANGE");
        entry.put("opcodeNum", 117);
        result.add(Collections.unmodifiableMap(entry));
        // 118
        entry = new HashMap<>();
        entry.put("format", "k3rc");
        entry.put("length", 3);
        entry.put("opcodeName", "INVOKE_DIRECT_RANGE");
        entry.put("opcodeNum", 118);
        result.add(Collections.unmodifiableMap(entry));
        // 119
        entry = new HashMap<>();
        entry.put("format", "k3rc");
        entry.put("length", 3);
        entry.put("opcodeName", "INVOKE_STATIC_RANGE");
        entry.put("opcodeNum", 119);
        result.add(Collections.unmodifiableMap(entry));
        // 120
        entry = new HashMap<>();
        entry.put("format", "k3rc");
        entry.put("length", 3);
        entry.put("opcodeName", "INVOKE_INTERFACE_RANGE");
        entry.put("opcodeNum", 120);
        result.add(Collections.unmodifiableMap(entry));
        // 121
        entry = new HashMap<>();
        entry.put("format", "k10x");
        entry.put("length", 1);
        entry.put("opcodeName", "UNUSED_79");
        entry.put("opcodeNum", 121);
        result.add(Collections.unmodifiableMap(entry));
        // 122
        entry = new HashMap<>();
        entry.put("format", "k10x");
        entry.put("length", 1);
        entry.put("opcodeName", "UNUSED_7A");
        entry.put("opcodeNum", 122);
        result.add(Collections.unmodifiableMap(entry));
        // 123
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "NEG_INT");
        entry.put("opcodeNum", 123);
        result.add(Collections.unmodifiableMap(entry));
        // 124
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "NOT_INT");
        entry.put("opcodeNum", 124);
        result.add(Collections.unmodifiableMap(entry));
        // 125
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "NEG_LONG");
        entry.put("opcodeNum", 125);
        result.add(Collections.unmodifiableMap(entry));
        // 126
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "NOT_LONG");
        entry.put("opcodeNum", 126);
        result.add(Collections.unmodifiableMap(entry));
        // 127
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "NEG_FLOAT");
        entry.put("opcodeNum", 127);
        result.add(Collections.unmodifiableMap(entry));
        // 128
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "NEG_DOUBLE");
        entry.put("opcodeNum", 128);
        result.add(Collections.unmodifiableMap(entry));
        // 129
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "INT_TO_LONG");
        entry.put("opcodeNum", 129);
        result.add(Collections.unmodifiableMap(entry));
        // 130
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "INT_TO_FLOAT");
        entry.put("opcodeNum", 130);
        result.add(Collections.unmodifiableMap(entry));
        // 131
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "INT_TO_DOUBLE");
        entry.put("opcodeNum", 131);
        result.add(Collections.unmodifiableMap(entry));
        // 132
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "LONG_TO_INT");
        entry.put("opcodeNum", 132);
        result.add(Collections.unmodifiableMap(entry));
        // 133
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "LONG_TO_FLOAT");
        entry.put("opcodeNum", 133);
        result.add(Collections.unmodifiableMap(entry));
        // 134
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "LONG_TO_DOUBLE");
        entry.put("opcodeNum", 134);
        result.add(Collections.unmodifiableMap(entry));
        // 135
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "FLOAT_TO_INT");
        entry.put("opcodeNum", 135);
        result.add(Collections.unmodifiableMap(entry));
        // 136
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "FLOAT_TO_LONG");
        entry.put("opcodeNum", 136);
        result.add(Collections.unmodifiableMap(entry));
        // 137
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "FLOAT_TO_DOUBLE");
        entry.put("opcodeNum", 137);
        result.add(Collections.unmodifiableMap(entry));
        // 138
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "DOUBLE_TO_INT");
        entry.put("opcodeNum", 138);
        result.add(Collections.unmodifiableMap(entry));
        // 139
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "DOUBLE_TO_LONG");
        entry.put("opcodeNum", 139);
        result.add(Collections.unmodifiableMap(entry));
        // 140
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "DOUBLE_TO_FLOAT");
        entry.put("opcodeNum", 140);
        result.add(Collections.unmodifiableMap(entry));
        // 141
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "INT_TO_BYTE");
        entry.put("opcodeNum", 141);
        result.add(Collections.unmodifiableMap(entry));
        // 142
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "INT_TO_CHAR");
        entry.put("opcodeNum", 142);
        result.add(Collections.unmodifiableMap(entry));
        // 143
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "INT_TO_SHORT");
        entry.put("opcodeNum", 143);
        result.add(Collections.unmodifiableMap(entry));
        // 144
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "ADD_INT");
        entry.put("opcodeNum", 144);
        result.add(Collections.unmodifiableMap(entry));
        // 145
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "SUB_INT");
        entry.put("opcodeNum", 145);
        result.add(Collections.unmodifiableMap(entry));
        // 146
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "MUL_INT");
        entry.put("opcodeNum", 146);
        result.add(Collections.unmodifiableMap(entry));
        // 147
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "DIV_INT");
        entry.put("opcodeNum", 147);
        result.add(Collections.unmodifiableMap(entry));
        // 148
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "REM_INT");
        entry.put("opcodeNum", 148);
        result.add(Collections.unmodifiableMap(entry));
        // 149
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "AND_INT");
        entry.put("opcodeNum", 149);
        result.add(Collections.unmodifiableMap(entry));
        // 150
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "OR_INT");
        entry.put("opcodeNum", 150);
        result.add(Collections.unmodifiableMap(entry));
        // 151
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "XOR_INT");
        entry.put("opcodeNum", 151);
        result.add(Collections.unmodifiableMap(entry));
        // 152
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "SHL_INT");
        entry.put("opcodeNum", 152);
        result.add(Collections.unmodifiableMap(entry));
        // 153
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "SHR_INT");
        entry.put("opcodeNum", 153);
        result.add(Collections.unmodifiableMap(entry));
        // 154
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "USHR_INT");
        entry.put("opcodeNum", 154);
        result.add(Collections.unmodifiableMap(entry));
        // 155
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "ADD_LONG");
        entry.put("opcodeNum", 155);
        result.add(Collections.unmodifiableMap(entry));
        // 156
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "SUB_LONG");
        entry.put("opcodeNum", 156);
        result.add(Collections.unmodifiableMap(entry));
        // 157
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "MUL_LONG");
        entry.put("opcodeNum", 157);
        result.add(Collections.unmodifiableMap(entry));
        // 158
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "DIV_LONG");
        entry.put("opcodeNum", 158);
        result.add(Collections.unmodifiableMap(entry));
        // 159
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "REM_LONG");
        entry.put("opcodeNum", 159);
        result.add(Collections.unmodifiableMap(entry));
        // 160
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "AND_LONG");
        entry.put("opcodeNum", 160);
        result.add(Collections.unmodifiableMap(entry));
        // 161
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "OR_LONG");
        entry.put("opcodeNum", 161);
        result.add(Collections.unmodifiableMap(entry));
        // 162
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "XOR_LONG");
        entry.put("opcodeNum", 162);
        result.add(Collections.unmodifiableMap(entry));
        // 163
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "SHL_LONG");
        entry.put("opcodeNum", 163);
        result.add(Collections.unmodifiableMap(entry));
        // 164
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "SHR_LONG");
        entry.put("opcodeNum", 164);
        result.add(Collections.unmodifiableMap(entry));
        // 165
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "USHR_LONG");
        entry.put("opcodeNum", 165);
        result.add(Collections.unmodifiableMap(entry));
        // 166
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "ADD_FLOAT");
        entry.put("opcodeNum", 166);
        result.add(Collections.unmodifiableMap(entry));
        // 167
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "SUB_FLOAT");
        entry.put("opcodeNum", 167);
        result.add(Collections.unmodifiableMap(entry));
        // 168
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "MUL_FLOAT");
        entry.put("opcodeNum", 168);
        result.add(Collections.unmodifiableMap(entry));
        // 169
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "DIV_FLOAT");
        entry.put("opcodeNum", 169);
        result.add(Collections.unmodifiableMap(entry));
        // 170
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "REM_FLOAT");
        entry.put("opcodeNum", 170);
        result.add(Collections.unmodifiableMap(entry));
        // 171
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "ADD_DOUBLE");
        entry.put("opcodeNum", 171);
        result.add(Collections.unmodifiableMap(entry));
        // 172
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "SUB_DOUBLE");
        entry.put("opcodeNum", 172);
        result.add(Collections.unmodifiableMap(entry));
        // 173
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "MUL_DOUBLE");
        entry.put("opcodeNum", 173);
        result.add(Collections.unmodifiableMap(entry));
        // 174
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "DIV_DOUBLE");
        entry.put("opcodeNum", 174);
        result.add(Collections.unmodifiableMap(entry));
        // 175
        entry = new HashMap<>();
        entry.put("format", "k23x");
        entry.put("length", 2);
        entry.put("opcodeName", "REM_DOUBLE");
        entry.put("opcodeNum", 175);
        result.add(Collections.unmodifiableMap(entry));
        // 176
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "ADD_INT_2ADDR");
        entry.put("opcodeNum", 176);
        result.add(Collections.unmodifiableMap(entry));
        // 177
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "SUB_INT_2ADDR");
        entry.put("opcodeNum", 177);
        result.add(Collections.unmodifiableMap(entry));
        // 178
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "MUL_INT_2ADDR");
        entry.put("opcodeNum", 178);
        result.add(Collections.unmodifiableMap(entry));
        // 179
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "DIV_INT_2ADDR");
        entry.put("opcodeNum", 179);
        result.add(Collections.unmodifiableMap(entry));
        // 180
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "REM_INT_2ADDR");
        entry.put("opcodeNum", 180);
        result.add(Collections.unmodifiableMap(entry));
        // 181
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "AND_INT_2ADDR");
        entry.put("opcodeNum", 181);
        result.add(Collections.unmodifiableMap(entry));
        // 182
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "OR_INT_2ADDR");
        entry.put("opcodeNum", 182);
        result.add(Collections.unmodifiableMap(entry));
        // 183
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "XOR_INT_2ADDR");
        entry.put("opcodeNum", 183);
        result.add(Collections.unmodifiableMap(entry));
        // 184
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "SHL_INT_2ADDR");
        entry.put("opcodeNum", 184);
        result.add(Collections.unmodifiableMap(entry));
        // 185
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "SHR_INT_2ADDR");
        entry.put("opcodeNum", 185);
        result.add(Collections.unmodifiableMap(entry));
        // 186
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "USHR_INT_2ADDR");
        entry.put("opcodeNum", 186);
        result.add(Collections.unmodifiableMap(entry));
        // 187
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "ADD_LONG_2ADDR");
        entry.put("opcodeNum", 187);
        result.add(Collections.unmodifiableMap(entry));
        // 188
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "SUB_LONG_2ADDR");
        entry.put("opcodeNum", 188);
        result.add(Collections.unmodifiableMap(entry));
        // 189
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "MUL_LONG_2ADDR");
        entry.put("opcodeNum", 189);
        result.add(Collections.unmodifiableMap(entry));
        // 190
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "DIV_LONG_2ADDR");
        entry.put("opcodeNum", 190);
        result.add(Collections.unmodifiableMap(entry));
        // 191
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "REM_LONG_2ADDR");
        entry.put("opcodeNum", 191);
        result.add(Collections.unmodifiableMap(entry));
        // 192
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "AND_LONG_2ADDR");
        entry.put("opcodeNum", 192);
        result.add(Collections.unmodifiableMap(entry));
        // 193
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "OR_LONG_2ADDR");
        entry.put("opcodeNum", 193);
        result.add(Collections.unmodifiableMap(entry));
        // 194
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "XOR_LONG_2ADDR");
        entry.put("opcodeNum", 194);
        result.add(Collections.unmodifiableMap(entry));
        // 195
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "SHL_LONG_2ADDR");
        entry.put("opcodeNum", 195);
        result.add(Collections.unmodifiableMap(entry));
        // 196
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "SHR_LONG_2ADDR");
        entry.put("opcodeNum", 196);
        result.add(Collections.unmodifiableMap(entry));
        // 197
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "USHR_LONG_2ADDR");
        entry.put("opcodeNum", 197);
        result.add(Collections.unmodifiableMap(entry));
        // 198
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "ADD_FLOAT_2ADDR");
        entry.put("opcodeNum", 198);
        result.add(Collections.unmodifiableMap(entry));
        // 199
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "SUB_FLOAT_2ADDR");
        entry.put("opcodeNum", 199);
        result.add(Collections.unmodifiableMap(entry));
        // 200
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "MUL_FLOAT_2ADDR");
        entry.put("opcodeNum", 200);
        result.add(Collections.unmodifiableMap(entry));
        // 201
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "DIV_FLOAT_2ADDR");
        entry.put("opcodeNum", 201);
        result.add(Collections.unmodifiableMap(entry));
        // 202
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "REM_FLOAT_2ADDR");
        entry.put("opcodeNum", 202);
        result.add(Collections.unmodifiableMap(entry));
        // 203
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "ADD_DOUBLE_2ADDR");
        entry.put("opcodeNum", 203);
        result.add(Collections.unmodifiableMap(entry));
        // 204
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "SUB_DOUBLE_2ADDR");
        entry.put("opcodeNum", 204);
        result.add(Collections.unmodifiableMap(entry));
        // 205
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "MUL_DOUBLE_2ADDR");
        entry.put("opcodeNum", 205);
        result.add(Collections.unmodifiableMap(entry));
        // 206
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "DIV_DOUBLE_2ADDR");
        entry.put("opcodeNum", 206);
        result.add(Collections.unmodifiableMap(entry));
        // 207
        entry = new HashMap<>();
        entry.put("format", "k12x");
        entry.put("length", 1);
        entry.put("opcodeName", "REM_DOUBLE_2ADDR");
        entry.put("opcodeNum", 207);
        result.add(Collections.unmodifiableMap(entry));
        // 208
        entry = new HashMap<>();
        entry.put("format", "k22s");
        entry.put("length", 2);
        entry.put("opcodeName", "ADD_INT_LIT16");
        entry.put("opcodeNum", 208);
        result.add(Collections.unmodifiableMap(entry));
        // 209
        entry = new HashMap<>();
        entry.put("format", "k22s");
        entry.put("length", 2);
        entry.put("opcodeName", "RSUB_INT");
        entry.put("opcodeNum", 209);
        result.add(Collections.unmodifiableMap(entry));
        // 210
        entry = new HashMap<>();
        entry.put("format", "k22s");
        entry.put("length", 2);
        entry.put("opcodeName", "MUL_INT_LIT16");
        entry.put("opcodeNum", 210);
        result.add(Collections.unmodifiableMap(entry));
        // 211
        entry = new HashMap<>();
        entry.put("format", "k22s");
        entry.put("length", 2);
        entry.put("opcodeName", "DIV_INT_LIT16");
        entry.put("opcodeNum", 211);
        result.add(Collections.unmodifiableMap(entry));
        // 212
        entry = new HashMap<>();
        entry.put("format", "k22s");
        entry.put("length", 2);
        entry.put("opcodeName", "REM_INT_LIT16");
        entry.put("opcodeNum", 212);
        result.add(Collections.unmodifiableMap(entry));
        // 213
        entry = new HashMap<>();
        entry.put("format", "k22s");
        entry.put("length", 2);
        entry.put("opcodeName", "AND_INT_LIT16");
        entry.put("opcodeNum", 213);
        result.add(Collections.unmodifiableMap(entry));
        // 214
        entry = new HashMap<>();
        entry.put("format", "k22s");
        entry.put("length", 2);
        entry.put("opcodeName", "OR_INT_LIT16");
        entry.put("opcodeNum", 214);
        result.add(Collections.unmodifiableMap(entry));
        // 215
        entry = new HashMap<>();
        entry.put("format", "k22s");
        entry.put("length", 2);
        entry.put("opcodeName", "XOR_INT_LIT16");
        entry.put("opcodeNum", 215);
        result.add(Collections.unmodifiableMap(entry));
        // 216
        entry = new HashMap<>();
        entry.put("format", "k22b");
        entry.put("length", 2);
        entry.put("opcodeName", "ADD_INT_LIT8");
        entry.put("opcodeNum", 216);
        result.add(Collections.unmodifiableMap(entry));
        // 217
        entry = new HashMap<>();
        entry.put("format", "k22b");
        entry.put("length", 2);
        entry.put("opcodeName", "RSUB_INT_LIT8");
        entry.put("opcodeNum", 217);
        result.add(Collections.unmodifiableMap(entry));
        // 218
        entry = new HashMap<>();
        entry.put("format", "k22b");
        entry.put("length", 2);
        entry.put("opcodeName", "MUL_INT_LIT8");
        entry.put("opcodeNum", 218);
        result.add(Collections.unmodifiableMap(entry));
        // 219
        entry = new HashMap<>();
        entry.put("format", "k22b");
        entry.put("length", 2);
        entry.put("opcodeName", "DIV_INT_LIT8");
        entry.put("opcodeNum", 219);
        result.add(Collections.unmodifiableMap(entry));
        // 220
        entry = new HashMap<>();
        entry.put("format", "k22b");
        entry.put("length", 2);
        entry.put("opcodeName", "REM_INT_LIT8");
        entry.put("opcodeNum", 220);
        result.add(Collections.unmodifiableMap(entry));
        // 221
        entry = new HashMap<>();
        entry.put("format", "k22b");
        entry.put("length", 2);
        entry.put("opcodeName", "AND_INT_LIT8");
        entry.put("opcodeNum", 221);
        result.add(Collections.unmodifiableMap(entry));
        // 222
        entry = new HashMap<>();
        entry.put("format", "k22b");
        entry.put("length", 2);
        entry.put("opcodeName", "OR_INT_LIT8");
        entry.put("opcodeNum", 222);
        result.add(Collections.unmodifiableMap(entry));
        // 223
        entry = new HashMap<>();
        entry.put("format", "k22b");
        entry.put("length", 2);
        entry.put("opcodeName", "XOR_INT_LIT8");
        entry.put("opcodeNum", 223);
        result.add(Collections.unmodifiableMap(entry));
        // 224
        entry = new HashMap<>();
        entry.put("format", "k22b");
        entry.put("length", 2);
        entry.put("opcodeName", "SHL_INT_LIT8");
        entry.put("opcodeNum", 224);
        result.add(Collections.unmodifiableMap(entry));
        // 225
        entry = new HashMap<>();
        entry.put("format", "k22b");
        entry.put("length", 2);
        entry.put("opcodeName", "SHR_INT_LIT8");
        entry.put("opcodeNum", 225);
        result.add(Collections.unmodifiableMap(entry));
        // 226
        entry = new HashMap<>();
        entry.put("format", "k22b");
        entry.put("length", 2);
        entry.put("opcodeName", "USHR_INT_LIT8");
        entry.put("opcodeNum", 226);
        result.add(Collections.unmodifiableMap(entry));
        // 227
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IGET_QUICK");
        entry.put("opcodeNum", 227);
        result.add(Collections.unmodifiableMap(entry));
        // 228
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IGET_WIDE_QUICK");
        entry.put("opcodeNum", 228);
        result.add(Collections.unmodifiableMap(entry));
        // 229
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IGET_OBJECT_QUICK");
        entry.put("opcodeNum", 229);
        result.add(Collections.unmodifiableMap(entry));
        // 230
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IPUT_QUICK");
        entry.put("opcodeNum", 230);
        result.add(Collections.unmodifiableMap(entry));
        // 231
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IPUT_WIDE_QUICK");
        entry.put("opcodeNum", 231);
        result.add(Collections.unmodifiableMap(entry));
        // 232
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IPUT_OBJECT_QUICK");
        entry.put("opcodeNum", 232);
        result.add(Collections.unmodifiableMap(entry));
        // 233
        entry = new HashMap<>();
        entry.put("format", "k35c");
        entry.put("length", 3);
        entry.put("opcodeName", "INVOKE_VIRTUAL_QUICK");
        entry.put("opcodeNum", 233);
        result.add(Collections.unmodifiableMap(entry));
        // 234
        entry = new HashMap<>();
        entry.put("format", "k3rc");
        entry.put("length", 3);
        entry.put("opcodeName", "INVOKE_VIRTUAL_RANGE_QUICK");
        entry.put("opcodeNum", 234);
        result.add(Collections.unmodifiableMap(entry));
        // 235
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IPUT_BOOLEAN_QUICK");
        entry.put("opcodeNum", 235);
        result.add(Collections.unmodifiableMap(entry));
        // 236
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IPUT_BYTE_QUICK");
        entry.put("opcodeNum", 236);
        result.add(Collections.unmodifiableMap(entry));
        // 237
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IPUT_CHAR_QUICK");
        entry.put("opcodeNum", 237);
        result.add(Collections.unmodifiableMap(entry));
        // 238
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IPUT_SHORT_QUICK");
        entry.put("opcodeNum", 238);
        result.add(Collections.unmodifiableMap(entry));
        // 239
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IGET_BOOLEAN_QUICK");
        entry.put("opcodeNum", 239);
        result.add(Collections.unmodifiableMap(entry));
        // 240
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IGET_BYTE_QUICK");
        entry.put("opcodeNum", 240);
        result.add(Collections.unmodifiableMap(entry));
        // 241
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IGET_CHAR_QUICK");
        entry.put("opcodeNum", 241);
        result.add(Collections.unmodifiableMap(entry));
        // 242
        entry = new HashMap<>();
        entry.put("format", "k22c");
        entry.put("length", 2);
        entry.put("opcodeName", "IGET_SHORT_QUICK");
        entry.put("opcodeNum", 242);
        result.add(Collections.unmodifiableMap(entry));
        // 243
        entry = new HashMap<>();
        entry.put("format", "k10x");
        entry.put("length", 1);
        entry.put("opcodeName", "UNUSED_F3");
        entry.put("opcodeNum", 243);
        result.add(Collections.unmodifiableMap(entry));
        // 244
        entry = new HashMap<>();
        entry.put("format", "k10x");
        entry.put("length", 1);
        entry.put("opcodeName", "UNUSED_F4");
        entry.put("opcodeNum", 244);
        result.add(Collections.unmodifiableMap(entry));
        // 245
        entry = new HashMap<>();
        entry.put("format", "k10x");
        entry.put("length", 1);
        entry.put("opcodeName", "UNUSED_F5");
        entry.put("opcodeNum", 245);
        result.add(Collections.unmodifiableMap(entry));
        // 246
        entry = new HashMap<>();
        entry.put("format", "k10x");
        entry.put("length", 1);
        entry.put("opcodeName", "UNUSED_F6");
        entry.put("opcodeNum", 246);
        result.add(Collections.unmodifiableMap(entry));
        // 247
        entry = new HashMap<>();
        entry.put("format", "k10x");
        entry.put("length", 1);
        entry.put("opcodeName", "UNUSED_F7");
        entry.put("opcodeNum", 247);
        result.add(Collections.unmodifiableMap(entry));
        // 248
        entry = new HashMap<>();
        entry.put("format", "k10x");
        entry.put("length", 1);
        entry.put("opcodeName", "UNUSED_F8");
        entry.put("opcodeNum", 248);
        result.add(Collections.unmodifiableMap(entry));
        // 249
        entry = new HashMap<>();
        entry.put("format", "k10x");
        entry.put("length", 1);
        entry.put("opcodeName", "UNUSED_F9");
        entry.put("opcodeNum", 249);
        result.add(Collections.unmodifiableMap(entry));
        // 250
        entry = new HashMap<>();
        entry.put("format", "k45cc");
        entry.put("length", 4);
        entry.put("opcodeName", "INVOKE_POLYMORPHIC");
        entry.put("opcodeNum", 250);
        result.add(Collections.unmodifiableMap(entry));
        // 251
        entry = new HashMap<>();
        entry.put("format", "k4rcc");
        entry.put("length", 4);
        entry.put("opcodeName", "INVOKE_POLYMORPHIC_RANGE");
        entry.put("opcodeNum", 251);
        result.add(Collections.unmodifiableMap(entry));
        // 252
        entry = new HashMap<>();
        entry.put("format", "k35c");
        entry.put("length", 3);
        entry.put("opcodeName", "INVOKE_CUSTOM");
        entry.put("opcodeNum", 252);
        result.add(Collections.unmodifiableMap(entry));
        // 253
        entry = new HashMap<>();
        entry.put("format", "k3rc");
        entry.put("length", 3);
        entry.put("opcodeName", "INVOKE_CUSTOM_RANGE");
        entry.put("opcodeNum", 253);
        result.add(Collections.unmodifiableMap(entry));
        // 254
        entry = new HashMap<>();
        entry.put("format", "k21c");
        entry.put("length", 2);
        entry.put("opcodeName", "CONST_METHOD_HANDLE");
        entry.put("opcodeNum", 254);
        result.add(Collections.unmodifiableMap(entry));
        // 255
        entry = new HashMap<>();
        entry.put("format", "k21c");
        entry.put("length", 2);
        entry.put("opcodeName", "CONST_METHOD_TYPE");
        entry.put("opcodeNum", 255);
        result.add(Collections.unmodifiableMap(entry));


        return Collections.unmodifiableList(result);
    }

    private static Map<String, Class> createStaticMap() {
        Map<String, Class> result = new HashMap<>();
        result.put("INVOKE_STATIC", OpcodeInvokeKind.class);
        result.put("INVOKE_DIRECT", OpcodeInvokeKind.class);
        result.put("INVOKE_INTERFACE", OpcodeInvokeKind.class);
        result.put("INVOKE_VIRTUAL", OpcodeInvokeKind.class);
        result.put("INVOKE_SUPER", OpcodeInvokeKind.class);
        result.put("INVOKE_STATIC_RANGE", OpcodeInvokeKindRange.class);
        result.put("INVOKE_DIRECT_RANGE", OpcodeInvokeKindRange.class);
        result.put("INVOKE_INTERFACE_RANGE", OpcodeInvokeKindRange.class);
        result.put("INVOKE_VIRTUAL_RANGE", OpcodeInvokeKindRange.class);
        result.put("INVOKE_SUPER_RANGE", OpcodeInvokeKindRange.class);
        result.put("INVOKE_VIRTUAL_QUICK", OpcodeInvokeKindQuick.class);
        result.put("INVOKE_VIRTUAL_RANGE_QUICK", OpcodeInvokeKindQuick.class);
        result.put("INSTANCE_OF", OpcodeInstanceOf.class);
        result.put("IGET", OpcodeIInstanceOp.class);
        result.put("IGET_OBJECT", OpcodeIInstanceOp.class);
        result.put("IGET_WIDE", OpcodeIInstanceOp.class);
        result.put("IGET_BOOLEAN", OpcodeIInstanceOp.class);
        result.put("IGET_BYTE", OpcodeIInstanceOp.class);
        result.put("IGET_CHAR", OpcodeIInstanceOp.class);
        result.put("IGET_SHORT", OpcodeIInstanceOp.class);
        result.put("IPUT", OpcodeIInstanceOp.class);
        result.put("IPUT_OBJECT", OpcodeIInstanceOp.class);
        result.put("IPUT_WIDE", OpcodeIInstanceOp.class);
        result.put("IPUT_BOOLEAN", OpcodeIInstanceOp.class);
        result.put("IPUT_BYTE", OpcodeIInstanceOp.class);
        result.put("IPUT_CHAR", OpcodeIInstanceOp.class);
        result.put("IPUT_SHORT", OpcodeIInstanceOp.class);
        result.put("IGET_QUICK", OpcodeIInstanceOpQuick.class);
        result.put("IGET_BYTE_QUICK", OpcodeIInstanceOpQuick.class);
        result.put("IGET_CHAR_QUICK", OpcodeIInstanceOpQuick.class);
        result.put("IGET_SHORT_QUICK", OpcodeIInstanceOpQuick.class);
        result.put("IGET_WIDE_QUICK", OpcodeIInstanceOpQuick.class);
        result.put("IGET_OBJECT_QUICK", OpcodeIInstanceOpQuick.class);
        result.put("IGET_BOOLEAN_QUICK", OpcodeIInstanceOpQuick.class);
        result.put("IPUT_OBJECT_QUICK", OpcodeIInstanceOpQuick.class);
        result.put("IPUT_BYTE_QUICK", OpcodeIInstanceOpQuick.class);
        result.put("IPUT_CHAR_QUICK", OpcodeIInstanceOpQuick.class);
        result.put("IPUT_BOOLEAN_QUICK", OpcodeIInstanceOpQuick.class);
        result.put("IPUT_WIDE_QUICK", OpcodeIInstanceOpQuick.class);
        result.put("IPUT_SHORT_QUICK", OpcodeIInstanceOpQuick.class);
        result.put("IPUT_QUICK", OpcodeIInstanceOpQuick.class);
        result.put("SGET", OpcodeSStaticOp.class);
        result.put("SGET_WIDE", OpcodeSStaticOp.class);
        result.put("SGET_OBJECT", OpcodeSStaticOp.class);
        result.put("SGET_BOOLEAN", OpcodeSStaticOp.class);
        result.put("SGET_BYTE", OpcodeSStaticOp.class);
        result.put("SGET_CHAR", OpcodeSStaticOp.class);
        result.put("SGET_SHORT", OpcodeSStaticOp.class);
        result.put("SPUT", OpcodeSStaticOp.class);
        result.put("SPUT_WIDE", OpcodeSStaticOp.class);
        result.put("SPUT_OBJECT", OpcodeSStaticOp.class);
        result.put("SPUT_BOOLEAN", OpcodeSStaticOp.class);
        result.put("SPUT_BYTE", OpcodeSStaticOp.class);
        result.put("SPUT_CHAR", OpcodeSStaticOp.class);
        result.put("SPUT_SHORT", OpcodeSStaticOp.class);
        result.put("CONST_STRING", OpcodeConstString.class);
        result.put("CONST_STRING_JUMBO", OpcodeConstStringJumbo.class);
        result.put("CONST_CLASS", OpcodeConstClass.class);
        result.put("NEW_INSTANCE", OpcodeNewInstance.class);
        result.put("CHECK_CAST", OpcodeCheckCast.class);
        result.put("NEW_ARRAY", OpcodeNewArray.class);
        result.put("FILLED_NEW_ARRAY", OpcodeFilledNewArray.class);
        result.put("FILLED_NEW_ARRAY_RANGE", OpcodeFilledNewArray.class);
        result.put("NOP", OpcodeHashFull.class);
        result.put("RETURN", OpcodeHashFull.class);
        result.put("RETURN_WIDE", OpcodeHashFull.class);
        result.put("RETURN_VOID", OpcodeHashFull.class);
        result.put("RETURN_OBJECT", OpcodeHashFull.class);
        result.put("RETURN_VOID_NO_BARRIER", OpcodeHashFull.class);
        result.put("THROW", OpcodeHashFull.class);
        result.put("MOVE", OpcodeHashFull.class);
        result.put("MOVE_FROM16", OpcodeHashFull.class);
        result.put("MOVE_16", OpcodeHashFull.class);
        result.put("MOVE_WIDE", OpcodeHashFull.class);
        result.put("MOVE_WIDE_FROM16", OpcodeHashFull.class);
        result.put("MOVE_WIDE_16", OpcodeHashFull.class);
        result.put("MOVE_RESULT", OpcodeHashFull.class);
        result.put("MOVE_RESULT_OBJECT", OpcodeHashFull.class);
        result.put("MOVE_RESULT_WIDE", OpcodeHashFull.class);
        result.put("MOVE_EXCEPTION", OpcodeHashFull.class);
        result.put("MOVE_OBJECT", OpcodeHashFull.class);
        result.put("MOVE_OBJECT_FROM16", OpcodeHashFull.class);
        result.put("MOVE_OBJECT_16", OpcodeHashFull.class);
        result.put("CONST", OpcodeConst.class);
        result.put("CONST_4", OpcodeHashFull.class);
        result.put("CONST_16", OpcodeHashFull.class);
        result.put("CONST_HIGH16", OpcodeHashFull.class);
        result.put("CONST_WIDE", OpcodeHashFull.class);
        result.put("CONST_WIDE_16", OpcodeHashFull.class);
        result.put("CONST_WIDE_HIGH16", OpcodeHashFull.class);
        result.put("CONST_WIDE_32", OpcodeHashFull.class);
        result.put("CMPL_FLOAT", OpcodeHashFull.class);
        result.put("CMPG_FLOAT", OpcodeHashFull.class);
        result.put("CMPL_DOUBLE", OpcodeHashFull.class);
        result.put("CMPG_DOUBLE", OpcodeHashFull.class);
        result.put("CMP_LONG", OpcodeHashFull.class);
        result.put("IF_EQ", OpcodeHashFull.class);
        result.put("IF_NE", OpcodeHashFull.class);
        result.put("IF_LE", OpcodeHashFull.class);
        result.put("IF_GE", OpcodeHashFull.class);
        result.put("IF_LT", OpcodeHashFull.class);
        result.put("IF_GT", OpcodeHashFull.class);
        result.put("IF_EQZ", OpcodeHashFull.class);
        result.put("IF_NEZ", OpcodeHashFull.class);
        result.put("IF_LTZ", OpcodeHashFull.class);
        result.put("IF_GEZ", OpcodeHashFull.class);
        result.put("IF_GTZ", OpcodeHashFull.class);
        result.put("IF_LEZ", OpcodeHashFull.class);
        result.put("GOTO", OpcodeHashFull.class);
        result.put("GOTO_16", OpcodeHashFull.class);
        result.put("GOTO_32", OpcodeHashFull.class);
        result.put("MONITOR_ENTER", OpcodeHashFull.class);
        result.put("MONITOR_EXIT", OpcodeHashFull.class);
        result.put("ARRAY_LENGTH", OpcodeHashFull.class);
        result.put("FILL_ARRAY_DATA", OpcodeHashFull.class);
        result.put("SPARSE_SWITCH", OpcodeHashFull.class);
        result.put("PACKED_SWITCH", OpcodeHashFull.class);
        result.put("AGET", OpcodeHashFull.class);
        result.put("AGET_WIDE", OpcodeHashFull.class);
        result.put("AGET_OBJECT", OpcodeHashFull.class);
        result.put("AGET_BOOLEAN", OpcodeHashFull.class);
        result.put("AGET_BYTE", OpcodeHashFull.class);
        result.put("AGET_CHAR", OpcodeHashFull.class);
        result.put("AGET_SHORT", OpcodeHashFull.class);
        result.put("APUT", OpcodeHashFull.class);
        result.put("APUT_WIDE", OpcodeHashFull.class);
        result.put("APUT_OBJECT", OpcodeHashFull.class);
        result.put("APUT_BOOLEAN", OpcodeHashFull.class);
        result.put("APUT_BYTE", OpcodeHashFull.class);
        result.put("APUT_CHAR", OpcodeHashFull.class);
        result.put("APUT_SHORT", OpcodeHashFull.class);
        result.put("ADD_INT_LIT8", OpcodeHashFull.class);
        result.put("RSUB_INT_LIT8", OpcodeHashFull.class);
        result.put("MUL_INT_LIT8", OpcodeHashFull.class);
        result.put("DIV_INT_LIT8", OpcodeHashFull.class);
        result.put("REM_INT_LIT8", OpcodeHashFull.class);
        result.put("AND_INT_LIT8", OpcodeHashFull.class);
        result.put("OR_INT_LIT8", OpcodeHashFull.class);
        result.put("XOR_INT_LIT8", OpcodeHashFull.class);
        result.put("SHL_INT_LIT8", OpcodeHashFull.class);
        result.put("SHR_INT_LIT8", OpcodeHashFull.class);
        result.put("USHR_INT_LIT8", OpcodeHashFull.class);
        result.put("INVOKE_POLYMORPHIC", OpcodeInvokePolymorphic.class);
        result.put("INVOKE_POLYMORPHIC_RANGE", OpcodeInvokePolymorphic.class);

        return Collections.unmodifiableMap(result);
    }

}
