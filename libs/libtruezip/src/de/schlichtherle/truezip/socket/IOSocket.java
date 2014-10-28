/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.Streams;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An abstract class for objects to do some I/O on a
 * {@link #getLocalTarget() local target}.
 * I/O sockets are designed to {@link #copy(InputSocket, OutputSocket) copy}
 * data fast and easily.
 * When copying data, the local target gets connected to a
 * {@link #getPeerTarget() peer target}.
 * Once connected, the sockets can then setup the data to be transferred by
 * the copy method.
 * For example, this feature can get used to avoid data recompression when
 * copying a compressed archive entry to another archive entry of the same
 * type.
 *
 * @param  <LT> the type of the {@link #getLocalTarget() local target}
 *         for I/O operations.
 * @param  <PT> the type of the {@link #getPeerTarget() peer target}
 *         for I/O operations.
 * @author Christian Schlichtherle
 */
public abstract class IOSocket<LT, PT> {

    /** You cannot instantiate this class outside its package. */
    IOSocket() {
    }

    /**
     * Returns the <i>local target</i> for I/O operations.
     * <p>
     * Note that this interface contract does <em>not</em> state any other
     * terms or conditions for the returned object.
     * In particular, clients need to consider that multiple invocations of
     * this method could return different objects (e.g. defensive copies) which
     * may even fail the {@link Object#equals} test.
     * On the other hand, implementations need to consider that clients could
     * attempt to change the state of the returned object in arbitrary manner.
     * Consequently, the result of doing so is undefined, too.
     * In particular, a subsequent I/O operation may not reflect the change
     * or may even fail.
     * Sub-interfaces or implementations may add additional terms and
     * conditions in order to resolve these potential issues.
     *
     * @return The local target for I/O operations.
     * @throws IOException On any I/O failure.
     */
    public abstract LT getLocalTarget() throws IOException;

    /**
     * Returns the <i>peer target</i> for I/O operations.
     * <p>
     * The same considerations as for {@link #getLocalTarget} apply here, too.
     *
     * @return The peer target for I/O operations.
     * @throws IOException On any I/O failure.
     */
    // TODO: Consider renaming to getRemoteTarget() in TrueZIP 8.
    public abstract PT getPeerTarget() throws IOException;

    /**
     * Copies an input stream {@link InputSocket#newInputStream created}
     * by the given input socket {@code input} to an output stream
     * {@link OutputSocket#newOutputStream created} by the given output socket
     * {@code output}.
     * <p>
     * This is a high performance implementation which uses a pooled background
     * thread to fill a FIFO of pooled buffers which is concurrently flushed by
     * the current thread.
     *
     * @param  input an input socket for the input target.
     * @param  output an output socket for the output target.
     * @throws InputException if copying the data fails because of an
     *         {@code IOException} thrown by the <em>input socket</em>.
     * @throws IOException if copying the data fails because of an
     *         {@code IOException} thrown by the <em>output socket</em>.
     */
    public static void copy(final InputSocket <?> input,
                            final OutputSocket<?> output)
    throws IOException {
        if (null == output)
            throw new NullPointerException();

        OutputStream out = null;
        final InputStream in;
        try {
            in = input.connect(output).newInputStream();
        } catch (final IOException ex) {
            throw new InputException(ex);
        }
        try {
            // .connect(input) is redundant unless .newInputStream() messed
            // with the connection, which is impossible outside this package.
            out = output/*.connect(input)*/.newOutputStream();
        } finally {
            if (null == out) { // exception?
                try {
                    in.close();
                } catch (IOException ex) {
                    throw new InputException(ex);
                }
            }
        }

        Streams.copy(in, out);
        // Disconnect for subsequent use, if any.
        input.connect(null); // or output.connect(null)
    }

    /**
     * Returns a string representing a connection of the local and peer
     * targets.
     */
    @Override
    public String toString() {
        Object lt;
        try {
            lt = getLocalTarget();
        } catch (final IOException ex) {
            lt = ex;
        }
        Object pt;
        try {
            pt = getPeerTarget();
        } catch (final IOException ex) {
            pt = ex;
        }
        return String.format("%s[localTarget=%s, peerTarget=%s]",
                getClass().getName(), lt, pt);
    }

    /**
     * Two sockets are considered equal if and only if they are identical.
     *
     * @param that The object to test.
     */
    @Override
    public final boolean equals(Object that) {
        return this == that;
    }

    /**
     * Returns a hash code which is consistent with {@link #equals}.
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }
}
