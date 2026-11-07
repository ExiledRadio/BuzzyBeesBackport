# Stream L: languages (Rule 4), first pass

The family's tool, `tools/lang/build_langs.py`, copied unchanged from Backport Core's `languages` branch;
settings in `tools/lang/langs.json` (Mojang versions 26.3, 1.15.2 and 1.14.4, the family's banned words).

## Done

*   All 18 lang files exist next to `en_us.lang`, with every vanilla name from Mojang's files: 31 keys (blocks,
    items, the bee, the advancements, subtitles, death messages, "Respawn point set" through
    `tools/lang/vanilla_map.txt`, since 26.3 renamed its key).
*   `tools/lang/todo/<code>.lang`: the 57 keys of our own text each language still needs (config names and
    tooltips, the creative tab, the two hive tooltips), with the English.

## Not done

*   **Our own text is not translated yet**: 57 keys x 15 languages (es_ar, es_uy and es_ve copy es_mx). Until
    `tools/lang/translations/<code>.lang` exists for each, the game shows English for them and
    `python -I tools/lang/build_langs.py check` reports them as missing (1027 errors). Then `build`,
    `snapshot`, commit.
*   26.3's English differs for two advancement descriptions ("Glass Bottle", "Bee Nest or Beehive"); the
    translations use 26.3's text, en_us keeps 1.18's.
