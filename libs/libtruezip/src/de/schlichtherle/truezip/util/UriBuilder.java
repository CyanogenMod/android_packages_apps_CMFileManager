/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.util;

import static de.schlichtherle.truezip.util.UriEncoder.Encoding.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;

/**
 * A mutable JavaBean for composing URIs according to
 * <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
 * and its updates in
 * <a href="http://www.ietf.org/rfc/rfc2732.txt">RFC&nbsp;2732</a>
 * for IPv6 addresses.
 * <p>
 * This class complements the immutable {@link URI} class by enabling its
 * clients to compose a URI from its components which can get read or written
 * as independent properties.
 * Each URI is composed of the five components {@link #getScheme() scheme},
 * {@link #getAuthority() authority}, {@link #getPath() path},
 * {@link #getQuery() query} and {@link #getFragment fragment}.
 * When done with setting the properties for the URI components, the resulting
 * URI can be composed by calling any of the methods {@link #getUri()},
 * {@link #toUri()}, {@link #getString()} or {@link #toString()}.
 * <p>
 * This class quotes illegal characters wherever required for the respective
 * URI component.
 * As a deviation from RFC&nbsp;2396, non-US-ASCII characters get preserved
 * when encoding.
 * <p>
 * Note that using this class is superior to the five argument URI constructor
 * <code>new {@link URI#URI(String, String, String, String, String) URI(scheme, authority, path, query, fragment)}</code>
 * because the URI constructor does not quote all paths correctly.
 * For example, {@code new URI(null, null, "foo:bar", null, null)} does not
 * quote the colon before parsing so the resulting URI will have a scheme
 * component {@code foo} and a path component {@code bar} instead of just a
 * path component {@code foo:bar}.
 *
 * <h3>Identities</h3>
 * For any {@link URI} {@code u} it is generally true that
 * <pre>{@code new UriBuilder(u).toUri().equals(u);}</pre>
 * and
 * <pre>{@code new UriBuilder().uri(u).toUri().equals(u);}</pre>
 * and
 * <pre>{@code
 * new UriBuilder()
 *     .scheme(u.getScheme())
 *     .authority(u.getAuthority())
 *     .path(u.isOpaque() ? u.getSchemeSpecificPart() : u.getPath())
 *     .query(u.getQuery())
 *     .fragment(u.getFragment())
 *     .toUri()
 *     .equals(u);
 * }</pre>
 * These identity productions apply for the method {@link #toUri()} as well as
 * the method {@link #getUri()}.
 *
 * @see    <a href="http://www.ietf.org/rfc/rfc2396.txt">
 *         RFC&nbsp;2396: Uniform Resource Identifiers (URI): Generic Syntax</a>
 * @see    <a href="http://www.ietf.org/rfc/rfc2732.txt">
 *         RFC&nbsp;2732: Format for Literal IPv6 Addresses in URL's</a>
 * @see    UriEncoder
 * @author Christian Schlichtherle
 */
public final class UriBuilder {

    private final UriEncoder encoder;
    private StringBuilder builder;
    private String scheme;
    private String authority;
    private String path;
    private String query;
    private String fragment;

    /**
     * Constructs a new URI builder.
     * Equivalent to {@link #UriBuilder(boolean) UriBuilder(false)}.
     */
    public UriBuilder() {
        this(false);
    }

    /**
     * Constructs a new URI builder.
     *
     * @param raw If {@code true}, then the {@code '%'} character doesn't get
     *        quoted.
     */
    public UriBuilder(boolean raw) {
        this.encoder = new UriEncoder(null, raw);
    }

    /**
     * Constructs a new URI builder.
     * Equivalent to {@link #UriBuilder(URI, boolean) UriBuilder(uri, false)}.
     *
     * @param uri the uri for initializing the initial state.
     */
    public UriBuilder(URI uri) {
        this(uri, false);
    }

    /**
     * Constructs a new URI builder.
     *
     * @param uri the uri for initializing the initial state.
     * @param raw If {@code true}, then the {@code '%'} character doesn't get
     *        quoted.
     */
    public UriBuilder(URI uri, boolean raw) {
        this.encoder = new UriEncoder(null, raw);
        setUri(uri); // OK - class is final!
    }

    /**
     * Clears the state of this URI builder.
     * Calling this method is effectively the same as setting all URI component
     * properties to {@code null}.
     *
     * @return {@code this}
     */
    public UriBuilder clear() {
        scheme = null;
        authority = null;
        path = null;
        query = null;
        fragment = null;
        return this;
    }

