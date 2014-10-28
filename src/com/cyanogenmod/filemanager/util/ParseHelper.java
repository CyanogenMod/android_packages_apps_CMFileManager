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

import com.cyanogenmod.filemanager.model.BlockDevice;
import com.cyanogenmod.filemanager.model.CharacterDevice;
import com.cyanogenmod.filemanager.model.Directory;
import com.cyanogenmod.filemanager.model.DiskUsage;
import com.cyanogenmod.filemanager.model.DomainSocket;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.Group;
import com.cyanogenmod.filemanager.model.GroupPermission;
import com.cyanogenmod.filemanager.model.MountPoint;
import com.cyanogenmod.filemanager.model.NamedPipe;
import com.cyanogenmod.filemanager.model.OthersPermission;
import com.cyanogenmod.filemanager.model.Permission;
import com.cyanogenmod.filemanager.model.Permissions;
import com.cyanogenmod.filemanager.model.RegularFile;
import com.cyanogenmod.filemanager.model.Symlink;
import com.cyanogenmod.filemanager.model.User;
import com.cyanogenmod.filemanager.model.UserPermission;

import java.io.File;
import java.text.ParseException;
import java.util.Date;

/**
 * A helper class with useful methods for deal with parse of results.
 */
public final class ParseHelper {

    // The structure of a terse stat output
    // http://mailman.lug.org.uk/pipermail/nottingham/2007-January/009303.html
    private static enum TERSE_STAT_STRUCT {
        FILENAME,
        SIZE,
        BLOCKS,
        RAW_MODE,
        UID,
        GID,
        DEVICE,
        INODE,
        HARD_LINKS,
        MAJOR_DEVICE_TYPE,
        MINOR_DEVICE_TYPE,
        ACCESS,
        MODIFY,
        CHANGE,
        IOBLOCK
    }
    private static int TERSE_STAT_STRUCT_LENGTH = TERSE_STAT_STRUCT.values().length;

    // The structure of raw mode in hex format (defined with octal values)
    // http://unix.stackexchange.com/questions/39716/what-is-raw-mode-in-hex-from-stat-output
    private static enum RMIHF {
        S_IFMT   (0170000),   //bit mask for the file type bit fields
        S_IFSOCK (0140000),   //socket
        S_IFLNK  (0120000),   //symbolic link
        S_IFREG  (0100000),   //regular file
        S_IFBLK  (0060000),   //block device
        S_IFDIR  (0040000),   //directory
        S_IFCHR  (0020000),   //character device
        S_IFIFO  (0010000),   //FIFO
        S_ISUID  (0004000),   //set UID bit
        S_ISGID  (0002000),   //set-group-ID bit (see below)
        S_ISVTX  (0001000),   //sticky bit (see below)
        S_IRWXU  (0000700),   //mask for file owner permissions
        S_IRUSR  (0000400),   //owner has read permission
        S_IWUSR  (0000200),   //owner has write permission
        S_IXUSR  (0000100),   //owner has execute permission
        S_IRWXG  (0000070),   //mask for group permissions
        S_IRGRP  (0000040),   //group has read permission
        S_IWGRP  (0000020),   //group has write permission
        S_IXGRP  (0000010),   //group has execute permission
        S_IRWXO  (0000007),   //mask for permissions for others (not in group)
        S_IROTH  (0000004),   //others have read permission
        S_IWOTH  (0000002),   //others have write permission
        S_IXOTH  (0000001);   //others have execute permission

        final int mValue;
        RMIHF(int value) {
            this.mValue = value;
        }
    }

    /**
     * Constructor of <code>ParseHelper</code>.
     */
    private ParseHelper() {
        super();
    }

