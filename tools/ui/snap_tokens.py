#!/usr/bin/env python3
"""
One-off migration: snap literal spacing, corners and type in the UI onto the
tokens in Theme.kt. Kept because DESIGN_SYSTEM.md explains its choices, and so a
long-lived branch can be brought across. DesignSystemTest stops new literals.

Run from the repo root.
"""
import glob
import os
import re

UI = "app/src/main/java/com/ampersand/ui"
EXEMPT_DIRS = ("/theme/", "/brick/", "/splash/")

SPACE = {
    2: "hair", 3: "xs", 4: "xs", 5: "xs",
    6: "s", 7: "s", 8: "s",
    10: "m", 12: "m", 13: "m", 14: "m",
    16: "l", 18: "l",
    20: "xl", 22: "xl", 24: "xl", 26: "xl",
    28: "xxl", 30: "xxl", 32: "xxl", 36: "xxl",
    40: "page", 48: "page", 60: "page",
}
CORNER = {
    3: "hair", 6: "small", 8: "small",
    10: "medium", 12: "medium", 14: "medium", 16: "medium",
    20: "control", 28: "card", 36: "card",
}
TYPE = {"labelMedium": "labelLarge", "titleLarge": "headlineSmall"}


def space(match_value):
    n = int(match_value)
    return f"Space.{SPACE[n]}" if n in SPACE else f"{n}.dp"


def migrate_line(line, is_screen):
    if "// course-fit" in line:
        return line
    # A screen's own side margin is one value, whatever it was.
    if is_screen:
        line = re.sub(r"\.padding\(horizontal = (20|24|28)\.dp\)", ".padding(horizontal = Inset.screen)", line)
        line = re.sub(r"\.padding\(horizontal = 28\.dp, vertical", ".padding(horizontal = Inset.screen, vertical", line)
        line = re.sub(r"PaddingValues\(start = 24\.dp, end = 24\.dp", "PaddingValues(start = Inset.screen, end = Inset.screen", line)

    line = re.sub(r"(Spacer\(Modifier\.(?:height|width)\()(\d+)\.dp", lambda m: m[1] + space(m[2]), line)
    line = re.sub(r"(spacedBy\()(\d+)\.dp", lambda m: m[1] + space(m[2]), line)
    line = re.sub(r"(padding\()(\d+)\.dp", lambda m: m[1] + space(m[2]), line)
    line = re.sub(r"\b((?:horizontal|vertical) = )(\d+)\.dp", lambda m: m[1] + space(m[2]), line)
    if "padding(" in line or "PaddingValues(" in line or re.fullmatch(r"\s*(?:top|bottom|start|end) = \d+\.dp,?\s*", line):
        line = re.sub(r"\b((?:top|bottom|start|end) = )(\d+)\.dp", lambda m: m[1] + space(m[2]), line)

    line = re.sub(r"RoundedCornerShape\((\d+)\.dp\)", lambda m: f"Radius.{CORNER[int(m[1])]}", line)
    line = re.sub(r"\b(corner = )(\d+)\.dp", lambda m: f"{m[1]}Corner.{CORNER[int(m[2])]}", line)
    line = re.sub(r"typography\.(\w+)", lambda m: "typography." + TYPE.get(m[1], m[1]), line)
    return line


def add_imports(text, eol):
    for token in ("Space", "Inset", "Corner", "Radius"):
        statement = f"import com.ampersand.ui.theme.{token}"
        if re.search(rf"\b{token}\.", text) and statement not in text:
            text = text.replace("\nimport ", f"\n{statement}{eol}import ", 1)
    return text


def main():
    for path in glob.glob(UI + "/**/*.kt", recursive=True):
        norm = path.replace(os.sep, "/")
        name = os.path.basename(norm)
        if any(d in norm for d in EXEMPT_DIRS) or name.endswith("Art.kt") or name == "StudBoard.kt":
            continue
        with open(path, encoding="utf-8", newline="") as f:
            text = f.read()
        eol = "\r\n" if "\r\n" in text else "\n"
        is_screen = name.endswith("Screen.kt")
        out = eol.join(migrate_line(line, is_screen) for line in text.split(eol))
        out = add_imports(out, eol)
        if out != text:
            with open(path, "w", encoding="utf-8", newline="") as f:
                f.write(out)
            print("migrated", name)


if __name__ == "__main__":
    main()
