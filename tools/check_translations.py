#!/usr/bin/env python3
"""
Checks the translated strings against the English base.

Run from the project root:  python3 tools/check_translations.py

Catches the three things that actually break at runtime, none of which a translator can see:

  * a key that is missing or has been invented in one locale
  * a placeholder that was dropped, duplicated or renumbered -- "%2$s" turning into "%s" is a
    crash, not a typo, and it crashes only in that one language
  * a plural that is missing a quantity its language requires (Arabic needs six, Russian four)

It also reports how much is still English, so "translated" is a number rather than an impression.
"""

import collections
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

RES = pathlib.Path("app/src/main/res")

# Quantities each language actually uses, per CLDR. Anything not listed needs one/other.
REQUIRED_QUANTITIES = {
    "ar": {"zero", "one", "two", "few", "many", "other"},
    "ru": {"one", "few", "many", "other"},
    "uk": {"one", "few", "many", "other"},
    "es": {"one", "many", "other"},
    "fr": {"one", "many", "other"},
    "pt": {"one", "many", "other"},
    # Languages with no plural inflection at all. Listing them matters: the fallback below is
    # one/other, which would wrongly demand a "one" form these languages do not have.
    "zh": {"other"},
    "ja": {"other"},
    "ko": {"other"},
    "vi": {"other"},
    "in": {"other"},
    "th": {"other"},
}


def args_in(text):
    return sorted(re.findall(r"%(\d+)\$[sd]", text or ""))


def language_of(folder):
    return folder.name.removeprefix("values-").split("-")[0]


def main():
    base_file = RES / "values" / "strings.xml"
    if not base_file.exists():
        sys.exit("run me from the project root")

    base = ET.parse(base_file).getroot()
    base_strings = {
        e.get("name"): (e.text or "")
        for e in base.findall("string")
        if e.get("translatable") != "false"
    }
    base_plurals = {e.get("name"): (e.find("item").text or "") for e in base.findall("plurals")}

    failures = 0
    print(f"base: {len(base_strings)} translatable strings, {len(base_plurals)} plurals\n")

    for folder in sorted(RES.glob("values-*")):
        f = folder / "strings.xml"
        if not f.exists():
            continue
        root = ET.parse(f).getroot()
        strings = {e.get("name"): (e.text or "") for e in root.findall("string")}
        plurals = {e.get("name"): e for e in root.findall("plurals")}

        problems = []
        for name in sorted(set(base_strings) - set(strings)):
            problems.append(f"missing string: {name}")
        for name in sorted(set(strings) - set(base_strings)):
            problems.append(f"unknown string (not in base): {name}")
        for name, text in sorted(strings.items()):
            if name in base_strings and args_in(text) != args_in(base_strings[name]):
                problems.append(
                    f"{name}: placeholders {args_in(base_strings[name])} became {args_in(text)}"
                )

        wanted = REQUIRED_QUANTITIES.get(language_of(folder), {"one", "other"})
        for name, el in sorted(plurals.items()):
            have = {i.get("quantity") for i in el}
            if missing := wanted - have:
                problems.append(f"{name}: missing plural forms {sorted(missing)}")
            # A plural item may legitimately use FEWER arguments than the base: Arabic words its
            # singular and dual ("one day", "two days") instead of printing a digit, and the
            # surplus argument is simply ignored at format time. Using a HIGHER index than the
            # caller supplies is the real hazard, so only that is an error.
            available = len(args_in(base_plurals.get(name, "")))
            for item in el:
                used = args_in(item.text)
                if used and max(int(i) for i in used) > max(available, 1):
                    problems.append(
                        f"{name}[{item.get('quantity')}]: uses %{max(used)}$ "
                        f"but only {available} argument(s) are passed"
                    )

        english = sum(1 for k, v in strings.items() if k in base_strings and v == base_strings[k])
        done = len(base_strings) - english
        pct = round(100 * done / len(base_strings)) if base_strings else 0

        status = "OK " if not problems else "FAIL"
        print(f"{status} {folder.name:<16} translated {done}/{len(base_strings)} ({pct}%)")
        for p in problems:
            failures += 1
            print(f"       - {p}")

    print()
    if failures:
        print(f"{failures} problem(s) found")
        return 1
    print("no structural problems")
    return 0


if __name__ == "__main__":
    sys.exit(main())
