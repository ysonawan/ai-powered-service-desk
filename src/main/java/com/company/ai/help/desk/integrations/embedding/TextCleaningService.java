package com.company.ai.help.desk.integrations.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Service for cleaning and normalizing text before embedding.
 * Handles HTML removal, whitespace normalization, special characters, etc.
 */
@Service
@Slf4j
public class TextCleaningService {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile("\\s+");
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[^\\p{L}\\p{N}\\s.,!?-]");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s]+");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w.-]+@[\\w.-]+\\.\\w+");
    private static final Pattern MARKDOWN_PATTERN = Pattern.compile("[*_`#\\-\\[\\]]");

    /**
     * Clean and normalize text for embedding
     *
     * @param text The raw text to clean
     * @return Cleaned and normalized text
     */
    public String cleanText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        // Step 1: Remove HTML tags
        String cleaned = removeHtmlTags(text);

        // Step 2: Remove URLs
        cleaned = removeUrls(cleaned);

        // Step 3: Remove email addresses (optional, uncomment if needed)
        // cleaned = removeEmails(cleaned);

        // Step 4: Remove Markdown formatting
        cleaned = removeMarkdown(cleaned);

        // Step 5: Remove special characters (keeping basic punctuation)
        cleaned = removeSpecialCharacters(cleaned);

        // Step 6: Normalize whitespace
        cleaned = normalizeWhitespace(cleaned);

        // Step 7: Convert to lowercase for consistency
        cleaned = cleaned.toLowerCase().trim();

        // Step 8: Remove leading/trailing punctuation
        cleaned = cleaned.replaceAll("^[.,!?\\-]+|[.,!?\\-]+$", "");

        return cleaned;
    }

    /**
     * Remove HTML tags from text
     */
    private String removeHtmlTags(String text) {
        return HTML_TAG_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Remove URLs from text
     */
    private String removeUrls(String text) {
        return URL_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Remove email addresses from text
     */
    private String removeEmails(String text) {
        return EMAIL_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Remove Markdown formatting
     */
    private String removeMarkdown(String text) {
        return MARKDOWN_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Remove special characters
     */
    private String removeSpecialCharacters(String text) {
        return SPECIAL_CHARS_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Normalize whitespace (multiple spaces to single space)
     */
    private String normalizeWhitespace(String text) {
        return MULTIPLE_SPACES_PATTERN.matcher(text).replaceAll(" ");
    }

    /**
     * Split text into chunks of specified size with overlap
     *
     * @param text The text to split
     * @param chunkSize Maximum characters per chunk
     * @param overlapSize Number of characters to overlap between chunks
     * @return Array of text chunks
     */
    public String[] splitIntoChunks(String text, int chunkSize, int overlapSize) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }

        // Clean text first
        String cleanedText = cleanText(text);

        if (cleanedText.length() <= chunkSize) {
            return new String[]{cleanedText};
        }

        java.util.List<String> chunks = new java.util.ArrayList<>();
        int start = 0;

        while (start < cleanedText.length()) {
            int end = Math.min(start + chunkSize, cleanedText.length());

            // Try to split at sentence boundary if possible
            int splitPoint = findSentenceBoundary(cleanedText, start, end);
            if (splitPoint > start && splitPoint < end) {
                end = splitPoint;
            }

            String chunk = cleanedText.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // Move to next chunk with overlap
            start = end - overlapSize;
            if (start < end) {
                start = end;
            }
        }

        return chunks.toArray(new String[0]);
    }

    /**
     * Find a sentence boundary (. ! ?) near the end position
     */
    private int findSentenceBoundary(String text, int start, int end) {
        for (int i = end - 1; i >= start; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?') {
                // Include the punctuation
                int boundaryPos = i + 1;
                // Skip any whitespace after punctuation
                while (boundaryPos < text.length() && Character.isWhitespace(text.charAt(boundaryPos))) {
                    boundaryPos++;
                }
                return boundaryPos;
            }
        }
        return -1; // No sentence boundary found
    }

    /**
     * Truncate text to a maximum length
     *
     * @param text The text to truncate
     * @param maxLength Maximum length
     * @return Truncated text
     */
    public String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength).trim();
    }

    /**
     * Check if text is valid for embedding
     *
     * @param text The text to validate
     * @param minLength Minimum required length
     * @param maxLength Maximum allowed length
     * @return true if text is valid
     */
    public boolean isValidForEmbedding(String text, int minLength, int maxLength) {
        if (text == null) {
            return false;
        }

        String cleaned = cleanText(text);
        int length = cleaned.length();

        return length >= minLength && length <= maxLength;
    }
}