    /**
     * Method that parses the output of a terse stat command.<br/>
     * <br/>
     * The stat terse format is described as:<br/>
     * <br/>
     * <code/>
     *  terse format = "%n %s %b %f %u %g %D %i %h %t %T %X %Y %Z %o":
     *      filename
     *      size(bytes)
     *      blocks
     *      Raw_mode(HEX)
     *      Uid
     *      Gid
     *      Device(HEX)
     *      Inode
     *      hard_links
     *      major_device_type(HEX)
     *      minor_device_type(HEX)
     *      Access(Epoch seconds)
     *      Modify(Epoch seconds)
     *      Change(Epoch seconds)
     *      IOblock
     * </code>
     *
     * @param output Line with the output of a line of a stat command
     * @return FileSystemObject The file system object reference
     * @throws ParseException If the permissions can't be parsed
     * @{link "http://www.gnu.org/software/coreutils/manual/html_node/stat-invocation.html"}
     */
    public static FileSystemObject parseStatOutput(final String output) throws ParseException {

        try {
            // Split the terse line
            String[] data = output.split(" "); //$NON-NLS-1$
            boolean valid = true;
            try {
                getTerseStatInt(data, TERSE_STAT_STRUCT.IOBLOCK);
            } catch (Exception e) {
                valid = false;
            }
            if (valid && output.startsWith("stat:")) { //$NON-NLS-1$
                throw new ParseException(
                        String.format("Stat failed: %s", output), 0); //$NON-NLS-1$
            }
            if (valid && data.length < TERSE_STAT_STRUCT.values().length) {
                throw new ParseException(
                        String.format("Not enought data: %s", output), 0); //$NON-NLS-1$
            }

            // Parse the line
            String raw = getTerseRawPermissions(data);
            char type = raw.charAt(0);
            Permissions permissions = parsePermission(raw);
            Date lastAccessedTime = getTerseStatDate(data, TERSE_STAT_STRUCT.ACCESS);
            Date lastModifiedTime = getTerseStatDate(data, TERSE_STAT_STRUCT.MODIFY);
            Date lastChangedTime = getTerseStatDate(data, TERSE_STAT_STRUCT.CHANGE);
            int uid = getTerseStatInt(data, TERSE_STAT_STRUCT.UID);
            User user = new User(uid, AIDHelper.getNullSafeName(uid));
            int gid = getTerseStatInt(data, TERSE_STAT_STRUCT.GID);
            Group group = new Group(gid, AIDHelper.getNullSafeName(gid));
            long size = getTerseStatLong(data, TERSE_STAT_STRUCT.SIZE);
            File file = new File(getTerseStatName(data));
            String name = file.getName();
            if (name.trim().length() == 0) {
                name = FileHelper.ROOT_DIRECTORY;
            }
            String parentDir = FileHelper.getParentDir(file);

            // Create the file system object
            FileSystemObject fso =
                    createObject(
                        parentDir, type, name, null, user, group, permissions,
                        size, lastAccessedTime, lastModifiedTime, lastChangedTime);

            // Check if its a symlink
            if (type == Symlink.UNIX_ID) {
                // Extract the ref info
                Symlink symlink = (Symlink)fso;
                File refFile = file.getCanonicalFile();
                char refType = refFile.isDirectory() ? Directory.UNIX_ID : RegularFile.UNIX_ID;
                String refName = refFile.getName();
                String refParentDir = FileHelper.getParentDir(refFile);
                Date refLastModifiedTime = new Date(refFile.lastModified());
                long refSize = refFile.length();

                // Create the ref file system object
                FileSystemObject refFso =
                        createObject(
                            refParentDir, refType, refName, null, null, null, null,
                            refSize, null, refLastModifiedTime, null);

                // Update the symlink ref
                symlink.setLink(refParentDir);
                symlink.setLinkRef(refFso);
            }

            // Parsed
            return fso;

        } catch (Exception ex) {
            // Notify the exception when parsing the data
            throw new ParseException(ex.getMessage(), 0);
        }
    }

    /**
     * Method that parses and extracts the permissions from a unix string format.
     *
     * @param permissions The raw permissions
     * @return Permissions An object with all the permissions
     * @throws ParseException If the permissions can't be parsed
     * @{link "http://en.wikipedia.org/wiki/File_system_permissions"}
     */
    public static Permissions parsePermission(String permissions) throws ParseException {
        if (permissions.length() != 10) {
            throw new ParseException("permission length() != 10", 0); //$NON-NLS-1$
        }
        UserPermission up = new UserPermission(
                permissions.charAt(1) == Permission.READ,
                permissions.charAt(2) == Permission.WRITE,
                permissions.charAt(3) ==  Permission.EXECUTE
                    || permissions.charAt(3) == UserPermission.SETUID_E,
                permissions.charAt(3) == UserPermission.SETUID_E
                    || permissions.charAt(3) == UserPermission.SETUID);
        GroupPermission gp = new GroupPermission(
                permissions.charAt(4) == Permission.READ,
                permissions.charAt(5) == Permission.WRITE,
                permissions.charAt(6) == Permission.EXECUTE
                    || permissions.charAt(6) == GroupPermission.SETGID_E,
                permissions.charAt(6) == GroupPermission.SETGID_E
                    || permissions.charAt(6) == GroupPermission.SETGID);
        OthersPermission op = new OthersPermission(
                permissions.charAt(7) == Permission.READ,
                permissions.charAt(8) == Permission.WRITE,
                permissions.charAt(9) == Permission.EXECUTE
                    || permissions.charAt(9) == OthersPermission.STICKY_E,
                permissions.charAt(9) == OthersPermission.STICKY_E
                    || permissions.charAt(9) == OthersPermission.STICKY);
        return new Permissions(up, gp, op);
    }

