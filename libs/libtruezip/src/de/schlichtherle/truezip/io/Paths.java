/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

import java.io.File;
import java.util.Locale;

/**
 * Static utility methods for path names.
 *
 * @author Christian Schlichtherle
 */
public final class Paths {

    /* Can't touch this - hammer time! */
    private Paths() { }

    /**
     * Equivalent to
     * <code>new {@link Normalizer#Paths.Normalizer(char) Normalizer(separatorChar)}&#x2e;{@link Normalizer#normalize(String) normalize(path)}</code>.
     */
    public static String normalize(String path, char separatorChar) {
        return new Normalizer(separatorChar).normalize(path);
    }

    /** A normalizer for path names. */
    public static class Normalizer {
        private final char separatorChar;
        private String path;
        private final StringBuilder buffer;

        public Normalizer(final char separatorChar) {
            this.separatorChar = separatorChar;
            buffer = new StringBuilder();
        }

        /**
         * Removes all redundant separators, dot directories ({@code "."}) and
         * dot-dot directories ({@code ".."}) from the given path name and
         * returns the result.
         * If present, a single trailing separator character is retained,
         * except after a dot-dot directory which couldn't get erased.
         * A resulting single dot-directory is truncated to an empty path.
         * <p>
         * On Windows, a path may be prefixed by a drive letter followed by a
         * colon.
         * On all platforms, a path may be prefixed by two leading separators
         * to indicate a UNC, although this is currently only supported on
         * Windows.
         *
         * @param  path the non-{@code null} path name to normalize.
         * @return {@code path} if it was already in normalized form.
         *         Otherwise, a new string with the normalized form of the
         *         given path name.
         */
        public String normalize(final String path) {
            final int prefixLen = prefixLength(path, separatorChar, false);
            final int pathLen = path.length();
            this.path = path.substring(prefixLen, pathLen);
            buffer.setLength(0);
            buffer.ensureCapacity(pathLen);
            normalize(0, pathLen - prefixLen);
            buffer.insert(0, path.substring(0, prefixLen));
            int bufferLen = buffer.length();
            String result;
            if (    pathLen > 0 && path.charAt(pathLen - 1) == separatorChar ||
                    pathLen > 1 && path.charAt(pathLen - 2) == separatorChar &&
                                   path.charAt(pathLen - 1) == '.') {
                slashify();
                bufferLen = buffer.length();
            }
            if (bufferLen == path.length()) {
                assert path.equals(buffer.toString());
                result = path;
            } else {
                result = buffer.toString();
                if (path.startsWith(result))
                    result = path.substring(0, bufferLen);
            }
            assert !result.equals(path) || result == path; // postcondition
            return result;
        }

        /**
         * This is a recursive call: The top level call should provide
         * {@code 0} as the {@code skip} parameter, the length
         * of the path as the {@code end} parameter and an empty string
         * buffer as the {@code result} parameter.
         *
         * @param  collapse the number of adjacent <i>dir/..</i> segments in
         *         the path to collapse.
         *         This value must not be negative.
         * @param  end the current position in {@code path}.
         *         Only the string to the left of this index is considered.
         *         If not positive, nothing happens.
         * @return The number of adjacent segments in the path which have
         *         <em>not</em> been collapsed at this position.
         */
        private int normalize(final int collapse, final int end) {
            assert collapse >= 0;
            if (0 >= end)
                return collapse;
            final int next = path.lastIndexOf(separatorChar, end - 1);
            final String base = path.substring(next + 1, end);
            int notCollapsed;
            if (0 >= base.length() || ".".equals(base)) {
                return normalize(collapse, next);
            } else if ("..".equals(base)) {
                notCollapsed = normalize(collapse + 1, next) - 1;
                if (0 > notCollapsed)
                    return 0;
            } else if (0 < collapse) {
                notCollapsed = normalize(collapse - 1, next);
                slashify();
                return notCollapsed;
            } else {
                assert 0 == collapse;
                notCollapsed = normalize(0, next);
                assert 0 == notCollapsed;
            }
            slashify();
            buffer.append(base);
            return notCollapsed;
        }

