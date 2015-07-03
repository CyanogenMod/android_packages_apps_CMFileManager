/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.filemanager.util;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;

import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.Query;
import com.cyanogenmod.filemanager.model.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A helper class with useful methods for deal with search results.
 */
public final class SearchHelper {

    private static final String REGEXP_WILCARD = "*";  //$NON-NLS-1$
    private static final String REGEXP_WILCARD_JAVA = ".*";  //$NON-NLS-1$

    /**
     * Constructor of <code>SearchHelper</code>.
     */
    private SearchHelper() {
        super();
    }

    /**
     * Method that create a regular expression from a user query.
     *
     * @param query The query requested by the user
     * @param javaRegExp If returns a java regexp
     * @return String The regular expressions of the query to match an ignore case search
     */
    @SuppressWarnings("boxing")
    public static String toIgnoreCaseRegExp(final String query, boolean javaRegExp) {
        //Check that all is correct
        if (query == null || query.trim().length() == 0) {
            return "";  //$NON-NLS-1$
        }

        // If the regexp for java, then prepare the query
        String q = query;
        if (javaRegExp) {
            q = prepareQuery(q);
        }

        //Convert the string to lower and upper
        final String lowerCase = q.toLowerCase(Locale.ROOT);
        final String upperCase = q.toUpperCase(Locale.ROOT);

        //Create the regular expression filter
        StringBuffer sb = new StringBuffer();
        int cc = lowerCase.length();
        for (int i = 0; i < cc; i++) {
            char lower = lowerCase.charAt(i);
            char upper = upperCase.charAt(i);
            if (lower != upper) {
                //Convert to expression
                sb.append(String.format("[%s%s]", lower, upper)); //$NON-NLS-1$
            } else {
                //Not need to convert
                sb.append(lower);
            }
        }
        return String.format(
                    "%s%s%s",  //$NON-NLS-1$;
                    javaRegExp ? REGEXP_WILCARD_JAVA : REGEXP_WILCARD,
                    sb.toString(), javaRegExp ? REGEXP_WILCARD_JAVA : REGEXP_WILCARD);
    }

    /**
     * Method that cleans and prepares the query of the user to conform with a valid regexp.
     *
     * @param query The query requested by the user
     * @return String The prepared the query
     */
    private static String prepareQuery(String query) {
        StringBuilder sb = new StringBuilder(query.length());
        for (int i = 0; i < query.length(); ++i) {
            char ch = query.charAt(i);
            if (Character.isLetterOrDigit(ch) ||
                    ch == ' ' ||
                    ch == '\'') {
                sb.append(ch);
            } else if (ch == '*') {
                sb.append(".*"); //$NON-NLS-1$
            }
        }
        return sb.toString();
    }

    /**
     * Method that returns the name string highlighted with the match query.
     *
     * @param result The result to highlight
     * @param queries The list of queries that parameterized the search
     * @param highlightedColor The highlight color
     * @return CharSequence The name string highlighted
     */
    public static CharSequence getHighlightedName(
            SearchResult result, List<String> queries, int highlightedColor) {
        String name = result.getFso().getName();
        int cc = queries.size();
        for (int i = 0; i < cc; i++) {
            //Get the query removing wildcards
            String query =
                    queries.get(i)
                        .replace(".", "[.]") //$NON-NLS-1$//$NON-NLS-2$
                        .replace("*", ".*"); //$NON-NLS-1$//$NON-NLS-2$
            Pattern pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(name);
            Spannable span =  new SpannableString(name);
            if (matcher.find()) {
                //Highlight the match
                span.setSpan(
                        new BackgroundColorSpan(highlightedColor),
                        matcher.start(), matcher.end(), 0);
                span.setSpan(
                        new StyleSpan(Typeface.BOLD), matcher.start(), matcher.end(), 0);
                return span;
            }
        }

        // Something is wrong!!!. Name should be matched by some of the queries
        // No highlight terms
        return name;
    }

    /**
     * Method that returns the name but not highlight the search terms
     *
     * @param result The result to highlight
     * @return CharSequence The non highlighted name string
     */
    public static CharSequence getNonHighlightedName(SearchResult result) {
        String name = result.getFso().getName();
        Spannable span = new SpannableString(name);
        span.setSpan(new StyleSpan(Typeface.BOLD), 0, name.length(), 0);
        return span;
    }

    /**
     * Method that converts the list of file system object to a search result.
     *
     * @param files The files to convert
     * @param queries The terms of the search
     * @return List<SearchResult> The files converted
     */
    public static List<SearchResult> convertToResults(List<FileSystemObject> files, Query queries) {
        //Converts the list of files in a list of search results
        List<SearchResult> results = new ArrayList<SearchResult>(files.size());
        int cc = files.size();
        for (int i = 0; i < cc; i++) {
            FileSystemObject fso = files.get(i);
            results.add( convertToResult(fso, queries) );
        }
        return results;
    }

    /**
     * Method that converts a file system object to a search result.
     *
     * @param fso FileSystemObject that needs to be converted to a SearchResult
     * @param queries The terms of the search
     * @return SearchResult
     */
    public static SearchResult convertToResult(FileSystemObject fso, Query queries) {
        double relevance = calculateRelevance(fso, queries);
        return new SearchResult(relevance, fso);
    }

    /**
     * Method that calculates the relevance of a file system object for the terms
     * of a query.<br/>
     * <br/>
     * The algorithm is described as:<br/>
     * <br/>
     * By Name:<br/>
     * <ul>
     * <li>3 points if the term matches the name</li>
     * <li>2 points if the term starts or ends in the name</li>
     * <li>1 point if the term has other matches in the name</li>
     * </ul>
     * <br/>
     * By Accuracy:<br/>
     * <ul>
     * <li>3 points if the term is the more accuracy (1st term)</li>
     * <li>1 point if the term is the less accuracy (last term)</li>
     * <li>2 points in other cases</li>
     * <li></li>
     * </ul>
     * <br/>
     * <code>Relevance = By Name * By Accuracy</code>
     *
     * @param fso The file system object
     * @param queries The terms of the search
     * @return double A value from 1 to 10 where 10 has more relevance
     */
    public static double calculateRelevance(FileSystemObject fso, Query queries) {
        double relevance = 1.0;  //Minimum relevance (is in the result so has some relevance)
        List<String> terms = queries.getQueries();
        String name = fso.getName();
        int cc = terms.size();
        for (int i = 0; i < cc; i++) {
            String query =
                    terms.get(i)
                        .replace(".", "[.]") //$NON-NLS-1$//$NON-NLS-2$
                        .replace("*", ".*"); //$NON-NLS-1$//$NON-NLS-2$
            Pattern pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(name);
            if (matcher.find()) {
                //By name
                double byNameRelevance = 1.0;
                if (matcher.group().length() == name.length()) {
                    byNameRelevance = 3.0;
                } else if (name.startsWith(matcher.group()) || name.endsWith(matcher.group())) {
                    byNameRelevance = 2.0;
                }

                //By accuracy
                double byNameAccuracy = 1.0;
                if (i == 0) {
                    byNameAccuracy = 3.0;
                } else if (i != terms.size()) {
                    byNameAccuracy = 2.0;
                }

                //Calculate the relevance
                relevance += byNameRelevance * byNameAccuracy;
            }
        }
        return relevance;

    }

}