    /**
     * Method that parse a disk usage line.
     *
     * @param src The disk usage line
     * @return DiskUsage The disk usage information
     * @throws ParseException If the line can't be parsed
     */
    public static DiskUsage toDiskUsage(final String src) throws ParseException {

        // Filesystem             Size   Used   Free   Blksize
        // /dev                   414M    48K   414M   4096
        // /mnt/asec              414M     0K   414M   4096
        // /mnt/secure/asec: Permission denied

        try {
            final int fields = 5;

            //Permission denied or invalid statistics
            if (src.indexOf(":") != -1) { //$NON-NLS-1$
                throw new ParseException(String.format("Non allowed: %s", src), 0); //$NON-NLS-1$
            }

            //Extract all the info
            String line = src;
            String[] data = new String[fields];
            for (int i = 0; i < fields; i++) {
                int pos = line.indexOf(" "); //$NON-NLS-1$
                data[i] = line.substring(0, pos != -1 ? pos : line.length());
                if (pos != -1) {
                    line = line.substring(pos).trim();
                }
            }

            //Return the disk usage
            return new DiskUsage(data[0], toBytes(data[1]), toBytes(data[2]), toBytes(data[3]));

        } catch (Exception e) {
            throw new ParseException(e.getMessage(), 0);
        }
    }

    /**
     * Method that parse a {@link "/proc/mounts"} line.
     *
     * @param src The mount point line
     * @return MountPoint The mount point information
     * @throws ParseException If the line can't be parsed
     */
    public static MountPoint toMountPoint(final String src) throws ParseException {

        // rootfs / rootfs ro,relatime 0 0
        // tmpfs /dev tmpfs rw,nosuid,relatime,mode=755 0 0
        // devpts /dev/pts devpts rw,relatime,mode=600 0 0
        // /dev/block/vold/179:25 /mnt/emmc vfat rw,dirsync,nosuid,nodev,noexec,relatime,uid=1000, gid=1015,fmask=0702,dmask=0702,allow_utime=0020,codepage=cp437,iocharset=iso8859-1, shortname=mixed,utf8,errors=remount-ro 0 0

        try {

            //Extract all the info
            String line = src;
            int pos = line.lastIndexOf(" "); //$NON-NLS-1$
            int pass = Integer.parseInt(line.substring(pos + 1));
            line = line.substring(0, pos).trim();
            pos = line.lastIndexOf(" "); //$NON-NLS-1$
            int dump = Integer.parseInt(line.substring(pos + 1));
            line = line.substring(0, pos).trim();
            pos = line.indexOf(" "); //$NON-NLS-1$
            String device = line.substring(0, pos).trim();
            line = line.substring(pos).trim();
            pos = line.lastIndexOf(" "); //$NON-NLS-1$
            String options = line.substring(pos + 1).trim();
            line = line.substring(0, pos).trim();
            pos = line.lastIndexOf(" "); //$NON-NLS-1$
            String type = line.substring(pos + 1).trim();
            String mountPoint = line.substring(0, pos).trim();


            //Return the mount point
            return new MountPoint(mountPoint, device, type, options, dump, pass, false, false);

        } catch (Exception e) {
            throw new ParseException(e.getMessage(), 0);
        }
    }

