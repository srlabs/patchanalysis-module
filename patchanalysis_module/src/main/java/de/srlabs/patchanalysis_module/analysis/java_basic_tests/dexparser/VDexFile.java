package de.srlabs.patchanalysis_module.analysis.java_basic_tests.dexparser;

import de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static de.srlabs.patchanalysis_module.analysis.java_basic_tests.Helper.longToIntExact;

public class VDexFile extends DexContainer {

    ArrayList<DexFile> dexFiles = new ArrayList<>();
    VDexHeader header;
    BufWithOffset self;
    DexSectHeader dexSectHeader;

    enum VDexHeaderType {
        VDex_006_010,
        VDex_019
    }

    public VDexFile(byte[] buf, int offset) {

        self = new BufWithOffset(buf, offset);
        byte[] magic = self.getSlice(0, 4);
        String magicString = new String(magic, StandardCharsets.UTF_8);

        if (magicString.equals("OPPO")) {
            throw new RuntimeException("Invalid VDEX magic: OPPO");
        }

        if (!magicString.equals("vdex")) {
            throw new RuntimeException("Invalid VDEX magic:" + Helper.bytesToHex(magic));
        }

        byte[] version = self.getSlice(4, 8);
        String versionHex = Helper.bytesToHex(version);
        if (versionHex.equals("30313000") || versionHex.equals("30303600")) {
            this.header = new VDEXHeader006_010(self.getSlice(0, VDEXHeader006_010.getSize()));
            int pos = VDEXHeader006_010.getSize() + 4 * longToIntExact(this.header.numberOfDexFiles);
            for (int i = 0; i < this.header.numberOfDexFiles; i++) {
                DexFile dexFile = new DexFile(self.buf, self.offset + pos);
                this.dexFiles.add(dexFile);
                pos += dexFile.header.fileSize;
            }
        } else if (versionHex.equals("30313900")) {
            this.header = new VDEXHeader019(self.getSlice(0, VDEXHeader019.getSize()));
            String dexSectionVersionHex = Helper.bytesToHex(this.header.dexSectionVersion);
            if (!(dexSectionVersionHex.equals("30303000") || dexSectionVersionHex.equals("30303200"))) {
                throw new RuntimeException("Invalid DEX section version: " + Helper.bytesToHex(header.dexSectionVersion));
            }

            // b'000\0' => kDexSectVerEmpty => No dex/cdex in file
            if (dexSectionVersionHex.equals("30303200")) {
                int pos = VDEXHeader019.getSize() + 4 * longToIntExact(this.header.numberOfDexFiles);
                this.dexSectHeader = new DexSectHeader(self.getSlice(pos, pos + DexSectHeader.getSize()));
                pos += DexSectHeader.getSize();
                BufWithOffset sharedData = new BufWithOffset(self, longToIntExact(pos +
                        this.dexSectHeader.dexSize));
                for (int dexFileNum = 0; dexFileNum < this.header.numberOfDexFiles; dexFileNum++) {
                    pos += 4;
                    DexFile dexFile = new DexFile(self.buf, pos, sharedData);
                    this.dexFiles.add(dexFile);
                    pos += dexFile.header.fileSize;
                }
            }
        } else {
            throw new RuntimeException("Unsupported VDEX version: " + versionHex);
        }
    }

    public VDexFile(byte[] buf) {
        this(buf, 0);
    }

    public ArrayList<DexFile> getDexFiles() {
        return dexFiles;
    }
}
