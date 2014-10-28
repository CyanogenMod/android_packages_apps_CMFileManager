/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.file;

import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncWarningException;
//import de.schlichtherle.truezip.fs.archive.tar.TarBZip2Driver;
//import de.schlichtherle.truezip.fs.archive.tar.TarDriver;
//import de.schlichtherle.truezip.fs.archive.tar.TarGZipDriver;
//import de.schlichtherle.truezip.fs.archive.zip.JarDriver;
//import de.schlichtherle.truezip.fs.archive.zip.ZipDriver;
//import de.schlichtherle.truezip.fs.archive.zip.raes.ParanoidZipRaesDriver;
//import de.schlichtherle.truezip.fs.archive.zip.raes.SafeZipRaesDriver;
//import de.schlichtherle.truezip.key.sl.KeyManagerLocator;
//import de.schlichtherle.truezip.socket.spi.ByteArrayIOPoolService;
//import de.schlichtherle.truezip.socket.sl.IOPoolLocator;

/**
 * A template class which aids in establishing the typical
 * <i>setup-work-sync</i> life cycle of a TrueZIP application.
 * The established life cycle consists of the {@link #setup} phase and one or
 * more iterations of the {@link #work} and finally {@link #sync} phases.
 * <p>
 * Subclass or copy-and-edit the source code of this template class to meet
 * your requirements.
 * <p>
 *
 * @param   <E> the {@link Exception} class to throw by {@link #work} and
 *          {@link #run}.
 * @author Christian Schlichtherle
 */
public abstract class TApplication<E extends Exception> {

    /**
     * Runs the setup-work-sync life cycle.
     * At first, this method calls {@link #setup}.
     * Then, a loop is entered which calls {@link #work} and (in a finally
     * block) {@link #sync}.
     * If {@link #work} returns a negative integer, then the loop is
     * repeated.
     * Otherwise, the return value is used as the
     * {@link System#exit(int) exit status} of the VM.
     * <p>
     * Note that calling {@link #sync} in a finally-block ensures that all
     * unsynchronized changes to the contents of all federated file systems
     * (i.e. archive files) get committed to their respective parent file
     * system, even if {@link #work} throws an exception.
     *
     * @throws E At the discretion of the {@link #work} method.
     * @throws FsSyncException At the discretion of the {@link #sync} method.
     */
    protected final int run(String[] args) throws E, FsSyncException {
        setup();
        int status;
        do {
            try {
                status = work(args);
            } finally {
                sync();
            }
        } while (status < 0);
        return status;
    }

