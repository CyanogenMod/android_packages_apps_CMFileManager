/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import static de.schlichtherle.truezip.fs.FsUriModifier.CANONICALIZE;
import static de.schlichtherle.truezip.fs.FsUriModifier.NULL;
import static de.schlichtherle.truezip.fs.FsUriModifier.PostFix.PATH;
import de.schlichtherle.truezip.util.QuotedUriSyntaxException;
import de.schlichtherle.truezip.util.UriBuilder;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Addresses an entry in a file system.
 * The purpose of a file system path is to parse a {@link URI} and decompose it
 * into a {@link #getMountPoint() mount point} of a file system and its
 * relative {@link #getEntryName() entry name}.
 *
 * <a name="specification"/><h3>Specification</h3>
 * <p>
 * A path adds the following syntax constraints to a
 * {@link URI Uniform Resource Identifier}:
 * <ol>
 * <li>The URI must not define a fragment component.
 * <li>If the URI is opaque, its scheme specific part must contain at least
 *     one mount point separator {@code "!/"}.
 *     The part <em>up to</em> the last mount point separator is parsed
 *     according to the syntax constraints for an {@link FsMountPoint} and set
 *     as the value of the {@link #getMountPoint() mountPoint} property.
 *     The part <em>after</em> the last mount point separator is parsed
 *     according to the syntax constraints for an {@link FsEntryName} and set
 *     as the value of the {@link #getEntryName() entryName} property.
 * <li>Otherwise, if the URI is absolute, it's resolved with {@code "."},
 *     parsed according to the syntax constraints for an {@link FsMountPoint}
 *     and set as the value of the {@link #getMountPoint() mountPoint} property.
 *     The URI relativized to this mount point is parsed according to the
 *     syntax constraints for an {@link FsEntryName} and set as the value of
 *     the {@link #getEntryName() entryName} property.
 * <li>Otherwise, the value of the {@link #getMountPoint() mountPoint} property
 *     is set to {@code null} and the URI is parsed according to the syntax
 *     constraints for an {@link FsEntryName} and set as the value of the
 *     {@link #getEntryName() entryName} property.
 * </ol>
 * For opaque URIs of the form {@code jar:<url>!/<entry>}, these constraints
 * build a close subset of the syntax allowed by a
 * {@link java.net.JarURLConnection}.
 *
 * <a name="examples"/><h3>Examples</h3>
 * <p>
 * Examples for valid path URIs are:
 * <table border=1 cellpadding=5 summary="">
 * <thead>
 * <tr>
 *   <th>{@link #toUri() uri} property</th>
 *   <th>{@link #getMountPoint() mountPoint} URI</th>
 *   <th>{@link #getEntryName() entryName} URI</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 *   <td>{@code foo}</td>
 *   <td>(null)</td>
 *   <td>{@code foo}</td>
 * </tr>
 * <tr>
 *   <td>{@code foo:/bar}</td>
 *   <td>{@code foo:/}</td>
 *   <td>{@code bar}</td>
 * </tr>
 * <tr>
 *   <td>{@code foo:/bar/}</td>
 *   <td>{@code foo:/bar}</td>
 *   <td>(empty - not null)</td>
 * </tr>
 * <tr>
 *   <td>{@code foo:bar:/baz!/bang}</td>
 *   <td>{@code foo:bar:/baz!/}</td>
 *   <td>{@code bang}</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * Examples for invalid path URIs are:
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
 *   <td>leading slash separator not allowed if URI is not absolute</td>
 * </tr>
 * <tr>
 *   <td>{@code foo/}</td>
 *   <td>trailing slash separator not allowed if URI is not absolute</td>
 * </tr>
 * <tr>
 *   <td>{@code foo:bar}</td>
 *   <td>missing mount point separator in opaque URI</td>
 * </tr>
 * <tr>
 *   <td>{@code foo:bar:baz:/bang!/boom}</td>
 *   <td>dito for {@code bar:baz:/bang}</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * <a name="identities"/><h3>Identities</h3>
 * <p>
 * For any path {@code p}, it's generally true that
 * {@code new FsPath(p.toUri()).equals(p)}.
 * <p>
 * Furthermore, it's generally true that
 * {@code new FsPath(p.getMountPoint(), p.getEntryName()).equals(p)}.
 *
 * <a name="serialization"/><h3>Serialization</h3>
 * <p>
 * This class supports serialization with both
 * {@link java.io.ObjectOutputStream} and {@link java.beans.XMLEncoder}.
 *
 * @see     FsMountPoint
 * @see     FsEntryName
 * @see     FsScheme
 * @author  Christian Schlichtherle
 */
public final class FsPath implements Serializable, Comparable<FsPath> {

    private static final long serialVersionUID = 5798435461242930648L;

    private static final URI DOT = URI.create(".");

    private URI uri; // not final for serialization only!

    private transient FsMountPoint mountPoint;

    private transient FsEntryName entryName;

    private transient volatile URI hierarchical;

    /**
     * Equivalent to {@link #create(String, FsUriModifier) create(uri, FsUriModifier.NULL)}.
     *
     * @deprecated This method does not quote characters with a special meaning
     *             in a URI - use the method variant with the URI parameter
     *             instead.
     */
    @Deprecated
    public static FsPath
    create(String uri) {
        return create(uri, NULL);
    }

    /**
     * Constructs a new path by constructing a new URI from
     * the given string representation and parsing the result.
     * This static factory method calls
     * {@link #FsPath(String, FsUriModifier) new FsPath(uri, modifier)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the URI string representation.
     * @param  modifier the URI modifier.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for paths.
     * @return A new path.
     * @deprecated This method does not quote characters with a special meaning
     *             in a URI - use the method variant with the URI parameter
     *             instead.
     */
    @Deprecated
    public static FsPath
    create(String uri, FsUriModifier modifier) {
        try {
            return new FsPath(uri, modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #create(URI, FsUriModifier) create(uri, FsUriModifier.NULL)}.
     */
    public static FsPath
    create(URI uri) {
        return create(uri, NULL);
    }

    /**
     * Constructs a new path by parsing the given URI.
     * This static factory method calls
     * {@link #FsPath(URI, FsUriModifier) new FsPath(uri, modifier)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  uri the {@link #toUri() URI}.
     * @param  modifier the URI modifier.
     * @throws IllegalArgumentException if {@code uri} does not conform to the
     *         syntax constraints for paths.
     * @return A new path.
     */
    public static FsPath
    create(URI uri, FsUriModifier modifier) {
        try {
            return new FsPath(uri, modifier);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Equivalent to {@link #FsPath(String, FsUriModifier) new FsPath(uri, FsUriModifier.NULL)}.
     *
     * @deprecated This constructor does not quote characters with a special
     *             meaning in a URI - use the constructor variant with the URI
     *             parameter instead.
     */
    @Deprecated
    public FsPath(String uri) throws URISyntaxException {
        parse(new URI(uri), NULL);
    }

    /**
     * Constructs a new path by calling {@link URI#URI(String) new URI(uri)}
     * and parsing the resulting URI.
     *
     * @param  uri the URI string representation.
     * @param  modifier the URI modifier.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for paths.
     * @deprecated This constructor does not quote characters with a special
     *             meaning in a URI - use the constructor variant with the URI
     *             parameter instead.
     */
    @Deprecated
    public FsPath(String uri, FsUriModifier modifier)
    throws URISyntaxException {
        parse(new URI(uri), modifier);
    }

    /**
     * Equivalent to {@link #FsPath(URI, FsUriModifier) new FsPath(file.toURI(), FsUriModifier.CANONICALIZE)}.
     * Note that this constructor is expected not to throw any exceptions.
     */
    public FsPath(File file) {
        try {
            parse(file.toURI(), CANONICALIZE);
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Equivalent to {@link #FsPath(URI, FsUriModifier) new FsPath(uri, FsUriModifier.NULL)}.
     */
    public FsPath(URI uri) throws URISyntaxException {
        parse(uri, NULL);
    }

    /**
     * Constructs a new path by parsing the given URI.
     *
     * @param  uri the non-{@code null} {@link #toUri() URI}.
     * @param  modifier the URI modifier.
     * @throws URISyntaxException if {@code uri} does not conform to the
     *         syntax constraints for paths.
     */
    public FsPath(URI uri, FsUriModifier modifier)
    throws URISyntaxException {
        parse(uri, modifier);
    }

    /**
     * Constructs a new path by composing its URI from the given nullable mount
     * point and entry name.
     *
     * @param  mountPoint the nullable {@link #getMountPoint() mount point}.
     * @param  entryName the {@link #getEntryName() entry name}.
     * @throws URISyntaxException if the composed path URI would not conform
     *         to the syntax constraints for paths.
     */
    public FsPath(  final FsMountPoint mountPoint,
                    final FsEntryName entryName) {
        URI mpu;
        if (null == mountPoint) {
            this.uri = entryName.toUri();
        } else if (entryName.isRoot()) {
            this.uri = mountPoint.toUri();
        } else if ((mpu = mountPoint.toUri()).isOpaque()) {
            try {
                // Compute mountPoint + entryName, but ensure that all URI
                // components are properly quoted.
                final String mpussp = mpu.getRawSchemeSpecificPart();
                final int mpusspl = mpussp.length();
                final URI enu = entryName.toUri();
                final String enup = enu.getRawPath();
                final int enupl = enup.length();
                final String enuq = enu.getRawQuery();
                final int enuql = null == enuq ? 0 : enuq.length() + 1;
                final StringBuilder ssp =
                        new StringBuilder(mpusspl + enupl + enuql)
                        .append(mpussp)
                        .append(enup);
                if (null != enuq)
                    ssp.append('?').append(enuq);
                this.uri = new UriBuilder(true)
                        .scheme(mpu.getScheme())
                        .path(ssp.toString())
                        .fragment(enu.getRawFragment())
                        .getUri();
            } catch (URISyntaxException ex) {
                throw new AssertionError(ex);
            }
        } else {
            this.uri = mpu.resolve(entryName.toUri());
        }
        this.mountPoint = mountPoint;
        this.entryName = entryName;

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
        uri = modifier.modify(uri, PATH);
        if (null != uri.getRawFragment())
            throw new QuotedUriSyntaxException(uri, "Fragment not allowed");
        if (uri.isOpaque()) {
            final String ssp = uri.getRawSchemeSpecificPart();
            final int i = ssp.lastIndexOf(FsMountPoint.SEPARATOR);
            if (0 > i)
                throw new QuotedUriSyntaxException(uri,
                        "Missing mount point separator \"" + FsMountPoint.SEPARATOR + '"');
            final UriBuilder b = new UriBuilder(true);
            mountPoint = new FsMountPoint(
                    b.scheme(uri.getScheme())
                     .path(ssp.substring(0, i + 2))
                     .toUri(),
                    modifier);
            entryName = new FsEntryName(
                    b.clear()
                     .pathQuery(ssp.substring(i + 2))
                     .fragment(uri.getRawFragment())
                     .toUri(),
                    modifier);
            if (NULL != modifier) {
                URI mpu = mountPoint.toUri();
                URI nuri = new URI(mpu.getScheme() + ':' + mpu.getRawSchemeSpecificPart() + entryName.toUri());
                if (!uri.equals(nuri))
                    uri = nuri;
            }
        } else if (uri.isAbsolute()) {
            mountPoint = new FsMountPoint(uri.resolve(DOT), modifier);
            entryName = new FsEntryName(mountPoint.toUri().relativize(uri), modifier);
        } else {
            mountPoint = null;
            entryName = new FsEntryName(uri, modifier);
            if (NULL != modifier)
                uri = entryName.toUri();
        }
        this.uri = uri;

        assert invariants();
    }

    private boolean invariants() {
        assert null != toUri();
        assert null == toUri().getRawFragment();
        assert (null != getMountPoint()) == toUri().isAbsolute();
        assert null != getEntryName();
        if (toUri().isOpaque()) {
            assert toUri().getRawSchemeSpecificPart().contains(FsMountPoint.SEPARATOR);
            /*try {
                assert toUri().equals(new URI(getMountPoint().toUri().getScheme(), getMountPoint().toUri().getSchemeSpecificPart() + toDecodedUri(getEntryName()), null));
            } catch (URISyntaxException ex) {
                throw new AssertionError(ex);
            }*/
        } else if (toUri().isAbsolute()) {
            assert toUri().normalize() == toUri();
            assert toUri().equals(getMountPoint().toUri().resolve(getEntryName().toUri()));
        } else {
            assert toUri().normalize() == toUri();
            assert getEntryName().toUri() == toUri();
        }
        return true;
    }

    /**
     * Returns the URI for this path.
     *
     * @return The URI for this path.
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
     * path so that it's absolute and hierarchical.
     * If this path is already in absolute and hierarchical form, its URI gets
     * returned.
     * <p>
     * For example, the path URIs {@code zip:file:/archive!/entry} and
     * {@code tar:file:/archive!/entry} would both produce the hierarchical URI
     * {@code file:/archive/entry}.
     *
     * @return A URI which is recursively transformed from the URI of this
     *         path so that it's absolute and hierarchical.
     * @since  TrueZIP 7.1.1
     */
    public URI toHierarchicalUri() {
        final URI hierarchical = this.hierarchical;
        if (null != hierarchical)
            return hierarchical;
        if (uri.isOpaque()) {
            final URI mpu = mountPoint.toHierarchicalUri();
            final URI enu = entryName.toUri();
            try {
                return this.hierarchical = enu.toString().isEmpty()
                        ? mpu
                        : new UriBuilder(mpu, true)
                            .path(mpu.getRawPath() + FsEntryName.SEPARATOR)
                            .getUri()
                            .resolve(enu);
            } catch (URISyntaxException ex) {
                throw new AssertionError(ex);
            }
        } else {
            return this.hierarchical = uri;
        }
    }

    /**
     * @deprecated
     * @see #toHierarchicalUri()
     */
    @Deprecated
    public URI getHierarchicalUri() {
        return toHierarchicalUri();
    }

    /**
     * Returns the mount point component or {@code null} iff this path's
     * {@link #toUri() URI} is not absolute.
     *
     * @return The nullable mount point.
     */
    public FsMountPoint getMountPoint() {
        return mountPoint;
    }

    /**
     * Returns the entry name component.
     * This may be empty, but is never {@code null}.
     *
     * @return The entry name.
     */
    public FsEntryName getEntryName() {
        return entryName;
    }

    /**
     * Resolves the given entry name against this path.
     *
     * @param  entryName an entry name relative to this path.
     * @return A new path with an absolute URI.
     */
    public FsPath
    resolve(final FsEntryName entryName) {
        if (entryName.isRoot() && null == this.uri.getQuery())
            return this;
        return new FsPath(
                this.mountPoint,
                new FsEntryName(this.entryName, entryName));
    }

    /**
     * Returns {@code true} iff the given object is a path name and its URI
     * {@link URI#equals(Object) equals} the URI of this path name.
     * Note that this ignores the mount point and entry name.
     */
    @Override
    public boolean equals(Object that) {
        return this == that
                || that instanceof FsPath
                    && this.uri.equals(((FsPath) that).uri);
    }

    /**
     * Implements a natural ordering which is consistent with
     * {@link #equals(Object)}.
     */
    @Override
    public int compareTo(FsPath that) {
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