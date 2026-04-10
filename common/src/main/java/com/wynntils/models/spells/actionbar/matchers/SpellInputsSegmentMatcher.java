/*
 * Copyright © Wynntils 2024-2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.spells.actionbar.matchers;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.actionbar.ActionBarSegment;
import com.wynntils.handlers.actionbar.ActionBarSegmentMatcher;
import com.wynntils.models.spells.actionbar.segments.SpellInputsSegment;
import com.wynntils.models.spells.type.SpellDirection;
import com.wynntils.models.spells.type.SpellType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SpellInputsSegmentMatcher implements ActionBarSegmentMatcher {
    // The start and end of a spell segment, a spacer
    private static final String SEGMENT_SEPARATOR = "\uDAFF\uDFE0";

    // The character for left click
    private static final String LEFT_CLICK = "[\uE100\uE103]";

    // The character for right click
    private static final String RIGHT_CLICK = "[\uE101|\uE104]";

    // The character for no click (yet)
    private static final String NO_CLICK = "[\uE102\uE105]";

    // The separator between the spell clicks (space + arrow)
    private static final String SEPARATOR = "\\s\uE106\\s";

    private static final Pattern SPELL_REGEX = Pattern.compile(SEGMENT_SEPARATOR
            + "(?<first>" + LEFT_CLICK + "|" + RIGHT_CLICK + "|" + NO_CLICK + ")" + SEPARATOR
            + "(?<second>" + LEFT_CLICK + "|" + RIGHT_CLICK + "|" + NO_CLICK + ")" + SEPARATOR
            + "(?<third>" + LEFT_CLICK + "|" + RIGHT_CLICK + "|" + NO_CLICK + ")"
            + SEGMENT_SEPARATOR); // TODO: The third click will no longer be matched due to how Wynncraft now
    // replaces the spell input bar with the spell name the instant it is cast
    private static final Pattern NO_CLICK_PATTERN = Pattern.compile(NO_CLICK);
    private static final Pattern RIGHT_CLICK_PATTERN = Pattern.compile(RIGHT_CLICK);
    private static final Pattern LEFT_CLICK_PATTERN = Pattern.compile(LEFT_CLICK);

    private static final Pattern CAST_REGEX;
    private static final Map<String, SpellType> NAME_MAP = new HashMap<>();

    static {
        for (SpellType spell : SpellType.values()) NAME_MAP.put(spell.getName(), spell);

        String allSpellNames = Arrays.stream(SpellType.values())
                .map(SpellType::getName)
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));

        CAST_REGEX = Pattern.compile("(?<spell>" + allSpellNames + ")");
    }

    @Override
    public ActionBarSegment parse(StyledText actionBar) {
        String actionBarString = actionBar.getStringWithoutFormatting();

        Matcher matcher = SPELL_REGEX.matcher(actionBarString);
        Matcher castMatcher = CAST_REGEX.matcher(actionBarString);
        if (matcher.find()) {
            SpellDirection first = fromCharacter(matcher.group("first"));
            SpellDirection second = fromCharacter(matcher.group("second"));
            SpellDirection third = fromCharacter(matcher.group("third"));

            SpellDirection[] directions;

            if (first == null) {
                directions = SpellDirection.NO_SPELL;
            } else if (second == null) {
                directions = new SpellDirection[] {first};
            } else if (third == null) {
                directions = new SpellDirection[] {first, second};
            } else {
                directions = new SpellDirection[] {first, second, third};
            }

            return new SpellInputsSegment(matcher.group(), matcher.start(), matcher.end(), directions);
        } else if (castMatcher.find()) {
            SpellType spell = fromName(castMatcher.group("spell"));

            SpellDirection[] directions = SpellType.toSpellDirectionArray(spell.getSpellNumber());
            if (directions[0] == SpellDirection.RIGHT) {
                directions = SpellDirection.invertArray(directions);
            }

            return new SpellInputsSegment(castMatcher.group(), castMatcher.start(), castMatcher.end(), directions);
        }
        return null;
    }

    private SpellType fromName(String name) {
        return NAME_MAP.get(name);
    }

    private SpellDirection fromCharacter(String spellCharacter) {
        if (LEFT_CLICK_PATTERN.matcher(spellCharacter).matches()) {
            return SpellDirection.LEFT;
        } else if (RIGHT_CLICK_PATTERN.matcher(spellCharacter).matches()) {
            return SpellDirection.RIGHT;
        } else if (NO_CLICK_PATTERN.matcher(spellCharacter).matches()) {
            return null;
        }

        WynntilsMod.warn("Unknown spell character: " + spellCharacter);
        return null;
    }
}
