#!/usr/bin/env python3
"""
One-off migration, the second half of snap_tokens.py: opacity onto Alpha, icon sizes
onto IconSize, and the neutral card onto Modifier.panel(). Run from the repo root.
"""
import glob
import os
import re

UI = "app/src/main/java/com/ampersand/ui"
EXEMPT_DIRS = ("/theme/", "/brick/", "/splash/")

ALPHA = {
    "0.12": "faint", "0.13": "faint",
    "0.16": "unlit", "0.2": "unlit", "0.25": "unlit", "0.28": "unlit", "0.3": "unlit", "0.32": "unlit",
    "0.35": "dim", "0.4": "dim", "0.40": "dim", "0.45": "dim", "0.5": "dim",
    "0.55": "quiet", "0.6": "quiet", "0.65": "quiet",
    "0.72": "secondary", "0.75": "secondary", "0.8": "secondary", "0.85": "secondary",
    "0.88": "strong", "0.9": "strong", "0.90": "strong", "0.92": "strong", "0.94": "strong",
}
ICON = {14: "mark", 20: "small", 22: "medium", 24: "medium", 26: "medium", 30: "large", 34: "large", 40: "tile", 44: "tile"}
SPACE = {4: "xs", 6: "s", 8: "s", 10: "m", 12: "m"}
IMPORTS = {
    "Alpha": "com.ampersand.ui.theme.Alpha",
    "IconSize": "com.ampersand.ui.theme.IconSize",
    "Space": "com.ampersand.ui.theme.Space",
    "Emphasis": "com.ampersand.ui.components.Emphasis",
}


def migrate_line(line):
    # A faded control that comes up to full as it is selected.
    line = line.replace("alpha = 0.34f + 0.66f * lift", "alpha = Alpha.rise(Alpha.dim, lift)")
    line = line.replace("alpha = 0.55f + 0.45f * lift", "alpha = Alpha.rise(Alpha.quiet, lift)")
    line = line.replace("alpha = 0.45f + 0.55f * lift", "alpha = Alpha.rise(Alpha.quiet, lift)")
    line = line.replace("GhostAlpha * 0.7f", "Alpha.faint")
    if re.search(r"\balpha\b", line):
        line = re.sub(r"\b(0\.\d+)f\b", lambda m: f"Alpha.{ALPHA[m[1]]}", line)

    line = re.sub(r"\bsize = (\d+)\.dp", lambda m: f"size = IconSize.{ICON[int(m[1])]}", line)
    line = re.sub(r"Spacer\(Modifier\.size\((\d+)\.dp\)", lambda m: f"Spacer(Modifier.size(Space.{SPACE[int(m[1])]})", line)
    line = re.sub(r"(Canvas\((?:Modifier|modifier)\.size\()(\d+)\.dp", lambda m: f"{m[1]}IconSize.{ICON[int(m[2])]}", line)
    line = re.sub(r"(WeatherBrickArt\(it, Modifier\.size\()(\d+)\.dp", lambda m: f"{m[1]}IconSize.{ICON[int(m[2])]}", line)

    line = line.replace(".glass(inkOnBrick(structureSlug()), corner = Corner.medium, strength = if (on) 2.2f else 1.4f)", ".panel(if (on) Emphasis.SELECTED else Emphasis.RAISED)")
    line = line.replace(".glass(inkOnBrick(structureSlug()), corner = Corner.medium, strength = 1.4f)", ".panel(Emphasis.RAISED)")
    line = line.replace(".glass(inkOnBrick(structureSlug()), corner = Corner.medium)", ".panel()")
    line = line.replace(".glass(ink, corner = Corner.medium, strength = 1.6f)", ".glass(ink, corner = Corner.medium, strength = Emphasis.RAISED.strength)")
    return line


def add_imports(text, eol, package):
    for token, path in IMPORTS.items():
        if path.rsplit(".", 1)[0] == package:
            continue
        if re.search(rf"\b{token}\.", text) and f"import {path}" not in text:
            text = text.replace("\nimport ", f"\nimport {path}{eol}import ", 1)
    if ".panel(" in text and package != "com.ampersand.ui.components" and "components.panel" not in text:
        text = text.replace("\nimport ", f"\nimport com.ampersand.ui.components.panel{eol}import ", 1)
    return text


def main():
    for path in glob.glob(UI + "/**/*.kt", recursive=True):
        norm = path.replace(os.sep, "/")
        name = os.path.basename(norm)
        if any(d in norm for d in EXEMPT_DIRS) or name.endswith("Art.kt") or name in ("StudBoard.kt", "Panel.kt"):
            continue
        with open(path, encoding="utf-8", newline="") as f:
            text = f.read()
        eol = "\r\n" if "\r\n" in text else "\n"
        package = re.search(r"^package (\S+)", text, re.M)[1]
        out = eol.join(migrate_line(line) for line in text.split(eol))
        out = add_imports(out, eol, package)
        if out != text:
            with open(path, "w", encoding="utf-8", newline="") as f:
                f.write(out)
            print("migrated", name)


if __name__ == "__main__":
    main()
