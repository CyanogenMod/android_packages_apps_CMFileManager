/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.crypto.param.AesKeyStrength;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyManagerProvider;
import de.schlichtherle.truezip.key.KeyProvider;
import de.schlichtherle.truezip.key.UnknownKeyException;
import de.schlichtherle.truezip.key.pbe.AesPbeParameters;
import de.schlichtherle.truezip.zip.*;
import java.net.URI;
import java.nio.charset.Charset;
import libtruezip.lcrypto.crypto.PBEParametersGenerator;
import static libtruezip.lcrypto.crypto.PBEParametersGenerator.PKCS5PasswordToBytes;

/**
 * An adapter which provides {@link ZipCryptoParameters} by using a
 * {@link KeyManagerProvider}.
 * <p>
 * The current implementation supports only {@link WinZipAesParameters}.
 *
 * @since  TrueZIP 7.3
 * @author Christian Schlichtherle
 */
public class KeyManagerZipCryptoParameters
implements ZipParametersProvider, ZipCryptoParameters {

    /** The ZIP driver which has been provided to the constructor. */
    private final ZipDriver driver;

    /** The file system model which has been provided to the constructor. */
    private final FsModel model;

    /**
     * The character set which has been provided to the constructor and is
     * used for encoding entry names and the file comment in the ZIP file .
     */
    protected final Charset charset;

    /**
     * Constructs new ZIP crypto parameters.
     *
     * @param driver the ZIP driver.
     * @param model the file system model.
     * @param charset the character set which is used for encoding entry names
     *        and the file comment in the ZIP file.
     */
    public KeyManagerZipCryptoParameters(
            final ZipDriver driver,
            final FsModel model,
            final Charset charset) {
        if (null == driver || null == model || null == charset)
            throw new NullPointerException();
        this.driver = driver;
        this.model = model;
        this.charset = charset;
    }

    /**
     * {@inheritDoc}
     * <p>
     * If {@code type} is assignable from {@link WinZipAesParameters}, then a
     * {@link KeyManager} for {@link AesPbeParameters} will get used which
     * is obtained from the {@link KeyManagerProvider provider} which has been
     * provided to the constructor.
     * <p>
     * Otherwise, {@code null} gets returned.
     */
    @Override
    public <P extends ZipParameters> P get(Class<P> type) {
        if (type.isAssignableFrom(WinZipAesParameters.class))
            return type.cast(new WinZipAes());
        return null;
    }

    /**
     * A template method to derive password bytes from the given password
     * characters and the given entry name.
     * Typically, only the given password characters should get encoded to
     * form the result.
     * Optionally, the given entry name can be mixed into the result for
     * authentication.
     * Note that this would then form a part of the password used for writing
     * or reading encrypted entries, however.
     * A third party which is expected to successfully read or write encrypted
     * entries would have to agree on the same password bytes derivation scheme.
     * <p>
     * The implementation in the class {@code KeyManagerZipCryptoParameters}
     * ignores the given entry name and just encodes the given password
     * characters using
     * {@link PBEParametersGenerator#PKCS5PasswordToBytes(char[])}.
     * <p>
     * A reasonable alternative implementation could encode the given password
     * characters using the value of the field {@link #charset}, which is the
     * character set actually used to encode entry names and comments in the
     * ZIP file, e.g. UTF-8 or CP437.
     *
     * @see    WinZipAesParameters#getWritePassword(String)
     *         Discussion Of Alternative Character Set Encodings
     * @param  characters the password characters to encode.
     * @param  name the entry name for optional mixing into the result.
     * @return The derived password bytes.
     * @see    <a href="http://www.ietf.org/rfc/rfc2898.txt">RFC 2898: PKCS #5: Password-Based Cryptography Specification Version 2.0 (IETF et al.)</a>
     */
    protected byte[] password(char[] characters, String name) {
        return PKCS5PasswordToBytes(characters);
    }

    private <K> KeyManager<K> keyManager(Class<K> type) {
        return driver.getKeyManagerProvider().get(type);
    }

    private URI resourceUri(String name) {
        return driver.resourceUri(model, name);
    }

    /**
     * Adapts a {@code KeyProvider} for {@link  AesPbeParameters} obtained
     * from the {@link #keyManager} to {@code WinZipAesParameters}.
     */
    private class WinZipAes implements WinZipAesParameters {
        final KeyManager<AesPbeParameters>
                manager = keyManager(AesPbeParameters.class);

        @Override
        public byte[] getWritePassword(final String name)
        throws ZipKeyException {
            final KeyProvider<AesPbeParameters>
                    provider = manager.getKeyProvider(resourceUri(name));
            try {
                return password(provider.getWriteKey().getPassword(), name);
            } catch (UnknownKeyException ex) {
                throw new ZipKeyException(ex);
            }
        }

        @Override
        public byte[] getReadPassword(final String name, final boolean invalid)
        throws ZipKeyException {
            final KeyProvider<AesPbeParameters>
                    provider = manager.getKeyProvider(resourceUri(name));
            try {
                return password(provider.getReadKey(invalid).getPassword(), name);
            } catch (UnknownKeyException ex) {
                throw new ZipKeyException(ex);
            }
        }

        @Override
        public AesKeyStrength getKeyStrength(final String name)
        throws ZipKeyException {
            final KeyProvider<AesPbeParameters>
                    provider = manager.getKeyProvider(resourceUri(name));
            try {
                return provider.getWriteKey().getKeyStrength();
            } catch (UnknownKeyException ex) {
                throw new ZipKeyException(ex);
            }
        }

        @Override
        public void setKeyStrength( final String name,
                                    final AesKeyStrength keyStrength)
        throws ZipKeyException {
            final KeyProvider<AesPbeParameters>
                    provider = manager.getKeyProvider(resourceUri(name));
            final AesPbeParameters param;
            try {
                param = provider.getReadKey(false);
            } catch (UnknownKeyException ex) {
                throw new ZipKeyException(ex);
            }
            param.setKeyStrength(keyStrength);
            provider.setKey(param);
        }
    } // WinZipAes
}