    /**
     * Returns a new URI string which conforms to the syntax constraints
     * defined in
     * <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
     * and its updates in
     * <a href="http://www.ietf.org/rfc/rfc2732.txt">RFC&nbsp;2732</a>
     * for IPv6 addresses.
     *
     * @return A valid URI string which is composed from the properties of
     *         this URI builder.
     * @throws IllegalStateException if composing a valid URI is not possible.
     * @see    #getString()
     */
    @Override
    public String toString() {
        try {
            return getString();
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Returns a new URI string which conforms to the syntax constraints
     * defined in
     * <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
     * and its updates in
     * <a href="http://www.ietf.org/rfc/rfc2732.txt">RFC&nbsp;2732</a>
     * for IPv6 addresses.
     *
     * @return A valid URI string which is composed from the properties of
     *         this URI builder.
     * @throws URISyntaxException if composing a valid URI is not possible due
     *         to an invalid scheme.
     * @see    #toString()
     */
    public String getString() throws URISyntaxException {
        final StringBuilder r = resetBuilder(); // result
        int errIdx = -1;                        // error index
        String errMsg = null;                   // error message
        final String    s = scheme,
                        a = authority, p = path, q = query,
                        f = fragment;
        final boolean absUri = null != s;
        if (absUri)
            r.append(s).append(':');
        final int ssp = r.length();             // index of scheme specific part
        final boolean hasAuth = null != a;
        if (hasAuth)
            encoder.encode(a, AUTHORITY, r.append("//"));
        boolean absPath = false;
        if (null != p && !p.isEmpty()) {
            if (p.startsWith("/")) {
                absPath = true;
                encoder.encode(p, ABSOLUTE_PATH, r);
            } else if (hasAuth) {
                absPath = true;
                errIdx = r.length();
                errMsg = "Relative path with " + (a.isEmpty() ? "" : "non-") + "empty authority";
                encoder.encode(p, ABSOLUTE_PATH, r);
            } else if (absUri) {
                encoder.encode(p, QUERY, r);
            } else {
                encoder.encode(p, PATH, r);
            }
        }
        if (null != q) {
            r.append('?');
            if (absUri && !absPath) {
                errIdx = r.length();
                errMsg = "Query in opaque URI";
            }
            encoder.encode(q, QUERY, r);
        }
        assert absUri == 0 < ssp;
        if (absUri && ssp >= r.length()){
            errIdx = r.length();
            errMsg = "Empty scheme specific part in absolute URI";
        }
        if (null != f)
            encoder.encode(f, FRAGMENT, r.append('#'));
        if (absUri)
            validateScheme((CharBuffer) CharBuffer.wrap(r).limit(s.length()));
        final String u = r.toString();
        if (0 <= errIdx)
            throw new QuotedUriSyntaxException(u, errMsg, errIdx);
        return u;
    }

    private StringBuilder resetBuilder() {
        StringBuilder builder = this.builder;
        if (null == builder)
            this.builder = builder = new StringBuilder();
        else
            builder.setLength(0);
        return builder;
    }

    /**
     * Checks the given string to conform to the syntax constraints for URI
     * schemes in
     * <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
     *
     * @param  scheme the string to validate.
     * @throws URISyntaxException if {@code scheme} does not conform to the
     *         syntax constraints for URI schemes in
     *         <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>.
     */
    public static void validateScheme(final String scheme)
    throws URISyntaxException {
        validateScheme(CharBuffer.wrap(scheme));
    }

    private static void validateScheme(final CharBuffer input)
    throws URISyntaxException {
        if (!input.hasRemaining())
            throw newURISyntaxException(input, "Empty URI scheme");
        char c = input.get();
        // TODO: Character class is no help here - consider table lookup!
        if ((c < 'a' || 'z' < c) && (c < 'A' || 'Z' < c))
            throw newURISyntaxException(input, "Illegal character in URI scheme");
        while (input.hasRemaining()) {
            c = input.get();
            if ((c < 'a' || 'z' < c) && (c < 'A' || 'Z' < c)
                    && (c < '0' || '9' < c)
                    && c != '+' && c != '-' && c != '.')
                throw newURISyntaxException(input, "Illegal character in URI scheme");
        }
    }

    private static URISyntaxException newURISyntaxException(CharBuffer input, String reason) {
        int p = input.position() - 1;
        return new QuotedUriSyntaxException(input.rewind().limit(input.capacity()), reason, p);
    }

    /**
     * Initializes all URI components from the given URI string.
     *
     * @param  uri the URI string.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints of the {@link URI} class.
     */
    public void setString(String uri) {
        setUri(URI.create(uri));
    }

    /**
     * Initializes all URI components from the given URI string.
     *
     * @param  uri the URI string.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints of the {@link URI} class.
     * @return {@code this}
     */
    public UriBuilder string(String uri) {
        setString(uri);
        return this;
    }

    /**
     * Returns a new URI which conforms to the syntax constraints
     * defined in
     * <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
     * and its updates in
     * <a href="http://www.ietf.org/rfc/rfc2732.txt">RFC&nbsp;2732</a>
     * for IPv6 addresses.
     *
     * @return A valid URI which is composed from the properties of
     *         this URI builder.
     * @throws IllegalStateException if composing a valid URI is not possible.
     * @see    #getUri()
     */
    public URI toUri() {
        try {
            return getUri();
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Returns a new URI which conforms to the syntax constraints
     * defined in
     * <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
     * and its updates in
     * <a href="http://www.ietf.org/rfc/rfc2732.txt">RFC&nbsp;2732</a>
     * for IPv6 addresses.
     *
     * @return A valid URI which is composed from the properties of
     *         this URI builder.
     * @throws URISyntaxException if composing a valid URI is not possible.
     * @see    #toUri()
     */
    public URI getUri() throws URISyntaxException {
        String u = getString();
        try {
            return new URI(u);
        } catch (URISyntaxException ex) {
            // See http://java.net/jira/browse/TRUEZIP-180
            throw new AssertionError(ex);
        }
    }

    /**
     * Initializes all URI components from the given URI.
     *
     * @param  uri the URI.
     */
    public void setUri(final URI uri) {
        if (encoder.isRaw()) {
            setScheme(uri.getScheme());
            setAuthority(uri.getRawAuthority());
            setPath(uri.isOpaque() ? uri.getRawSchemeSpecificPart() : uri.getRawPath());
            setQuery(uri.getRawQuery());
            setFragment(uri.getRawFragment());
        } else {
            setScheme(uri.getScheme());
            setAuthority(uri.getAuthority());
            setPath(uri.isOpaque() ? uri.getSchemeSpecificPart() : uri.getPath());
            setQuery(uri.getQuery());
            setFragment(uri.getFragment());
        }
    }

    /**
     * Initializes all URI components from the given URI.
     *
     * @param  uri the URI.
     * @return {@code this}
     */
    public UriBuilder uri(URI uri) {
        setUri(uri);
        return this;
    }

    /**
     * Returns the URI scheme component.
     *
     * @return The URI scheme component.
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * Sets the URI scheme component.
     *
     * @param  scheme the URI scheme component.
     */
    public void setScheme(final String scheme) {
        this.scheme = scheme;
    }

    /**
     * Sets the URI scheme component.
     *
     * @param  scheme the URI scheme component.
     * @return {@code this}
     */
    public UriBuilder scheme(String scheme) {
        setScheme(scheme);
        return this;
    }

    /**
     * Returns the URI authority component.
     * If this URI builder has been {@link #setUri(URI) initialized} from an
     * {@link URI#isOpaque() opaque} URI, then this property is {@code null}.
     *
     * @return The URI authority component.
     */
    public String getAuthority() {
        return authority;
    }

    /**
     * Sets the URI authority component.
     *
     * @param  authority the URI authority component.
     */
    public void setAuthority(final String authority) {
        this.authority = authority;
    }

    /**
     * Sets the URI authority component.
     *
     * @param  authority the URI authority component.
     * @return {@code this}
     */
    public UriBuilder authority(String authority) {
        setAuthority(authority);
        return this;
    }

    /**
     * Returns the URI path component.
     * If this URI builder has been {@link #setUri(URI) initialized} from an
     * {@link URI#isOpaque() opaque} URI, then this property contains the
     * scheme specific part of the URI.
     *
     * @return The URI path component.
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the URI path component.
     *
     * @param  path the URI path component.
     */
    public void setPath(final String path) {
        this.path = path;
    }

    /**
     * Sets the URI path component.
     *
     * @param  path the URI path component.
     * @return {@code this}
     */
    public UriBuilder path(String path) {
        setPath(path);
        return this;
    }

    /**
     * Returns the URI query component.
     * If this URI builder has been {@link #setUri(URI) initialized} from an
     * {@link URI#isOpaque() opaque} URI, then this property is {@code null}.
     *
     * @return The URI query component.
     */
    public String getQuery() {
        return query;
    }

    /**
     * Sets the URI query component.
     *
     * @param  query the URI query component.
     */
    public void setQuery(final String query) {
        this.query = query;
    }

    /**
     * Sets the URI query component.
     *
     * @param  query the URI query component.
     * @return {@code this}
     */
    public UriBuilder query(String query) {
        setQuery(query);
        return this;
    }

    /**
     * Sets the URI path an query components by splitting the given string at
     * the first occurence of the query separator {@code '?'}.
     *
     * @param  pathQuery the combined URI path and query components.
     * @since  TrueZIP 7.4.2
     */
    public void setPathQuery(final String pathQuery) {
        final int i;
        if (null != pathQuery && 0 <= (i = pathQuery.indexOf('?'))) {
            this.path = pathQuery.substring(0, i);
            this.query = pathQuery.substring(i + 1);
        } else {
            this.path = pathQuery;
            this.query = null;
        }
    }

    /**
     * Sets the URI path an query components by splitting the given string at
     * the first occurence of the query separator {@code '?'}.
     *
     * @param  pathQuery the combined URI path and query components.
     * @return {@code this}
     * @since  TrueZIP 7.4.2
     */
    public UriBuilder pathQuery(String pathQuery) {
        setPathQuery(pathQuery);
        return this;
    }

    /**
     * Returns the URI fragment component.
     *
     * @return The URI fragment component.
     */
    public String getFragment() {
        return fragment;
    }

    /**
     * Sets the URI fragment component.
     *
     * @param  fragment the URI fragment component.
     */
    public void setFragment(final String fragment) {
        this.fragment = fragment;
    }
   
    /**
     * Sets the URI fragment component.
     *
     * @param  fragment the URI fragment component.
     * @return {@code this}
     */
    public UriBuilder fragment(String fragment) {
        setFragment(fragment);
        return this;
    }
}