    /**
     * Method that creates the appropriate file system object.
     *
     * @param parentDir The parent directory
     * @param type The raw char type of the file system object
     * @param name The name of the object
     * @param link The real file that this symlink is point to
     * @param user The user proprietary of the object
     * @param group The group proprietary of the object
     * @param permissions The permissions of the object
     * @param size The size in bytes of the object
     * @param lastAccessedTime The last time that the object was accessed
     * @param lastModifiedTime The last time that the object was modified
     * @param lastChangedTime The last time that the object was changed
     * @return FileSystemObject The file system object reference
     * @throws ParseException If type couldn't be translate into a reference
     * file system object
     */
    private static FileSystemObject createObject(
            String parentDir, char type, String name, String link, User user,
            Group group, Permissions permissions, long size,
            Date lastAccessedTime, Date lastModifiedTime, Date lastChangedTime)
            throws ParseException {

        String parent =
                (parentDir == null && name.compareTo(FileHelper.ROOT_DIRECTORY) != 0) ?
                            FileHelper.ROOT_DIRECTORY :
                            parentDir;

        if (type == RegularFile.UNIX_ID) {
            return new RegularFile(
                    name, parent, user, group, permissions, size,
                    lastAccessedTime, lastModifiedTime, lastChangedTime);
        }
        if (type == Directory.UNIX_ID) {
            return new Directory(name, parent, user, group, permissions,
                    lastAccessedTime, lastModifiedTime, lastChangedTime);
        }
        if (type == Symlink.UNIX_ID) {
            return new Symlink(name, link, parent, user, group, permissions,
                    lastAccessedTime, lastModifiedTime, lastChangedTime);
        }
        if (type == BlockDevice.UNIX_ID) {
            return new BlockDevice(name, parent, user, group, permissions,
                    lastAccessedTime, lastModifiedTime, lastChangedTime);
        }
        if (type == CharacterDevice.UNIX_ID) {
            return new CharacterDevice(name, parent, user, group, permissions,
                    lastAccessedTime, lastModifiedTime, lastChangedTime);
        }
        if (type == NamedPipe.UNIX_ID) {
            return new NamedPipe(name, parent, user, group, permissions,
                    lastAccessedTime, lastModifiedTime, lastChangedTime);
        }
        if (type == DomainSocket.UNIX_ID) {
            return new DomainSocket(name, parent, user, group, permissions,
                    lastAccessedTime, lastModifiedTime, lastChangedTime);
        }
        throw new ParseException("no file system object", 0); //$NON-NLS-1$
    }

    /**
     * Method that converts to bytes the string representation
     * of a size (10M, 1G, 0K, ...).
     *
     * @param size The size as a string representation
     * @return long The size in bytes
     */
    private static long toBytes(String size) {
        double bytes = Double.parseDouble(size.substring(0, size.length() - 1));
        String unit = size.substring(size.length() - 1);
        if (unit.compareToIgnoreCase("G") == 0) { //$NON-NLS-1$
            return (long)(bytes * 1024 * 1024 * 1024);
        }
        if (unit.compareToIgnoreCase("M") == 0) { //$NON-NLS-1$
            return (long)(bytes * 1024 * 1024);
        }
        if (unit.compareToIgnoreCase("K") == 0) { //$NON-NLS-1$
            return (long)(bytes * 1024);
        }

        //Don't touch
        return (long)bytes;
    }

    /**
     * Method that extract a date from a terse stat ouput.
     *
     * @param stat The terse stat data
     * @param e The position of the date
     * @return Date The date
     */
    private static Date getTerseStatDate(String[] stat, TERSE_STAT_STRUCT e) {
        int cc = stat.length;
        return new Date(
                Long.parseLong(stat[cc - (TERSE_STAT_STRUCT_LENGTH - e.ordinal())]) * 1000L);
    }

    /**
     * Method that extract a integer value from a terse stat ouput.
     *
     * @param stat The terse stat data
     * @param e The position of the date
     * @return int The integer value
     */
    private static int getTerseStatInt(String[] stat, TERSE_STAT_STRUCT e) {
        int cc = stat.length;
        return Integer.parseInt(stat[cc - (TERSE_STAT_STRUCT_LENGTH - e.ordinal())]);
    }

    /**
     * Method that extract a long value from a terse stat ouput.
     *
     * @param stat The terse stat data
     * @param e The position of the date
     * @return long The long value
     */
    private static long getTerseStatLong(String[] stat, TERSE_STAT_STRUCT e) {
        int cc = stat.length;
        return Long.parseLong(stat[cc - (TERSE_STAT_STRUCT_LENGTH - e.ordinal())]);
    }

