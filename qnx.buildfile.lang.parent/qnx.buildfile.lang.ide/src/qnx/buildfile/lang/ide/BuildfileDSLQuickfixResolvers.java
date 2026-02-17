package qnx.buildfile.lang.ide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import qnx.buildfile.lang.attributes.AttributeKeywords;

/**
 * Shared quickfix resolution logic for buildfile validation issues.
 * Used by both the Eclipse quickfix provider ({@code BuildfileDSLQuickfixProvider})
 * and the LSP code action service ({@code BuildfileDSLCodeActionService}).
 * <p>
 * No Eclipse UI or lsp4j dependencies — pure Java utility.
 */
public final class BuildfileDSLQuickfixResolvers {

    /** Maximum Levenshtein distance to consider a suggestion relevant. */
    private static final int MAX_DISTANCE = 4;

    /** Maximum number of suggestions to offer. */
    private static final int MAX_SUGGESTIONS = 3;

    private BuildfileDSLQuickfixResolvers() {}

    /**
     * A suggested replacement for an invalid attribute name.
     */
    public static final class Suggestion {
        private final String keyword;
        private final int distance;

        Suggestion(String keyword, int distance) {
            this.keyword = keyword;
            this.distance = distance;
        }

        public String getKeyword() { return keyword; }
        public int getDistance() { return distance; }
    }

    /**
     * Find the closest matching attribute keywords for an unknown attribute name.
     *
     * @param badName the unrecognized attribute name
     * @return up to {@value #MAX_SUGGESTIONS} closest matches, sorted by distance
     */
    public static List<Suggestion> suggestAttributeNames(String badName) {
        if (badName == null || badName.isEmpty()) {
            return List.of();
        }
        return findClosestMatches(badName, AttributeKeywords.ALL_ATTRIBUTE_KEYWORDS);
    }

    /**
     * Extract the bad attribute name from a validation error message.
     * <p>
     * Handles messages like:
     * <ul>
     *   <li>{@code Unknown BooleanAttribute "name} (missing closing quote)</li>
     *   <li>{@code Unknown ValuedAttribute "name"}</li>
     * </ul>
     */
    public static String extractBadNameFromMessage(String message) {
        if (message == null) return null;
        int firstQuote = message.indexOf('"');
        int lastQuote = message.lastIndexOf('"');
        if (firstQuote >= 0 && lastQuote > firstQuote) {
            return message.substring(firstQuote + 1, lastQuote);
        }
        // Handle the case where the closing quote is missing (BooleanAttribute message)
        if (firstQuote >= 0 && lastQuote == firstQuote) {
            return message.substring(firstQuote + 1);
        }
        return null;
    }

    private static List<Suggestion> findClosestMatches(String badName, List<String> keywords) {
        List<Suggestion> candidates = new ArrayList<>();
        String lowerBadName = badName.toLowerCase();

        for (String keyword : keywords) {
            int distance = levenshteinDistance(lowerBadName, keyword.toLowerCase());
            if (distance <= MAX_DISTANCE && distance > 0) {
                candidates.add(new Suggestion(keyword, distance));
            }
        }

        candidates.sort(Comparator.comparingInt(Suggestion::getDistance));

        if (candidates.size() > MAX_SUGGESTIONS) {
            candidates = candidates.subList(0, MAX_SUGGESTIONS);
        }

        return candidates;
    }

    /**
     * Compute the Levenshtein distance between two strings.
     */
    public static int levenshteinDistance(String a, String b) {
        int lenA = a.length();
        int lenB = b.length();
        int[][] dp = new int[lenA + 1][lenB + 1];

        for (int i = 0; i <= lenA; i++) dp[i][0] = i;
        for (int j = 0; j <= lenB; j++) dp[0][j] = j;

        for (int i = 1; i <= lenA; i++) {
            for (int j = 1; j <= lenB; j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }

        return dp[lenA][lenB];
    }
}
