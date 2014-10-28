ChangeLog
========================

TrueZip v7
----------
Grabbed from https://hg.java.net/hg/truezip~v7 (r6207).
The source was downgrade to fully comply with JS6 and remove NIO2/ServiceLocator/Swing/Console
support and stripped down to use the minimal subset of classes to run RAES engine. Removed
javax/findBugs annotations.

BouncyCastle (Lightweight API)
------------------------------
Grabbed from source version lcrypto-jdk15on-151.
Only contains the crypto classes requiered by TrueZip.
Change namespace from org.bouncycastle to securespace.lcrypto to avoid
namespace conflicts.

Commons Compress (Bzip2)
------------------------
Grabbed from source version 1.9.
Only contains Bzip2 compressor engine.
Change namespace from org.apache.commons.compress to securespace.compress to avoid
namespace conflicts.