        private void slashify() {
            final int bufferLen = buffer.length();
            if (bufferLen > 0 && buffer.charAt(bufferLen - 1) != separatorChar)
                buffer.append(separatorChar);
        }
    } // class Normalizer

    /**
     * Cuts off any separator characters at the end of the given path name,
     * unless the path name contains of only separator characters, in which
     * case a single separator character is retained to denote the root
     * directory.
     *
     * @param  path The path name to chop.
     * @param  separatorChar The file name separator character.
     * @return {@code path} if it's a path name without trailing separators
     *         or contains the separator character only.
     *         Otherwise, the substring until the first of at least one
     *         separating characters is returned.
     */
    public static String cutTrailingSeparators(
            final String path,
            final char separatorChar) {
        int i = path.length();
        if (0 >= i || separatorChar != path.charAt(--i))
            return path;
        while (0 < i && separatorChar == path.charAt(--i)) {
        }
        return path.substring(0, ++i);
    }

    /**
     * Equivalent to
     * <code>new {@link Splitter#Paths.Splitter(char, boolean) Splitter(separatorChar, keepTrailingSeparator)}&#x2e;{@link Splitter#split(String) split(path)}</code>.
     */
    public static Splitter split(   String path,
                                    char separatorChar,
                                    boolean keepTrailingSeparator) {
        return new Splitter(separatorChar, keepTrailingSeparator).split(path);
    }

    /** Splits a given path name into its parent path name and member name. */
    public static class Splitter {
        private final char separatorChar;
        private final int fixum;
        private String parentPath;
        private String memberName;

        /**
         * Constructs a new splitter.
         *
         * @param separatorChar the file name separator character.
         * @param keepTrailingSeparator whether or not a parent path name
         *        should have a single trailing separator if present in the
         *        original path name.
         */
        public Splitter(final char separatorChar,
                        final boolean keepTrailingSeparator) {
            this.separatorChar = separatorChar;
            this.fixum = keepTrailingSeparator ? 1 : 0;
        }

        /**
         * Splits the given path name into its parent path name and member name,
         * recognizing platform specific file system roots.
         *
         * @param  path the path name which's parent path name and member name
         *         are to be returned.
         * @return {@code this}
         */
        public Splitter split(final String path) {
            final int prefixLen = prefixLength(path, separatorChar, false);
            int memberEnd = path.length() - 1;
            if (prefixLen > memberEnd) {
                parentPath = null;
                memberName = "";
                return this;
            }
            memberEnd = lastIndexNot(path, separatorChar, memberEnd);
            final int memberInd = path.lastIndexOf(separatorChar, memberEnd) + 1;
            memberEnd++;
            final int parentEnd;
            if (prefixLen >= memberEnd) {
                parentPath = null;
                memberName = "";
            } else if (prefixLen >= memberInd) {
                parentPath = 0 >= prefixLen ? null : path.substring(0, prefixLen);
                memberName = path.substring(prefixLen, memberEnd);
            } else if (prefixLen >= (parentEnd = lastIndexNot(path, separatorChar, memberInd - 1) + 1)) {
                parentPath = path.substring(0, prefixLen);
                memberName = path.substring(memberInd, memberEnd);
            } else {
                parentPath = path.substring(0, parentEnd + fixum);
                memberName = path.substring(memberInd, memberEnd);
            }
            return this;
        }

        private static int lastIndexNot(String path, char separatorChar, int last) {
            while (separatorChar == path.charAt(last) && --last >= 0) {
            }
            return last;
        }

        /** Returns the parent path name. */
        public String getParentPath() {
            return parentPath;
        }

        /** Returns the member name. */
        public String getMemberName() {
            return memberName;
        }
    } // class Splitter

