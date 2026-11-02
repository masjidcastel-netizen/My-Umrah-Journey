#!/usr/bin/env python3
"""Reports whether the native libraries in an APK are ready for 16 KB memory pages.

Usage: python3 tools/check_elf_alignment.py path/to/app.apk

For every lib/<abi>/*.so it prints the alignment of the ELF LOAD segments and, for libraries stored
uncompressed, the offset of their data inside the APK. Google Play requires 64-bit libraries
(arm64-v8a, x86_64) to support 16 KB pages for apps targeting Android 15+: LOAD alignment of at least
0x4000 and, when stored uncompressed, an APK data offset that is a multiple of 16384.

Informational only: always exits 0, so it never blocks a build. Pure standard library.
"""
import struct
import sys
import zipfile

PT_LOAD = 1
PAGE_16K = 0x4000


def load_alignments(data: bytes):
    if data[:4] != b"\x7fELF":
        return None
    is64 = data[4] == 2
    little = data[5] == 1
    e = "<" if little else ">"
    if is64:
        phoff = struct.unpack_from(e + "Q", data, 0x20)[0]
        phentsize, phnum = struct.unpack_from(e + "HH", data, 0x36)
        fmt, align_at = e + "IIQQQQQQ", 48
    else:
        phoff = struct.unpack_from(e + "I", data, 0x1C)[0]
        phentsize, phnum = struct.unpack_from(e + "HH", data, 0x2A)
        fmt, align_at = e + "IIIIIIII", 28
    aligns = []
    for i in range(phnum):
        off = phoff + i * phentsize
        if is64:
            p_type, _flags, _o, _v, _p, _fs, _ms, p_align = struct.unpack_from(fmt, data, off)
        else:
            p_type, _o, _v, _p, _fs, _ms, _flags, p_align = struct.unpack_from(fmt, data, off)
        if p_type == PT_LOAD:
            aligns.append(p_align)
    return is64, aligns


def data_offset(path: str, info: zipfile.ZipInfo) -> int:
    with open(path, "rb") as f:
        f.seek(info.header_offset)
        header = f.read(30)
    name_len, extra_len = struct.unpack_from("<HH", header, 26)
    return info.header_offset + 30 + name_len + extra_len


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__)
        return 0
    apk = sys.argv[1]
    problems = 0
    with zipfile.ZipFile(apk) as z:
        libs = sorted(
            (i for i in z.infolist() if i.filename.startswith("lib/") and i.filename.endswith(".so")),
            key=lambda i: i.filename,
        )
        if not libs:
            print("No native libraries found in", apk)
        for info in libs:
            result = load_alignments(z.read(info))
            if result is None:
                print(f"{info.filename}: not an ELF file")
                continue
            is64, aligns = result
            min_align = min(aligns) if aligns else 0
            stored = info.compress_type == zipfile.ZIP_STORED
            offset = data_offset(apk, info) if stored else None
            elf_ok = min_align >= PAGE_16K
            zip_ok = (offset % 16384 == 0) if stored else True
            needs_16k = "/arm64-v8a/" in info.filename or "/x86_64/" in info.filename
            verdict = "16KB-OK" if (elf_ok and zip_ok) else "NOT 16KB-ready"
            if needs_16k and verdict != "16KB-OK":
                problems += 1
            print(
                f"{info.filename}: {'64' if is64 else '32'}-bit, LOAD align min=0x{min_align:x}, "
                f"{'stored, data offset %% 16384 = %d' % (offset % 16384) if stored else 'compressed'} "
                f"-> {verdict}{' (Play requires this for 64-bit libs)' if needs_16k and verdict != '16KB-OK' else ''}"
            )
    print(f"\n64-bit libraries not 16 KB-ready: {problems}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
