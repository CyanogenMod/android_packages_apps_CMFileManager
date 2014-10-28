/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.EntryName;
import static de.schlichtherle.truezip.fs.FsUriModifier.NULL;
import static de.schlichtherle.truezip.fs.FsUriModifier.PostFix.ENTRY_NAME;
import de.schlichtherle.truezip.util.QuotedUriSyntaxException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Addresses an entry in a file system relative to its
 * {@link FsMountPoint mount point}.
 *
 * <a name="specification"/><h3>Specification</h3>
 * <p>
 * An entry name adds the following syntax constraints to a
 * {@link URI Uniform Resource Identifier}:
 * <ol>
 * <li>The URI must be relative, that is it must not define a scheme component.
 * <li>The URI must not define an authority component.
 * <li>The URI must define a path component.
 * <li>The URI's path must be in normal form, that is its path component must
 *     not contain redundant {@code "."} and {@code ".."} segments.
 * <li>The URI's path component must not equal {@code "."}.
 * <li>The URI's path component must not equal {@code ".."}.
 * <li>The URI's path component must not start with {@code "/"}.
 * <li>The URI's path component must not start with {@code "./"}
 *     (this rule is actually redundant - see #3).
 * <li>The URI's path component must not start with {@code "../"}.
 * <li>The URI's path component must not end with {@code "/"}.
 * <li>The URI must not define a fragment component.
 * </ol>
 *
 * <a name="examples"/><h3>Examples</h3>
 * <p>
 * Examples for valid entry name URIs are:
 * <ul>
 * <li>{@code "foo"}
 * <li>{@code "foo/bar"}
 * </ul>
 * <table border=1 cellpadding=5 summary="">
 * <thead>
 * <tr>
 *   <th>{@link #toUri() uri} property</th>
 *   <th>{@link #getPath() path} property</th>
 *   <th>{@link #getQuery() query} property</th>
 *   <th>{@link #getFragment() fragment} property</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 *   <td>{@code foo}</td>
 *   <td>{@code foo}</td>
 *   <td>(null)</td>
 *   <td>(null)</td>
 * </tr>
 * <tr>
 *   <td>{@code foo/bar}</td>
 *   <td>{@code foo/bar}</td>
 *   <td>(null)</td>
 *   <td>(null)</td>
 * </tr>
 * <tr>
 *   <td>{@code foo?bar}</td>
 *   <td>{@code foo}</td>
 *   <td>{@code bar}</td>
 *   <td>(null)</td>
 * </tr>
 * <tr>
 *   <td>{@code foo#bar}</td>
 *   <td>{@code foo}</td>
 *   <td>(null)</td>
 *   <td>{@code bar}</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * Examples for invalid entry name URIs are:
 * <table border=1 cellpadding=5 summary="">
 * <thead>
 * <tr>
 *   <th>URI</th>
 *   <th>Issue</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 *   <td>{@code foo:/bar}</td>
 *   <td>not a relative URI</td>
 * </tr>
 * <tr>
 *   <td>{@code //foo/bar}</td>
 *   <td>authority component defined</td>
 * </tr>
 * <tr>
 *   <td>{@code /foo}</td>
 *   <td>leading slash not allowed</td>
 * </tr>
 * <tr>
 *   <td>{@code foo/}</td>
 *   <td>trailing slash not allowed</td>
 * </tr>
 * <tr>
 *   <td>{@code foo/.}</td>
 *   <td>not a normalized URI</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * <a name="identities"/><h3>Identities</h3>
 * <p>
 * For any entry name {@code e}, it's generally true that
 * {@code new FsEntryName(e.toUri()).equals(e)}.
 *
 * <a name="serialization"/><h3>Serialization</h3>
 * <p>
 * This class supports serialization with both
 * {@link java.io.ObjectOutputStream} and {@link java.beans.XMLEncoder}.
 *
 * @see     FsPath
 * @see     FsMountPoint
 * @see     FsScheme
 * @see     FsEntry#getName()
 * @author  Christian Schlichtherle
 */
public final class FsEntryName extends EntryName {
    private static final long serialVersionUID = 2212342253466752478L;

    private static final String ILLEGAL_PREFIX = ".." + SEPARATOR;

    /**
     * The file system entry name of the root directory,
     * which is an empty URI.
     */
    public static final FsEntryName ROOT;
    static {
        try {
            ROOT = new FsEntryName(new URI(""));
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Equivalent to {@link #create(String, FsUriModifier) create(uri, FsUriModifier.NULL)}.
     *
     * @deprecated This method does not quote characters with a special meaning
     *             in a URI - use the method variant with the URI parameter
     *             instead.
     */
    @Deprecated
    public static FsEntryName
    create(String uri) {
        return create(uri, NULL);
    }

    /**
     * Constructs a new file system entry name by constructing a new URI from
     * the given string representation and parsing the result.
     * This static factory method calls
     * {@link #FsEntryName(String, FsUriModifier) new FsEntryName(uri, modifier)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the URI string representation.
     * @param  modifier the URI modifier.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for entry names.
     * @return A new file system entry name.
     * @deprecated This method does not quote characters with a special meaning
     *             in a URI - use the method variant with the URI parameter
     *             instead.
     */
    @Deprecated
    public static FsEntryName
    create(String uri, FsUriModifier modifier) {
        try {
            return uri.isEmpty()
                    ? ROOT
                    : new FsEntryName(uri, modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /** Equivalent to {@link #create(URI, FsUriModifier) create(uri, FsUriModifier.NULL)}. */
    public static FsEntryName
    create(URI uri) {
        return create(uri, NULL);
    }

    /**
     * Constructs a new file system entry name by parsing the given URI.
     * This static factory method calls
     * {@link #FsEntryName(URI, FsUriModifier) new FsEntryName(uri, modifier)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the {@link #toUri() URI}.
     * @param  modifier the URI modifier.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for entry names.
     * @return A new file system entry name.
     */
    public static FsEntryName
    create(URI uri, FsUriModifier modifier) {
        try {
            return uri.toString().isEmpty()
                    ? ROOT
                    : new FsEntryName(uri, modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #FsEntryName(String, FsUriModifier) new FsEntryName(uri, FsUriModifier.NULL)}.
     *
     * @deprecated This constructor does not quote characters with a special
     *             meaning in a URI - use the constructor variant with the URI
     *             parameter instead.
     */
    @Deprecated
    public FsEntryName(String uri) throws URISyntaxException {
        this(uri, NULL);
    }

    /**
     * Constructs a new file system entry name by calling
     * {@link URI#URI(String) new URI(uri)} and parsing the resulting URI.
     *
     * @param  uri the URI string representation.
     * @param  modifier the URI modifier.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for entry names.
     * @deprecated This constructor does not quote characters with a special
     *             meaning in a URI - use the constructor variant with the URI
     *             parameter instead.
     */
    @Deprecated
    public FsEntryName(String uri, FsUriModifier modifier)
    throws URISyntaxException {
        this(new URI(uri), modifier);
    }

    /**
     * Equivalent to {@link #FsEntryName(URI, FsUriModifier) new FsEntryName(uri, FsUriModifier.NULL)}.
     */
    public FsEntryName(URI uri) throws URISyntaxException {
        this(uri, NULL);
    }

    /**
     * Constructs a new file system entry name by parsing the given URI.
     *
     * @param  uri the {@link #toUri() URI}.
     * @param  modifier the URI modifier.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for file system entry names.
     */
    public FsEntryName(URI uri, final FsUriModifier modifier)
    throws URISyntaxException {
        super(uri = modifier.modify(uri, ENTRY_NAME));
        parse(uri);
    }

    private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        try {
            parse(toUri()); // protect against manipulation
        } catch (URISyntaxException ex) {
            throw (InvalidObjectException) new InvalidObjectException(ex.toString())
                    .initCause(ex);
        }
    }

    private void parse(final URI uri) throws URISyntaxException {
        final String p = uri.getRawPath();
        if (p.startsWith(SEPARATOR))
            throw new QuotedUriSyntaxException(uri,
                    "Illegal start of URI path component");
        if (!p.isEmpty() && ILLEGAL_PREFIX.startsWith(p.substring(0,
                Math.min(p.length(), ILLEGAL_PREFIX.length()))))
            throw new QuotedUriSyntaxException(uri,
                    "Illegal start of URI path component");
        if (p.endsWith(SEPARATOR))
            throw new QuotedUriSyntaxException(uri,
                    "Illegal separator \"" + SEPARATOR + "\" at end of URI path");

        assert invariants();
    }

    /**
     * Constructs a new file system entry name by resolving the given member
     * file system entry name against the given parent file system entry name.
     * Note that the URI of the parent file system entry name is always
     * considered to name a directory, so calling this constructor with
     * {@code "foo"} and {@code "bar"} as the URIs for the parent and member
     * file system entry names results in {@code "foo/bar"} as the file system
     * entry name URI.
     *
     * @param  parent an entry name for the parent.
     * @param  member an entry name for the member.
     */
    public FsEntryName( final FsEntryName parent,
                        final FsEntryName member) {
        super(parent, member);

        assert invariants();
    }

    private boolean invariants() {
        assert null != toUri();
        //assert !toUri().isAbsolute();
        //assert null == toUri().getRawAuthority();
        //assert null == toUri().getRawFragment();
        assert toUri().normalize() == toUri();
        String p = toUri().getRawPath();
        assert !"..".equals(p);
        assert !p.startsWith(SEPARATOR);
        assert !p.startsWith("." + SEPARATOR);
        assert !p.startsWith(".." + SEPARATOR);
        assert !p.endsWith(SEPARATOR);
        return true;
    }

    /**
     * Returns {@code true} if and only if the path component of this file
     *         system entry name is empty and no query component is defined.
     *
     * @return {@code true} if and only if the path component of this file
     *         system entry name is empty and no query component is defined.
     */
    public boolean isRoot() {
        //return toUri().toString().isEmpty();
        final URI uri = toUri();
        final String path = uri.getRawPath();
        if (null != path && !path.isEmpty())
            return false;
        final String query = uri.getRawQuery();
        if (null != query)
            return false;
        return true;
    }
}