    /**
     * Returns {@code true} iff the given path name refers to the root
     * directory, i.e. if it's empty.
     */
    public static boolean isRoot(String path) {
        return path.isEmpty();
    }

    /**
     * Returns {@code true} iff the given path name is absolute.
     * Windows drives and UNC's are recognized if and only if this JVM is
     * running on Windows.
     *
     * @param  path the path name to test.
     * @param  separatorChar the file name separator character.
     * @return Whether or not path is prefixed and the prefix ends with a
     *         separator character.
     */
    public static boolean isAbsolute(String path, char separatorChar) {
        final int prefixLen = prefixLength(path, separatorChar, false);
        return 0 < prefixLen && separatorChar == path.charAt(prefixLen - 1);
    }

    /**
     * Returns the length of the file system prefix in {@code path}.
     * File system prefixes are:
     * <ol>
     * <li>A letter followed by a colon and an optional separator.
     *     This indicates a Windows Drive and is only recognized if this
     *     JVM is running on Windows.
     * <li>Two leading separators.
     *     This indicates an authority and is only recognized if this JVM is
     *     running on Windows.
     *     If {@code inclUNC} is {@code true}, then the next two segments
     *     following the two leading separators are accounted for in the prefix
     *     length as the UNC host and share name.
     * <li>A single leading separator.
     *     On Windows and POSIX, this is the notation for an absolute path.
     *     This is recognized on any OS.
     * </ol>
     *
     * @param  path The file system path.
     * @param  separatorChar The file name separator character.
     * @param  inclUNC whether or not an authority or a UNC host and
     *         share name should get accounted for in the prefix length.
     * @return The number of characters in the prefix.
     */
    public static int prefixLength(
            final String path,
            final char separatorChar,
            final boolean inclUNC) {
        final int pathLen = path.length();
        if (pathLen <= 0)
            return 0;
        char c = path.charAt(0);
        if ('\\' == File.separatorChar) {
            if (2 <= pathLen
                    && ':' == path.charAt(1)
                    && ('a' <= c && c <= 'z' || 'A' <= c && c <= 'Z')) {
                    // Windows Drive.
                    return 3 <= pathLen && separatorChar == path.charAt(2) ? 3 : 2;
            } else if (separatorChar == c) {
                if (2 <= pathLen && separatorChar == path.charAt(1)) {
                    // Windows UNC.
                    if (!inclUNC)
                        return 2;
                    final int i = path.indexOf(separatorChar, 2) + 1;
                    if (0 == i)
                        return pathLen;
                    // UNC host name.
                    final int j = path.indexOf(separatorChar, i) + 1;
                    if (0 == j)
                        return pathLen;
                    // UNC share name.
                    return j;
                } else {
                    // Absolute path.
                    return 1;
                }
            } else {
                // Relative path.
                return 0;
            }
        } else {
            // Absolute or relative path.
            return separatorChar == c ? 1 : 0;
        }
    }

    /**
     * Returns true if and only if the path name represented by {@code a}
     * contains the path name represented by {@code b}.
     * If and only if this JVM is running on Windows, the comparison is case
     * insensitive with respect to the
     * {@link Locale#getDefault() default locale}.
     *
     * @param a A non-{@code null} {@link String} reference.
     * @param b A non-{@code null} {@link String} reference.
     * @param separatorChar The file name separator character.
     */
    public static boolean contains(String a, String b, char separatorChar) {
        // Windows is just case preserving, all others are case sensitive.
        if ('\\' == File.separatorChar) {
            a = a.toLowerCase(Locale.getDefault());
            b = b.toLowerCase(Locale.getDefault());
        }
        if (!b.startsWith(a)) return false;
        final int lengthA = a.length();
        final int lengthB = b.length();
        if (lengthA == lengthB) {
            return true;
        } else if (lengthA < lengthB) {
            return b.charAt(lengthA) == separatorChar;
        }
        return false;
    }
}