    /**
     * Method that returns the name of file
     *
     * @param stat The terse stat data
     * @return String The name of file
     */
    private static String getTerseStatName(String[] stat) {
        int cc = stat.length;
        int to = cc - (TERSE_STAT_STRUCT_LENGTH - TERSE_STAT_STRUCT.SIZE.ordinal());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < to; i++) {
            sb.append(stat[i]);
            if (i < to-1) {
                sb.append(" "); //$NON-NLS-1$
            }
        }
        return sb.toString();
    }

    /**
     * Method that retrieve the raw string with the permissions.
     *
     * @param stat The terse stat data
     * @return String The raw string
     */
    private static String getTerseRawPermissions(String[] stat) {
        int cc = stat.length;
        int rawInt = Integer.parseInt(
                stat[cc - (TERSE_STAT_STRUCT_LENGTH - TERSE_STAT_STRUCT.RAW_MODE.ordinal())],16);

        // Extract the type
        char t = RegularFile.UNIX_ID;
        if (RMIHF.S_IFSOCK.mValue == (rawInt & RMIHF.S_IFSOCK.mValue)) {
            t = DomainSocket.UNIX_ID;
        } else if (RMIHF.S_IFLNK.mValue == (rawInt & RMIHF.S_IFLNK.mValue)) {
            t = Symlink.UNIX_ID;
        } else if (RMIHF.S_IFREG.mValue == (rawInt & RMIHF.S_IFREG.mValue)) {
            t = RegularFile.UNIX_ID;
        } else if (RMIHF.S_IFBLK.mValue == (rawInt & RMIHF.S_IFBLK.mValue)) {
            t = BlockDevice.UNIX_ID;
        } else if (RMIHF.S_IFDIR.mValue == (rawInt & RMIHF.S_IFDIR.mValue)) {
            t = Directory.UNIX_ID;
        } else if (RMIHF.S_IFCHR.mValue == (rawInt & RMIHF.S_IFCHR.mValue)) {
            t = CharacterDevice.UNIX_ID;
        } else if (RMIHF.S_IFIFO.mValue == (rawInt & RMIHF.S_IFIFO.mValue)) {
            t = NamedPipe.UNIX_ID;
        }

        // Extract User/Group/Others
        boolean us = RMIHF.S_ISUID.mValue == (rawInt & RMIHF.S_ISUID.mValue);
        boolean ur = RMIHF.S_IRUSR.mValue == (rawInt & RMIHF.S_IRUSR.mValue);
        boolean uw = RMIHF.S_IWUSR.mValue == (rawInt & RMIHF.S_IWUSR.mValue);
        boolean ux = RMIHF.S_IXUSR.mValue == (rawInt & RMIHF.S_IXUSR.mValue);
        boolean gs = RMIHF.S_ISGID.mValue == (rawInt & RMIHF.S_ISGID.mValue);
        boolean gr = RMIHF.S_IRGRP.mValue == (rawInt & RMIHF.S_IRGRP.mValue);
        boolean gw = RMIHF.S_IWGRP.mValue == (rawInt & RMIHF.S_IWGRP.mValue);
        boolean gx = RMIHF.S_IXGRP.mValue == (rawInt & RMIHF.S_IXGRP.mValue);
        boolean os = RMIHF.S_ISVTX.mValue == (rawInt & RMIHF.S_ISVTX.mValue);
        boolean or = RMIHF.S_IROTH.mValue == (rawInt & RMIHF.S_IROTH.mValue);
        boolean ow = RMIHF.S_IWOTH.mValue == (rawInt & RMIHF.S_IWOTH.mValue);
        boolean ox = RMIHF.S_IXOTH.mValue == (rawInt & RMIHF.S_IXOTH.mValue);

        // Build the raw string
        StringBuilder sb = new StringBuilder();
        sb.append(t);
        sb.append(ur ? Permission.READ : Permission.UNASIGNED);
        sb.append(uw ? Permission.WRITE : Permission.UNASIGNED);
        sb.append(us ? (ux ?
                           UserPermission.SETUID_E : UserPermission.SETUID)
                           : (ux ? Permission.EXECUTE : Permission.UNASIGNED));
        sb.append(gr ? Permission.READ : Permission.UNASIGNED);
        sb.append(gw ? Permission.WRITE : Permission.UNASIGNED);
        sb.append(gs ? (gx ?
                            GroupPermission.SETGID_E : GroupPermission.SETGID)
                            : (gx ? Permission.EXECUTE : Permission.UNASIGNED));
        sb.append(or ? Permission.READ : Permission.UNASIGNED);
        sb.append(ow ? Permission.WRITE : Permission.UNASIGNED);
        sb.append(os ? (ox ?
                            OthersPermission.STICKY_E : OthersPermission.STICKY)
                            : (ox ? Permission.EXECUTE : Permission.UNASIGNED));
        return sb.toString();
    }

}
