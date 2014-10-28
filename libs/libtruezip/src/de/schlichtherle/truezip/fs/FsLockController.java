/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.Threads;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * Provides read/write locking for multi-threaded access by its clients.
 *
 * @see    FsLockModel
 * @see    FsNeedsWriteLockException
 * @since  TrueZIP 7.5
 * @author Christian Schlichtherle
 */
final class FsLockController
extends FsLockModelDecoratingController<FsController<? extends FsLockModel>> {

    private static final SocketFactory SOCKET_FACTORY = SocketFactory.OIO;

    private static final ThreadLocal<Account> accounts =
            ThreadLocalAccountFactory.OLD.newThreadLocalAccount();

    private final ReadLock readLock;
    private final WriteLock writeLock;

    /**
     * Constructs a new file system lock controller.
     *
     * @param controller the decorated file system controller.
     */
    FsLockController(FsController<? extends FsLockModel> controller) {
        super(controller);
        this.readLock = getModel().readLock();
        this.writeLock = getModel().writeLock();
    }

    @Override
    protected ReadLock readLock() {
        return this.readLock;
    }

    @Override
    protected WriteLock writeLock() {
        return this.writeLock;
    }

    @Override
    public boolean isReadOnly() throws IOException {
        final class IsReadOnly implements Operation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return delegate.isReadOnly();
            }
        } // IsReadOnly
        return readOrWriteLocked(new IsReadOnly());
    }

    @Override
    public FsEntry getEntry(final FsEntryName name) throws IOException {
        final class GetEntry implements Operation<FsEntry> {
            @Override
            public FsEntry call() throws IOException {
                return delegate.getEntry(name);
            }
        } // GetEntry
        return readOrWriteLocked(new GetEntry());
    }

    @Override
    public boolean isReadable(final FsEntryName name) throws IOException {
        final class IsReadable implements Operation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return delegate.isReadable(name);
            }
        } // IsReadable
        return readOrWriteLocked(new IsReadable());
    }

    @Override
    public boolean isWritable(final FsEntryName name) throws IOException {
        final class IsWritable implements Operation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return delegate.isWritable(name);
            }
        } // IsWritable
        return readOrWriteLocked(new IsWritable());
    }

    @Override
    public boolean isExecutable(final FsEntryName name) throws IOException {
        final class IsExecutable implements Operation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return delegate.isExecutable(name);
            }
        } // IsExecutable
        return readOrWriteLocked(new IsExecutable());
    }

    @Override
    public void setReadOnly(final FsEntryName name) throws IOException {
        final class SetReadOnly implements Operation<Void> {
            @Override
            public Void call() throws IOException {
                delegate.setReadOnly(name);
                return null;
            }
        } // SetReadOnly
        writeLocked(new SetReadOnly());
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final Map<Access, Long> times,
            final BitField<FsOutputOption> options)
    throws IOException {
        final class SetTime implements Operation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return delegate.setTime(name, times, options);
            }
        } // SetTime
        return writeLocked(new SetTime());
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final BitField<Access> types,
            final long value,
            final BitField<FsOutputOption> options)
    throws IOException {
        final class SetTime implements Operation<Boolean> {
            @Override
            public Boolean call() throws IOException {
                return delegate.setTime(name, types, value, options);
            }
        } // SetTime
        return writeLocked(new SetTime());
    }

    @Override
    public InputSocket<?> getInputSocket(   FsEntryName name,
                                            BitField<FsInputOption> options) {
        return SOCKET_FACTORY.newInputSocket(this, name, options);
    }

    @Override
    public OutputSocket<?> getOutputSocket( FsEntryName name,
                                            BitField<FsOutputOption> options,
                                            Entry template) {
        return SOCKET_FACTORY.newOutputSocket(this, name, options, template);
    }

    @Override
    public void
    mknod(  final FsEntryName name,
            final Type type,
            final BitField<FsOutputOption> options,
            final Entry template)
    throws IOException {
        final class Mknod implements Operation<Void> {
            @Override
            public Void call() throws IOException {
                delegate.mknod(name, type, options, template);
                return null;
            }
        } // Mknod
        writeLocked(new Mknod());
    }

    @Override
    public void unlink(
            final FsEntryName name,
            final BitField<FsOutputOption> options)
    throws IOException {
        final class Unlink implements Operation<Void> {
            @Override
            public Void call() throws IOException {
                delegate.unlink(name, options);
                return null;
            }
        } // Unlink
        writeLocked(new Unlink());
    }

    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncException {
        final class Sync implements Operation<Void> {
            @Override
            public Void call() throws IOException {
                delegate.sync(options);
                return null;
            }
        } // Sync
        try {
            writeLocked(new Sync());
        } catch (final FsSyncException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new AssertionError(ex);
        }
    }

    private interface Operation<T> {
        T call() throws IOException;
    } // Operation

    <T> T readOrWriteLocked(Operation<T> operation)
    throws IOException {
        try {
            return readLocked(operation);
        } catch (FsNeedsWriteLockException ex) {
            return writeLocked(operation);
        }
    }

    <T> T readLocked(Operation<T> operation) throws IOException {
        return locked(operation, readLock());
    }

    <T> T writeLocked(Operation<T> operation) throws IOException {
        assert !getModel().isReadLockedByCurrentThread()
                : "Trying to upgrade a read lock to a write lock would only result in a dead lock - see Javadoc for ReentrantReadWriteLock!";
        return locked(operation, writeLock());
    }

    /**
     * Tries to call the given consistent operation while holding the given
     * lock.
     * <p>
     * If this is the first execution of this method on the call stack of the
     * current thread, then the lock gets acquired using {@link Lock#lock()}.
     * Once the lock has been acquired the operation gets called.
     * If this fails for some reason and the thrown exception chain contains a
     * {@link FsNeedsLockRetryException}, then the lock gets temporarily
     * released and the current thread gets paused for a small random time
     * interval before this procedure starts over again.
     * Otherwise, the exception chain gets just passed on to the caller.
     * <p>
     * If this is <em>not</em> the first execution of this method on the call
     * stack of the current thread, then the lock gets acquired using
     * {@link Lock#tryLock()} instead.
     * If this fails, an {@code FsNeedsLockRetryException} gets created and
     * passed to the given exception handler for mapping before finally
     * throwing the resulting exception by executing
     * {@code throw handler.fail(new FsNeedsLockRetryException())}.
     * Once the lock has been acquired the operation gets called.
     * If this fails for some reason then the exception chain gets just passed
     * on to the caller.
     * <p>
     * This algorithm prevents dead locks effectively by temporarily unwinding
     * the stack and releasing all locks for a small random time interval.
     * Note that this requires some minimal cooperation by the operation:
     * Whenever it throws an exception, it MUST leave its resources in a
     * consistent state so that it can get retried again!
     * Mind that this is standard requirement for any {@link FsController}.
     *
     * @param  <T> The return type of the operation.
     * @param  operation The atomic operation.
     * @param  lock The lock to hold while calling the operation.
     * @return The result of the operation.
     * @throws IOException As thrown by the operation.
     * @throws FsNeedsLockRetryException See above.
     */
    private <T> T locked(final Operation<T> operation, final Lock lock)
    throws IOException {
        final Account account = accounts.get();
        if (0 < account.lockCount) {
            if (!lock.tryLock()) throw FsNeedsLockRetryException.get();
            account.lockCount++;
            try {
                return operation.call();
            } finally {
                account.lockCount--;
                lock.unlock();
            }
        } else {
            try {
                while (true) {
                    try {
                        lock.lock();
                        account.lockCount++;
                        try {
                            return operation.call();
                        } finally {
                            account.lockCount--;
                            lock.unlock();
                        }
                    } catch (FsNeedsLockRetryException ex) {
                        account.pause();
                    }
                }
            } finally {
                accounts.remove();
            }
        }
    }

    static int getLockCount() {
        return accounts.get().lockCount;
    }

    private enum SocketFactory {
        OIO() {
            @Override
            InputSocket<?> newInputSocket(
                    FsLockController controller,
                    FsEntryName name,
                    BitField<FsInputOption> options) {
                return controller.new Input(name, options);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsLockController controller,
                    FsEntryName name,
                    BitField<FsOutputOption> options,
                    Entry template) {
                return controller.new Output(name, options, template);
            }
        };

        abstract InputSocket<?> newInputSocket(
                FsLockController controller,
                FsEntryName name,
                BitField<FsInputOption> options);

        abstract OutputSocket<?> newOutputSocket(
                FsLockController controller,
                FsEntryName name,
                BitField<FsOutputOption> options,
                Entry template);
    } // SocketFactory

    private class Input extends DecoratingInputSocket<Entry> {
        Input(  final FsEntryName name,
                final BitField<FsInputOption> options) {
            super(FsLockController.this.delegate
                    .getInputSocket(name, options));
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            final class GetLocalTarget implements Operation<Entry> {
                @Override
                public Entry call() throws IOException {
                    return getBoundSocket().getLocalTarget();
                }
            } // GetLocalTarget
            return writeLocked(new GetLocalTarget());
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            final class NewReadOnlyFile implements Operation<ReadOnlyFile> {
                @Override
                public ReadOnlyFile call() throws IOException {
                    return new LockReadOnlyFile(
                            getBoundSocket().newReadOnlyFile());
                }
            } // NewReadOnlyFile
            return writeLocked(new NewReadOnlyFile());
        }

        @Override
        public InputStream newInputStream() throws IOException {
            final class NewInputStream implements Operation<InputStream> {
                @Override
                public InputStream call() throws IOException {
                    return new LockInputStream(
                            getBoundSocket().newInputStream());
                }
            } // NewInputStream
            return writeLocked(new NewInputStream());
        }
    } // Input

    private class Output extends DecoratingOutputSocket<Entry> {
        Output( final FsEntryName name,
                final BitField<FsOutputOption> options,
                final Entry template) {
            super(FsLockController.this.delegate
                    .getOutputSocket(name, options, template));
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            final class GetLocalTarget implements Operation<Entry> {
                @Override
                public Entry call() throws IOException {
                    return getBoundSocket().getLocalTarget();
                }
            } // GetLocalTarget
            return writeLocked(new GetLocalTarget());
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            final class NewOutputStream implements Operation<OutputStream> {
                @Override
                public OutputStream call() throws IOException {
                    return new LockOutputStream(
                            getBoundSocket().newOutputStream());
                }
            } // NewOutputStream
            return writeLocked(new NewOutputStream());
        }
    } // Output

    private final class LockReadOnlyFile
    extends DecoratingReadOnlyFile {
        LockReadOnlyFile(ReadOnlyFile rof) {
            super(rof);
        }

        @Override
        public void close() throws IOException {
            FsLockController.this.close(delegate);
        }
    } // LockReadOnlyFile

    private final class LockInputStream
    extends DecoratingInputStream {
        LockInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            FsLockController.this.close(delegate);
        }
    } // LockInputStream

    private final class LockOutputStream
    extends DecoratingOutputStream {
        LockOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            FsLockController.this.close(delegate);
        }
    } // LockOutputStream

    void close(final Closeable closeable) throws IOException {
        final class Close implements Operation<Void> {
            @Override
            public Void call() throws IOException {
                closeable.close();
                return null;
            }
        } // Close
        writeLocked(new Close());
    }

    private static final class Account {
        int lockCount;
        final Random rnd;

        Account(Random rnd) { this.rnd = rnd; }

        /**
         * Delays the current thread for a random time interval between one and
         * {@link #WAIT_TIMEOUT_MILLIS} milliseconds inclusively.
         * Interrupting the current thread has no effect on this method.
         */
        void pause() {
            Threads.pause(1 + rnd.nextInt(WAIT_TIMEOUT_MILLIS));
        }
    } // ThreadUtil

    private enum ThreadLocalAccountFactory {
        OLD {
            @Override
            ThreadLocal<Account> newThreadLocalAccount() {
                return new ThreadLocal<Account>() {
                    @Override
                    public Account initialValue() {
                        return new Account(new Random());
                    }
                };
            }
        };

        abstract ThreadLocal<Account> newThreadLocalAccount();
    } // ThreadLocalAccountFactory
}