    /**
     * Runs the setup phase.
     * <p>
     * This method is called by {@link #run} at the start of the life cycle.
     * Its task is to configure the default behavior of the TrueZIP File* API
     * in order to answer the following questions:
     * <ul>
     * <li>Which file suffixes shall get detected to indicate archive
     *     files and hence as virtual directories?
     * <li>Which kind of temporary buffers shall get used?
     * <li>Shall missing archive files and directory entries get automatically
     *     created whenever required?
     * </ul>
     * <p>
     * Note that the method body of the implementation in the class
     * {@link TApplication} is empty!
     * This means that the initial setup is not changed.
     * The initial setup is determined by loading service classes from the
     * class path and applying reasonable defaults if nothing is found.
     * <p>
     * As an alternative to overriding this method, the setup may get changed
     * in a sub class constructor.
     * This would ensure it's changed exactly only once and not on every call
     * to {@link #run}.
     *
     * <h3>Examples</h3>
     * <p>
     * As the most simple use case, a client application might want to filter
     * the initial setup for the archive file suffixes it wants to detect.
     * This is done as follows:
     * <pre>{@code
     * TConfig.get().setArchiveDetector(new TArchiveDetector("ear|jar|war"));
     * }</pre>
     * <p>
     * This will filter all file system drivers found on the class path in the
     * initial setup so that only files with the pattern {@code *.ear},
     * {@code *.jar} or {@code *.war} (case ignoring) are detected
     * as prospective archive files.
     * If no file system driver is present for a named suffix in the initial
     * setup, an {@link IllegalArgumentException} is thrown.
     * <p>
     * The beauty of this simple example is that it does not require to change
     * the compile time class path.
     * For more advanced examples however, the file {@code pom.xml} needs to
     * get edited so that the respective modules get added to the compile time
     * class path.
     * <p>
     * The constructors of the {@link TFile} class use the
     * {@link TArchiveDetector} class to scan a path name for suffixes of
     * archive files which shall be treated like virtual directories.
     * You can either explicitly inject this ArchiveDetector dependency into a
     * TFile constructor or you can rely on the value of the class property
     * {@code defaultArchiveDetector}.
     * <p>
     * By default, all new {@link TFile} objects will use a default
     * {@link TArchiveDetector} which recognizes the canonical archive file
     * suffixes registered by all archive driver modules which are present on
     * the class path at run time - these can be configured by editing the
     * file {@code pom.xml}.
     * <p>
     * However, if you use the following statement, all {@link TFile} objects
     * will use the given default {@link TArchiveDetector} which recognizes
     * only the given canonical file suffixes for TAR, TAR.GZ, TAR.BZ2 and ZIP
     * files as archive files and hence as virtual directories.
     * This requires the JARs for the archive driver modules
     * {@code truezip-driver-tar} and {@code truezip-driver-zip} to be present
     * on the class path at compile time:
     * <pre>{@code
     * TConfig.get().setArchiveDetector(
     *         new TArchiveDetector(
     *             TArchiveDetector.NULL,
     *             new Object[][] {
     *                 { "tar", new TarDriver(IOPoolLocator.SINGLETON) },
     *                 { "tgz|tar.gz", new TarGZipDriver(IOPoolLocator.SINGLETON) },
     *                 { "tbz|tb2|tar.bz2", new TarBZip2Driver(IOPoolLocator.SINGLETON) },
     *                 { "zip", new ZipDriver(IOPoolLocator.SINGLETON)},
     *             }));
     * }</pre>
     * <p>
     * Another typical use case is to recognize only Java artifacts.
     * <pre>{@code
     * TConfig.get().setArchiveDetector(
     *         new TArchiveDetector(
     *             "ear|jar|war",
     *             new JarDriver(IOPoolLocator.SINGLETON)));
     * }</pre>
     * <p>
     * ... or an application file format.
     * <pre>{@code
     * TConfig.get().setArchiveDetector(
     *         new TArchiveDetector(
     *             "foo",
     *             new JarDriver(IOPoolLocator.SINGLETON)));
     * }</pre>
     * <p>
     * ... or an encrypted application file format.
     * This driver authenticates input archive files up to 512 KB using the
     * Message Authentication Code (MAC) specified by the RAES file format.
     * For larger input archive files, it just checks the CRC-32 value whenever
     * an archive entry input stream is closed.
     * CRC-32 has frequent collisions when compared to a MAC.
     * However, it should not be feasible to make an undetectable modification.
     * The driver also uses unencrypted temporary files for archive entries
     * whenever required.
     * <pre>{@code
     * TConfig.get().setArchiveDetector(
     *         new TArchiveDetector(
     *             "bar",
     *             new SafeZipRaesDriver(
     *                 IOPoolLocator.SINGLETON,
     *                 KeyManagerLocator.SINGLETON)));
     * }</pre>
     * <p>
     * If you're a bit paranoid, then you could use the following driver
     * instead:
     * This driver authenticates every input archive file using the Message
     * Authentication Code (MAC) specified by the RAES file format, which
     * makes it comparably slow.
     * The driver also uses unencrypted temporary files for archive entries
     * whenever required.
     * <pre>{@code
     * TConfig.get().setArchiveDetector(
     *         new TArchiveDetector(
     *             "bar",
     *             new ParanoidZipRaesDriver(
     *                 IOPoolLocator.SINGLETON,
     *                 KeyManagerLocator.SINGLETON)));
     * }</pre>
     * <p>
     * And finally, if you're quite paranoid, then this driver is for you:
     * This driver authenticates every input archive file using the Message
     * Authentication Code (MAC) specified by the RAES file format, which
     * makes it comparably slow.
     * The driver also uses unencrypted byte arrays for temporary storage of
     * archive entries whenever required.
     * If you were completely paranoid, you would even want to use encrypted
     * byte arrays or wipe them with nulls after use.
     * However, then you would have to write this yourself! ;-)
     * <pre>{@code
     * TConfig.get().setArchiveDetector(
     *         new TArchiveDetector(
     *             "bar",
     *             new ParanoidZipRaesDriver(
     *                 new ByteArrayIOPoolService(2048),
     *                 KeyManagerLocator.SINGLETON)));
     * }</pre>
     * <p>
     * Last but not least, the following class property controls whether
     * archive files and their member directories get automatically created
     * whenever required.
     * The default value of this class property is {@code true}!
     * <pre>{@code
     * TConfig.get().setLenient(false);
     * }</pre>
     */
    protected void setup() {
    }

    /**
     * Runs the work phase.
     * <p>
     * This method is called by {@link #run} at least once and repeatedly
     * called until it returns a non-negative integer for use as the
     * {@link System#exit(int) exist status} of the VM.
     * After this method, the {@link #sync} method is called in a finally-block.
     * <p>
     * Avoid repeating this method and updating the same archive file upon
     * each call!
     * This would degrade the overall performance from O(n) to O(m*n),
     * where m is the number of new or modified entries and n is the number
     * of all entries in the archive file!
     *
     * @param  args an array of arguments for this application.
     * @return A negative integer in order to continue calling this method
     *         in a loop.
     *         Otherwise, the return value is used as the
     *         {@link System#exit(int) exit status} of the VM.
     */
    protected abstract int work(String[] args) throws E;

    /**
     * Runs the sync phase.
     * <p>
     * This method is called by {@link #run} in a finally-block after each call
     * to {@link #work}.
     * Its task is to commit any unsynchronized changes to the contents of
     * archive files and purge all cached data so that third parties can safely
     * interact with these archive files until the next access by this TrueZIP
     * application.
     * <p>
     * The implementation in the class {@link TApplication} simply calls
     * {@link TVFS#umount() TVFS.umount()}.
     *
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         occur.
     *         This implies that the respective parent file system has been
     *         updated with constraints, such as a failure to set the last
     *         modification time of the entry for the federated file system
     *         (i.e. archive file) in its parent file system.
     * @throws FsSyncException if any error conditions occur.
     *         This implies loss of data!
     */
    protected void sync() throws FsSyncException {
        TVFS.umount();
    }
}
