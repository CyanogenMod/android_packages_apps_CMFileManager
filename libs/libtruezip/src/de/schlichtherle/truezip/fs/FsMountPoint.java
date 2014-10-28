/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import static de.schlichtherle.truezip.fs.FsUriModifier.NULL;
import static de.schlichtherle.truezip.fs.FsUriModifier.PostFix.MOUNT_POINT;
import de.schlichtherle.truezip.util.QuotedUriSyntaxException;
import de.schlichtherle.truezip.util.UriBuilder;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Addresses the mount point of a file system.
 *
 * <a name="specification"/><h3>Specification</h3>
 * <p>
 * A mount point adds the following syntax constraints to a
 * {@link URI Uniform Resource Identifier}:
 * <ol>
 * <li>The URI must be absolute, that is it must define a scheme component.
 * <li>The URI must not define a query component.
 * <li>The URI must not define a fragment component.
 * <li>If the URI is opaque, its scheme specific part must end with the mount
 *     point separator {@code "!/"}.
 *     The scheme specific part <em>before</em> the mount point separator is
 *     parsed according the syntax constraints for a {@link FsPath} and the
 *     following additional syntax constraints:
 *     The path component must be absolute.
 *     If its opaque, it's entry name must not be empty.
 *     Finally, its set as the value of the {@link #getPath() path} component
 *     property.
 * <li>Otherwise, if the URI is hierarchical, its path component must be in
 *     normal form and end with a {@code "/"}.
 *     The {@link #getPath() path} component property of the mount point is set
 *     to {@code null} in this case.
 * </ol>
 *
 * <a name="examples"/><h3>Examples</h3>
 * <p>
 * Examples for valid mount point URIs are:
 * <table border=1 cellpadding=5 summary="">
 * <thead>
 * <tr>
 *   <th>{@link #toUri() uri} property</th>
 *   <th>{@link #getScheme() scheme} property</th>
 *   <th>{@link #getPath() path} URI</th>
 *   <th>{@link #getParent() parent} URI</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 *   <td>{@code foo:/bar/}</td>
 *   <td>{@code foo}</td>
 *   <td>(n/a)<sup>*</sup></td>
 *   <td>(n/a)<sup>*</sup></td>
 * </tr>
 * <tr>
 *   <td>{@code foo:bar:/baz!/}</td>
 *   <td>{@code foo}</td>
 *   <td>{@code bar:/baz}</td>
 *   <td>{@code bar:/}</td>
 * </tr>
 * <tr>
 *   <td>{@code foo:bar:baz:/bang!/boom!/}</td>
 *   <td>{@code foo}</td>
 *   <td>{@code bar:baz:/bang!/boom}</td>
 *   <td>{@code baz:/bang}</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * <sup>*</sup> the component property is {@code null} and hence its URI is not
 * available.
 * <p>
 * Examples for invalid mount point URIs are:
 * <table border=1 cellpadding=5 summary="">
 * <thead>
 * <tr>
 *   <th>URI</th>
 *   <th>Issue</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 *   <td>{@code /foo}</td>
 *   <td>not an absolute URI</td>
 * </tr>
 * <tr>
 *   <td>{@code foo:/bar}</td>
 *   <td>missing slash at end of hierarchical URI</td>
 * </tr>
 * <tr>
 *   <td>{@code foo:/bar/#baz}</td>
 *   <td>fragment component defined</td>
 * </tr>
 * <tr>
 *   <td>{@code foo:bar:/baz!/bang}</td>
 *   <td>missing mount point separator {@code "!/"} at end</td>
 * </tr>
 * <tr>
 *   <td>{@code foo:bar:baz:/bang!/!/}</td>
 *   <td>empty entry name in path component after mount point {@code "bar:baz:/bang!/"}</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * <a name="identities"/><h3>Identities</h3>
 * <p>
 * For any mount point {@code m}, it's generally true that
 * {@code new FsMountPoint(m.toUri()).equals(m)}.
 * <p>
 * For any mount point {@code m} with an opaque URI, it's generally true that
 * {@code new FsMountPoint(m.getScheme(), m.getPath()).equals(m)}.
 *
 * <a name="serialization"/><h3>Serialization</h3>
 * <p>
 * This class supports serialization with both
 * {@link java.io.ObjectOutputStream} and {@link java.beans.XMLEncoder}.
 *
 * @see     FsPath
 * @see     FsEntryName
 * @see     FsScheme
 * @author  Christian Schlichtherle
 */
public final class FsMountPoint implements Serializable, Comparable<FsMountPoint> {

    private static final long serialVersionUID = 5723957985634276648L;

    /**
     * The separator which is used to split opaque path names into
     * {@link FsMountPoint mount points} and {@link FsEntryName entry names}.
     * This is identical to the separator in the class
     * {@link java.net.JarURLConnection}.
     */
    public static final String SEPARATOR = "!" + FsEntryName.SEPARATOR;

    private URI uri; // not final for serialization only!

    private transient FsPath path;

    private transient volatile FsScheme scheme;

    private transient volatile URI hierarchical;

    /**
     * Equivalent to {@link #create(URI, FsUriModifier) create(uri, FsUriModifier.NULL)}.
     *
     * @deprecated This method does not quote characters with a special meaning
     *             in a URI - use the method variant with the URI parameter
     *             instead.
     */
    @Deprecated
    public static FsMountPoint
    create(String uri) {
        return create(uri, NULL);
    }

    /**
     * Constructs a new mount point by constructing a new URI from
     * the given string representation and parsing the result.
     * This static factory method calls
     * {@link #FsMountPoint(String, FsUriModifier) new FsMountPoint(uri, modifier)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the URI string representation.
     * @param  modifier the URI modifier.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for mount points.
     * @return A new mount point.
     * @deprecated This method does not quote characters with a special meaning
     *             in a URI - use the method variant with the URI parameter
     *             instead.
     */
    @Deprecated
    public static FsMountPoint
    create(String uri, FsUriModifier modifier) {
        try {
            return new FsMountPoint(uri, modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #create(URI, FsUriModifier) create(uri, FsUriModifier.NULL)}.
     */
    public static FsMountPoint
    create(URI uri) {
        return create(uri, NULL);
    }

    /**
     * Constructs a new mount point by parsing the given URI.
     * This static factory method calls
     * {@link #FsMountPoint(URI, FsUriModifier) new FsMountPoint(uri, modifier)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the {@link #toUri() URI}.
     * @param  modifier the URI modifier.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for mount points.
     * @return A new mount point.
     */
    public static FsMountPoint
    create(URI uri, FsUriModifier modifier) {
        try {
            return new FsMountPoint(uri, modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Constructs a new mount point by composing its URI from the given scheme
     * and path.
     * This static factory method calls
     * {@link #FsMountPoint(FsScheme, FsPath) new FsMountPoint(scheme, path)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  scheme the {@link #getScheme() scheme}.
     * @param  path the {@link #getPath() path}.
     * @throws IllegalArgumentException if the composed mount point URI would
     *         not conform to the syntax constraints for mount points.
     * @return A new mount point.
     */
    public static FsMountPoint
    create(FsScheme scheme, FsPath path) {
        try {
            return new FsMountPoint(scheme, path);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #FsMountPoint(String, FsUriModifier) new FsMountPoint(uri, FsUriModifier.NULL)}.
     *
     * @deprecated This constructor does not quote characters with a special
     *             meaning in a URI - use the constructor variant with the URI
     *             parameter instead.
     */
    @Deprecated
    public FsMountPoint(String uri) throws URISyntaxException {
        parse(new URI(uri), NULL);
    }

    /**
     * Constructs a new path by calling
     * {@link URI#URI(String) new URI(uri)} and parsing the resulting URI.
     *
     * @param  uri the URI string representation.
     * @param  modifier the URI modifier.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for mount points.
     * @deprecated This constructor does not quote characters with a special
     *             meaning in a URI - use the constructor variant with the URI
     *             parameter instead.
     */
    @Deprecated
    public FsMountPoint(String uri, FsUriModifier modifier)
    throws URISyntaxException {
        parse(new URI(uri), modifier);
    }

    /**
     * Equivalent to {@link #FsMountPoint(URI, FsUriModifier) new FsMountPoint(uri, FsUriModifier.NULL)}.
     */
    public FsMountPoint(URI uri) throws URISyntaxException {
        parse(uri, NULL);
    }

    /**
     * Constructs a new mount point by parsing the given URI.
     *
     * @param  uri the {@link #toUri() URI}.
     * @param  modifier the URI modifier.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for mount points.
     */
    public FsMountPoint(URI uri, FsUriModifier modifier)
    throws URISyntaxException {
        parse(uri, modifier);
    }

    /**
     * Constructs a new opaque mount point by composing its URI from the given
     * scheme and path.
     *
     * @param  scheme the non-{@code null} {@link #getScheme() scheme}.
     * @param  path the non-{@code null} {@link #getPath() path}.
     * @throws URISyntaxException if the composed mount point URI would not
     *         conform to the syntax constraints for mount points.
     */
    public FsMountPoint(final FsScheme scheme,
                        final FsPath path)
    throws URISyntaxException {
        final URI pu = path.toUri();
        if (!pu.isAbsolute())
            throw new QuotedUriSyntaxException(pu, "Path not absolute");
        final String penup = path.getEntryName().toUri().getPath();
        if (0 == penup.length())
            throw new QuotedUriSyntaxException(pu, "Empty entry name");
        this.uri = new UriBuilder(true)
                .scheme(scheme.toString())
                .path(new StringBuilder(pu.getScheme())
                    .append(':')
                    .append(pu.getRawSchemeSpecificPart())
                    .append(SEPARATOR)
                    .toString())
                .toUri();
        this.scheme = scheme;
        this.path = path;

        assert invariants();
    }

    private void writeObject(ObjectOutputStream out)
    throws IOException {
        out.writeObject(uri.toString());
    }

    private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        try {
            parse(new URI(in.readObject().toString()), NULL);
        } catch (URISyntaxException ex) {
            throw (InvalidObjectException) new InvalidObjectException(ex.toString())
                    .initCause(ex);
        }
    }

    private void parse(URI uri, final FsUriModifier modifier)
    throws URISyntaxException {
        uri = modifier.modify(uri, MOUNT_POINT);
        if (null != uri.getRawQuery())
            throw new QuotedUriSyntaxException(uri, "Query not allowed");
        if (null != uri.getRawFragment())
            throw new QuotedUriSyntaxException(uri, "Fragment not allowed");
        if (uri.isOpaque()) {
            final String ssp = uri.getRawSchemeSpecificPart();
            final int i = ssp.lastIndexOf(SEPARATOR);
            if (ssp.length() - 2 != i)
                throw new QuotedUriSyntaxException(uri,
                        "Doesn't end with mount point separator \"" + SEPARATOR + '"');
            path = new FsPath(new URI(ssp.substring(0, i)), modifier);
            final URI pu = path.toUri();
            if (!pu.isAbsolute())
                throw new QuotedUriSyntaxException(uri, "Path not absolute");
            if (0 == path.getEntryName().getPath().length())
                throw new QuotedUriSyntaxException(uri, "Empty URI path of entry name of path");
            if (NULL != modifier) {
                URI nuri = new UriBuilder(true)
                        .scheme(uri.getScheme())
                        .path(new StringBuilder(pu.getScheme())
                            .append(':')
                            .append(pu.getRawSchemeSpecificPart())
                            .append(SEPARATOR)
                            .toString())
                        .toUri();
                if (!uri.equals(nuri))
                    uri = nuri;
            }
        } else {
            if (!uri.isAbsolute())
                throw new QuotedUriSyntaxException(uri, "Not absolute");
            if (!uri.getRawPath().endsWith(FsEntryName.SEPARATOR))
                throw new QuotedUriSyntaxException(uri,
                        "URI path doesn't end with separator \"" + FsEntryName.SEPARATOR + '"');
            path = null;
        }
        this.uri = uri;

        assert invariants();
    }

    private boolean invariants() {
        assert null != toUri();
        assert toUri().isAbsolute();
        assert null == toUri().getRawQuery();
        assert null == toUri().getRawFragment();
        if (toUri().isOpaque()) {
            assert toUri().getRawSchemeSpecificPart().endsWith(SEPARATOR);
            assert null != getPath();
            assert getPath().toUri().isAbsolute();
            assert null == getPath().toUri().getRawFragment();
            assert 0 != getPath().getEntryName().toUri().getRawPath().length();
        } else {
            assert toUri().normalize() == toUri();
            assert toUri().getRawPath().endsWith(FsEntryName.SEPARATOR);
            assert null == getPath();
        }
        return true;
    }

    /**
     * Returns the URI for this mount point.
     *
     * @return The URI for this mount point.
     * @since  TrueZIP 7.1.1
     */
    public URI toUri() {
        return uri;
    }

    /**
     * @deprecated
     * @see #toUri()
     */
    @Deprecated
    public URI getUri() {
        return uri;
    }

    /**
     * Returns a URI which is recursively transformed from the URI of this
     * mount point so that it's absolute and hierarchical.
     * If this mount point is already in absolute and hierarchical form, its
     * URI gets returned.
     * <p>
     * For example, the mount point URIs {@code zip:file:/archive!/} and
     * {@code tar:file:/archive!/} would both produce the hierarchical URI
     * {@code file:/archive}.
     *
     * @return A URI which is recursively transformed from the URI of this
     *         mount point so that it's absolute and hierarchical.
     * @since  TrueZIP 7.1.1
     */
    public URI toHierarchicalUri() {
        final URI hierarchical = this.hierarchical;
        return null != hierarchical
                ? hierarchical
                : (this.hierarchical = uri.isOpaque()
                    ? path.toHierarchicalUri()
                    : uri);
    }

    /**
     * @deprecated
     * @see #toHierarchicalUri()()
     */
    @Deprecated
    public URI getHierarchicalUri() {
        return toHierarchicalUri();
    }

    /**
     * Returns the scheme component.
     *
     * @return The scheme component.
     */
    public FsScheme getScheme() {
        final FsScheme scheme = this.scheme;
        return null != scheme
                ? scheme
                : (this.scheme = FsScheme.create(uri.getScheme()));
    }

    /**
     * Returns the path component
     * or {@code null} iff this mount point's {@link #toUri URI} doesn't name
     * a parent mount point, that is if and only if it's hierarchical.
     *
     * @return The nullable path component.
     */
    public FsPath getPath() {
        return path;
    }

    /**
     * Returns the parent component, that is the mount point of the parent file
     * system,
     * or {@code null} iff this mount point's {@link #toUri URI} doesn't name
     * a parent mount point, that is if and only if it's hierarchical.
     *
     * @return The nullable parent component.
     */
    public FsMountPoint getParent() {
        assert null == path || null != path.getMountPoint();
        return null == path ? null : path.getMountPoint();
    }

    /**
     * Resolves the given entry name against this mount point.
     *
     * @param  entryName an entry name relative to this mount point.
     * @return A new path with an absolute URI.
     */
    public FsPath
    resolve(FsEntryName entryName) {
        return new FsPath(this, entryName);
    }

    /**
     * Returns {@code true} iff the given object is a mount point and its URI
     * {@link URI#equals(Object) equals} the URI of this mount point.
     * Note that this ignores the scheme and path.
     */
    @Override
    public boolean equals(Object that) {
        return this == that
                || that instanceof FsMountPoint
                    && this.uri.equals(((FsMountPoint) that).uri);
    }

    /**
     * Implements a natural ordering which is consistent with
     * {@link #equals(Object)}.
     */
    @Override
    public int compareTo(FsMountPoint that) {
        return this.uri.compareTo(that.uri);
    }

    /**
     * Returns a hash code which is consistent with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    /**
     * Equivalent to calling {@link URI#toString()} on {@link #toUri()}.
     */
    @Override
    public String toString() {
        return uri.toString();
    }
}