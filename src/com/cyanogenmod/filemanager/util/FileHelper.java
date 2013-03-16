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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;

import com.cyanogenmod.filemanager.FileManagerApplication;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.commands.shell.ResolveLinkCommand;
import com.cyanogenmod.filemanager.console.ExecutionException;
import com.cyanogenmod.filemanager.model.AID;
import com.cyanogenmod.filemanager.model.BlockDevice;
import com.cyanogenmod.filemanager.model.CharacterDevice;
import com.cyanogenmod.filemanager.model.Directory;
import com.cyanogenmod.filemanager.model.DomainSocket;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.Group;
import com.cyanogenmod.filemanager.model.NamedPipe;
import com.cyanogenmod.filemanager.model.ParentDirectory;
import com.cyanogenmod.filemanager.model.Permissions;
import com.cyanogenmod.filemanager.model.RegularFile;
import com.cyanogenmod.filemanager.model.Symlink;
import com.cyanogenmod.filemanager.model.SystemFile;
import com.cyanogenmod.filemanager.model.User;
import com.cyanogenmod.filemanager.preferences.DisplayRestrictions;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.NavigationSortMode;
import com.cyanogenmod.filemanager.preferences.ObjectIdentifier;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A helper class with useful methods for deal with files.
 */
public final class FileHelper {

    private static final String TAG = "FileHelper"; //$NON-NLS-1$

    /**
     * Special extension for compressed tar files
     */
    private static final String[] COMPRESSED_TAR =
        {
         "tar.gz", "tar.bz2", "tar.lzma" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        };

    /**
     * The root directory.
     * @hide
     */
    public static final String ROOT_DIRECTORY = "/";  //$NON-NLS-1$

    /**
     * The parent directory string.
     * @hide
     */
    public static final String PARENT_DIRECTORY = "..";  //$NON-NLS-1$

    /**
     * The current directory string.
     * @hide
     */
    public static final String CURRENT_DIRECTORY = ".";  //$NON-NLS-1$

    /**
     * The administrator user.
     * @hide
     */
    public static final String USER_ROOT = "root";  //$NON-NLS-1$

    /**
     * The newline string.
     * @hide
     */
    public static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    /**
     * Constructor of <code>FileHelper</code>.
     */
    private FileHelper() {
        super();
    }

    /**
     * Method that check if a file is a symbolic link.
     *
     * @param file File to check
     * @return boolean If file is a symbolic link
     * @throws IOException If real file couldn't be checked
     */
    public static boolean isSymlink(File file) throws IOException {
        return file.getAbsolutePath().compareTo(file.getCanonicalPath()) != 0;
    }

    /**
     * Method that resolves a symbolic link to the real file or directory.
     *
     * @param file File to check
     * @return File The real file or directory
     * @throws IOException If real file couldn't be resolved
     */
    public static File resolveSymlink(File file) throws IOException {
        return file.getCanonicalFile();
    }

    /**
     * Method that returns a more human readable of the size
     * of a file system object.
     *
     * @param fso File system object
     * @return String The human readable size (void if fso don't supports size)
     */
    public static String getHumanReadableSize(FileSystemObject fso) {
        //Only if has size
        if (fso instanceof Directory) {
            return ""; //$NON-NLS-1$
        }
        if (hasSymlinkRef(fso)) {
            if (isSymlinkRefDirectory(fso)) {
                return ""; //$NON-NLS-1$
            }
            return getHumanReadableSize(((Symlink)fso).getLinkRef().getSize());
        }
        return getHumanReadableSize(fso.getSize());
    }

    /**
     * Method that returns a more human readable of a size in bytes.
     *
     * @param size The size in bytes
     * @return String The human readable size
     */
    public static String getHumanReadableSize(long size) {
        Resources res = FileManagerApplication.getInstance().getResources();
        final String format = "%d %s"; //$NON-NLS-1$
        final int[] magnitude = {
                                 R.string.size_bytes,
                                 R.string.size_kilobytes,
                                 R.string.size_megabytes,
                                 R.string.size_gigabytes
                                };

        long aux = size;
        int cc = magnitude.length;
        for (int i = 0; i < cc; i++) {
            long s = aux / 1024;
            if (aux < 1024) {
                return String.format(format, Long.valueOf(aux), res.getString(magnitude[i]));
            }
            aux = s;
        }
        return String.format(
                format, Long.valueOf(aux), res.getString(magnitude[magnitude.length - 1]));
    }

    /**
     * Method that returns if an file system object requires elevated privileges.
     * This occurs when the user is "root" or when the user console doesn't have
     * sufficient permissions over the file system object.
     *
     * @param fso File system object
     * @return boolean If the file system object requires elevated privileges
     */
    public static boolean isPrivileged(FileSystemObject fso) {
        //Parent directory doesn't require privileges
        if (fso instanceof ParentDirectory) {
            return false;
        }

        //Checks if user is the administrator user
        if (fso.getUser().getName().compareTo(USER_ROOT) == 0) {
            return true;
        }

        //No privileged
        return false;
    }

    /**
     * Method that returns if the file system object if the root directory.
     *
     * @param fso The file system object to check
     * @return boolean if the file system object if the root directory
     */
    public static boolean isRootDirectory(FileSystemObject fso) {
        if (fso.getName() == null) return true;
        return fso.getName().compareTo(FileHelper.ROOT_DIRECTORY) == 0;
    }

    /**
     * Method that returns if the folder if the root directory.
     *
     * @param folder The folder
     * @return boolean if the folder if the root directory
     */
    public static boolean isRootDirectory(String folder) {
        if (folder == null) return true;
        return isRootDirectory(new File(folder));
    }

    /**
     * Method that returns if the folder if the root directory.
     *
     * @param folder The folder
     * @return boolean if the folder if the root directory
     */
    public static boolean isRootDirectory(File folder) {
        if (folder.getPath() == null) return true;
        return folder.getPath().compareTo(FileHelper.ROOT_DIRECTORY) == 0;
    }

    /**
     * Method that returns if the parent file system object if the root directory.
     *
     * @param fso The parent file system object to check
     * @return boolean if the parent file system object if the root directory
     */
    public static boolean isParentRootDirectory(FileSystemObject fso) {
        if (fso.getParent() == null) return true;
        return fso.getParent().compareTo(FileHelper.ROOT_DIRECTORY) == 0;
    }

    /**
     * Method that returns the name without the extension of a file system object.
     *
     * @param fso The file system object
     * @return The name without the extension of the file system object.
     */
    public static String getName(FileSystemObject fso) {
        return getName(fso.getName());
    }

    /**
     * Method that returns the name without the extension of a file system object.
     *
     * @param name The name of file system object
     * @return The name without the extension of the file system object.
     */
    public static String getName(String name) {
       String ext = getExtension(name);
       if (ext == null) return name;
       return name.substring(0, name.length() - ext.length() - 1);
    }

    /**
     * Method that returns the extension of a file system object.
     *
     * @param fso The file system object
     * @return The extension of the file system object, or <code>null</code>
     * if <code>fso</code> has no extension.
     */
    public static String getExtension(FileSystemObject fso) {
        return getExtension(fso.getName());
    }

    /**
     * Method that returns the extension of a file system object.
     *
     * @param name The name of file system object
     * @return The extension of the file system object, or <code>null</code>
     * if <code>fso</code> has no extension.
     */
    public static String getExtension(String name) {
        final char dot = '.';
        int pos = name.lastIndexOf(dot);
        if (pos == -1 || pos == 0) { // Hidden files doesn't have extensions
            return null;
        }

        // Exceptions to the general extraction method
        int cc = COMPRESSED_TAR.length;
        for (int i = 0; i < cc; i++) {
            if (name.endsWith("." + COMPRESSED_TAR[i])) { //$NON-NLS-1$
                return COMPRESSED_TAR[i];
            }
        }

        // General extraction method
        return name.substring(pos + 1);
    }

    /**
     * Method that returns the parent directory of a file/folder
     *
     * @param path The file/folder
     * @return String The parent directory
     */
    public static String getParentDir(String path) {
        return getParentDir(new File(path));
    }

    /**
     * Method that returns the parent directory of a file/folder
     *
     * @param path The file/folder
     * @return String The parent directory
     */
    public static String getParentDir(File path) {
        String parent = path.getParent();
        if (parent == null && path.getAbsolutePath().compareTo(FileHelper.ROOT_DIRECTORY) != 0) {
            parent = FileHelper.ROOT_DIRECTORY;
        }
        return parent;
    }

    /**
     * Method that evaluates if a path is relative.
     *
     * @param src The path to check
     * @return boolean If a path is relative
     */
    public static boolean isRelativePath(String src) {
        if (src.startsWith(CURRENT_DIRECTORY + File.separator)) {
            return true;
        }
        if (src.startsWith(PARENT_DIRECTORY + File.separator)) {
            return true;
        }
        if (src.indexOf(File.separator + CURRENT_DIRECTORY + File.separator) != -1) {
            return true;
        }
        if (src.indexOf(File.separator + PARENT_DIRECTORY + File.separator) != -1) {
            return true;
        }
        if (!src.startsWith(ROOT_DIRECTORY)) {
            return true;
        }
        return false;
    }

    /**
     * Method that check if the file system object is a {@link Symlink} and
     * has a link reference.
     *
     * @param fso The file system object to check
     * @return boolean If file system object the has a link reference
     */
    public static boolean hasSymlinkRef(FileSystemObject fso) {
        if (fso instanceof Symlink) {
            return ((Symlink)fso).getLinkRef() != null;
        }
        return false;
    }

    /**
     * Method that check if the file system object is a {@link Symlink} and
     * the link reference is a directory.
     *
     * @param fso The file system object to check
     * @return boolean If file system object the link reference is a directory
     */
    public static boolean isSymlinkRefDirectory(FileSystemObject fso) {
        if (!hasSymlinkRef(fso)) {
            return false;
        }
        return ((Symlink)fso).getLinkRef() instanceof Directory;
    }

    /**
     * Method that check if the file system object is a {@link Symlink} and
     * the link reference is a system file.
     *
     * @param fso The file system object to check
     * @return boolean If file system object the link reference is a system file
     */
    public static boolean isSymlinkRefSystemFile(FileSystemObject fso) {
        if (!hasSymlinkRef(fso)) {
            return false;
        }
        return ((Symlink)fso).getLinkRef() instanceof SystemFile;
    }

    /**
     * Method that check if the file system object is a {@link Symlink} and
     * the link reference is a block device.
     *
     * @param fso The file system object to check
     * @return boolean If file system object the link reference is a block device
     */
    public static boolean isSymlinkRefBlockDevice(FileSystemObject fso) {
        if (!hasSymlinkRef(fso)) {
            return false;
        }
        return ((Symlink)fso).getLinkRef() instanceof BlockDevice;
    }

    /**
     * Method that check if the file system object is a {@link Symlink} and
     * the link reference is a character device.
     *
     * @param fso The file system object to check
     * @return boolean If file system object the link reference is a character device
     */
    public static boolean isSymlinkRefCharacterDevice(FileSystemObject fso) {
        if (!hasSymlinkRef(fso)) {
            return false;
        }
        return ((Symlink)fso).getLinkRef() instanceof CharacterDevice;
    }

    /**
     * Method that check if the file system object is a {@link Symlink} and
     * the link reference is a named pipe.
     *
     * @param fso The file system object to check
     * @return boolean If file system object the link reference is a named pipe
     */
    public static boolean isSymlinkRefNamedPipe(FileSystemObject fso) {
        if (!hasSymlinkRef(fso)) {
            return false;
        }
        return ((Symlink)fso).getLinkRef() instanceof NamedPipe;
    }

    /**
     * Method that check if the file system object is a {@link Symlink} and
     * the link reference is a domain socket.
     *
     * @param fso The file system object to check
     * @return boolean If file system object the link reference is a domain socket
     */
    public static boolean isSymlinkRefDomainSocket(FileSystemObject fso) {
        if (!hasSymlinkRef(fso)) {
            return false;
        }
        return ((Symlink)fso).getLinkRef() instanceof DomainSocket;
    }

    /**
     * Method that checks if a file system object is a directory (real o symlink).
     *
     * @param fso The file system object to check
     * @return boolean If file system object is a directory
     */
    public static boolean isDirectory(FileSystemObject fso) {
        if (fso instanceof Directory) {
            return true;
        }
        if (isSymlinkRefDirectory(fso)) {
            return true;
        }
        return false;
    }

    /**
     * Method that checks if a file system object is a system file (real o symlink).
     *
     * @param fso The file system object to check
     * @return boolean If file system object is a system file
     */
    public static boolean isSystemFile(FileSystemObject fso) {
        if (fso instanceof SystemFile) {
            return true;
        }
        if (isSymlinkRefSystemFile(fso)) {
            return true;
        }
        return false;
    }

    /**
     * Method that returns the real reference of a file system object
     * (the reference file system object if the file system object is a symlink.
     * Otherwise the same reference).
     *
     * @param fso The file system object to check
     * @return FileSystemObject The real file system object reference
     */
    public static FileSystemObject getReference(FileSystemObject fso) {
        if (hasSymlinkRef(fso)) {
            return ((Symlink)fso).getLinkRef();
        }
        return fso;
    }

    /**
     * Method that applies the configuration modes to the listed files
     * (sort mode, hidden files, ...).
     *
     * @param files The listed files
     * @param restrictions The restrictions to apply when displaying files
     * @param chRooted If app run with no privileges
     * @return List<FileSystemObject> The applied mode listed files
     */
    public static List<FileSystemObject> applyUserPreferences(
                    List<FileSystemObject> files, Map<DisplayRestrictions,
                    Object> restrictions, boolean chRooted) {
        return applyUserPreferences(files, restrictions, false, chRooted);
    }

    /**
     * Method that applies the configuration modes to the listed files
     * (sort mode, hidden files, ...).
     *
     * @param files The listed files
     * @param restrictions The restrictions to apply when displaying files
     * @param noSort If sort must be applied
     * @param chRooted If app run with no privileges
     * @return List<FileSystemObject> The applied mode listed files
     */
    public static List<FileSystemObject> applyUserPreferences(
            List<FileSystemObject> files, Map<DisplayRestrictions, Object> restrictions,
            boolean noSort, boolean chRooted) {
        //Retrieve user preferences
        SharedPreferences prefs = Preferences.getSharedPreferences();
        FileManagerSettings sortModePref = FileManagerSettings.SETTINGS_SORT_MODE;
        FileManagerSettings showDirsFirstPref = FileManagerSettings.SETTINGS_SHOW_DIRS_FIRST;
        FileManagerSettings showHiddenPref = FileManagerSettings.SETTINGS_SHOW_HIDDEN;
        FileManagerSettings showSystemPref = FileManagerSettings.SETTINGS_SHOW_SYSTEM;
        FileManagerSettings showSymlinksPref = FileManagerSettings.SETTINGS_SHOW_SYMLINKS;

        //Remove all unnecessary files (no required by the user)
        int cc = files.size();
        for (int i = cc - 1; i >= 0; i--) {
            FileSystemObject file = files.get(i);

            //Hidden files
            if (!prefs.getBoolean(
                    showHiddenPref.getId(),
                    ((Boolean)showHiddenPref.getDefaultValue()).booleanValue()) || chRooted) {
                if (file.isHidden()) {
                    files.remove(i);
                    continue;
                }
            }

            //System files
            if (!prefs.getBoolean(
                    showSystemPref.getId(),
                    ((Boolean)showSystemPref.getDefaultValue()).booleanValue()) || chRooted) {
                if (file instanceof SystemFile) {
                    files.remove(i);
                    continue;
                }
            }

            //Symlinks files
            if (!prefs.getBoolean(
                    showSymlinksPref.getId(),
                    ((Boolean)showSymlinksPref.getDefaultValue()).booleanValue()) || chRooted) {
                if (file instanceof Symlink) {
                    files.remove(i);
                    continue;
                }
            }

            // Restrictions (only apply to files)
            if (restrictions != null) {
                if (!isDirectory(file)) {
                    if (!isDisplayAllowed(file, restrictions)) {
                        files.remove(i);
                        continue;
                    }
                }
            }
        }

        //Apply sort mode
        if (!noSort) {
            final boolean showDirsFirst =
                    prefs.getBoolean(
                            showDirsFirstPref.getId(),
                        ((Boolean)showDirsFirstPref.getDefaultValue()).booleanValue());
            final NavigationSortMode sortMode =
                    NavigationSortMode.fromId(
                            prefs.getInt(sortModePref.getId(),
                            ((ObjectIdentifier)sortModePref.getDefaultValue()).getId()));
            Collections.sort(files, new Comparator<FileSystemObject>() {
                @Override
                public int compare(FileSystemObject lhs, FileSystemObject rhs) {
                    //Parent directory always goes first
                    boolean isLhsParentDirectory = lhs instanceof ParentDirectory;
                    boolean isRhsParentDirectory = rhs instanceof ParentDirectory;
                    if (isLhsParentDirectory || isRhsParentDirectory) {
                        if (isLhsParentDirectory && isRhsParentDirectory) {
                            return 0;
                        }
                        return (isLhsParentDirectory) ? -1 : 1;
                    }

                    //Need to sort directory first?
                    if (showDirsFirst) {
                        boolean isLhsDirectory = FileHelper.isDirectory(lhs);
                        boolean isRhsDirectory = FileHelper.isDirectory(rhs);
                        if (isLhsDirectory || isRhsDirectory) {
                            if (isLhsDirectory && isRhsDirectory) {
                                //Apply sort mode
                                return FileHelper.doCompare(lhs, rhs, sortMode);
                            }
                            return (isLhsDirectory) ? -1 : 1;
                        }
                    }

                    //Apply sort mode
                    return FileHelper.doCompare(lhs, rhs, sortMode);
                }

            });
        }

        //Return the files
        return files;
    }

    /**
     * Method that check if a file should be displayed according to the restrictions
     *
     * @param fso The file system object to check
     * @param restrictions The restrictions map
     * @return boolean If the file should be displayed
     */
    private static boolean isDisplayAllowed(
            FileSystemObject fso, Map<DisplayRestrictions, Object> restrictions) {
        Iterator<DisplayRestrictions> it = restrictions.keySet().iterator();
        while (it.hasNext()) {
            DisplayRestrictions restriction = it.next();
            Object value = restrictions.get(restriction);
            if (value == null) {
                continue;
            }
            switch (restriction) {
                case CATEGORY_TYPE_RESTRICTION:
                    if (value instanceof MimeTypeCategory) {
                        MimeTypeCategory cat1 = (MimeTypeCategory)value;
                        // NOTE: We don't need the context here, because mime-type
                        // database should be loaded prior to this call
                        MimeTypeCategory cat2 = MimeTypeHelper.getCategory(null, fso);
                        if (cat1.compareTo(cat2) != 0) {
                            return false;
                        }
                    }
                    break;

                case MIME_TYPE_RESTRICTION:
                    if (value instanceof String) {
                        String mimeType = (String)value;
                        if (mimeType.compareTo(MimeTypeHelper.ALL_MIME_TYPES) != 0) {
                            // NOTE: We don't need the context here, because mime-type
                            // database should be loaded prior to this call
                            if (!MimeTypeHelper.matchesMimeType(null, fso, mimeType)) {
                                return false;
                            }
                        }
                    }
                    break;

                case SIZE_RESTRICTION:
                    if (value instanceof Long) {
                        Long maxSize = (Long)value;
                        if (fso.getSize() > maxSize.longValue()) {
                            return false;
                        }
                    }
                    break;

                case DIRECTORY_ONLY_RESTRICTION:
                    if (value instanceof Boolean) {
                        Boolean directoryOnly = (Boolean) value;
                        if (directoryOnly.booleanValue() && !FileHelper.isDirectory(fso)) {
                            return false;
                        }
                    }
                    break;

                case LOCAL_FILESYSTEM_ONLY_RESTRICTION:
                    if (value instanceof Boolean) {
                        Boolean localOnly = (Boolean)value;
                        if (localOnly.booleanValue()) {
                            /** TODO Needed when CMFM gets networking **/
                        }
                    }
                    break;

                default:
                    break;
            }
        }
        return true;
    }

    /**
     * Method that resolve the symbolic links of the list of files passed as argument.<br />
     * This method invokes the {@link ResolveLinkCommand} in those files that hasn't a valid
     * symlink reference
     *
     * @param context The current context
     * @param files The listed files
     */
    public static void resolveSymlinks(Context context, List<FileSystemObject> files) {
        int cc = files.size();
        for (int i = 0; i < cc; i++) {
            FileSystemObject fso = files.get(i);
            if (fso instanceof Symlink && ((Symlink)fso).getLinkRef() == null) {
                try {
                    FileSystemObject symlink =
                            CommandHelper.resolveSymlink(context, fso.getFullPath(), null);
                    ((Symlink)fso).setLinkRef(symlink);
                } catch (Throwable ex) {/**NON BLOCK**/}
            }
        }
    }

    /**
     * Method that do a comparison between 2 file system objects.
     *
     * @param fso1 The first file system objects
     * @param fso2 The second file system objects
     * @param mode The sort mode
     * @return int a negative integer if {@code fso1} is less than {@code fso2};
     *         a positive integer if {@code fso1} is greater than {@code fso2};
     *         0 if {@code fso1} has the same order as {@code fso2}.
     */
    public static int doCompare(
            final FileSystemObject fso1,
            final FileSystemObject fso2,
            final NavigationSortMode mode) {

        // Retrieve the user preference for case sensitive sort
        boolean caseSensitive =
                Preferences.getSharedPreferences().
                    getBoolean(
                        FileManagerSettings.SETTINGS_CASE_SENSITIVE_SORT.getId(),
                        ((Boolean)FileManagerSettings.SETTINGS_CASE_SENSITIVE_SORT.
                                getDefaultValue()).booleanValue());

        //Name (ascending)
        if (mode.getId() == NavigationSortMode.NAME_ASC.getId()) {
            if (!caseSensitive) {
                return fso1.getName().compareToIgnoreCase(fso2.getName());
            }
            return fso1.getName().compareTo(fso2.getName());
        }
        //Name (descending)
        if (mode.getId() == NavigationSortMode.NAME_DESC.getId()) {
            if (!caseSensitive) {
                return fso1.getName().compareToIgnoreCase(fso2.getName()) * -1;
            }
            return fso1.getName().compareTo(fso2.getName()) * -1;
        }

        //Date (ascending)
        if (mode.getId() == NavigationSortMode.DATE_ASC.getId()) {
            return fso1.getLastModifiedTime().compareTo(fso2.getLastModifiedTime());
        }
        //Date (descending)
        if (mode.getId() == NavigationSortMode.DATE_DESC.getId()) {
            return fso1.getLastModifiedTime().compareTo(fso2.getLastModifiedTime()) * -1;
        }

        //Comparison between files directly
        return fso1.compareTo(fso2);
    }

    /**
     * Method that add to the path the trailing slash
     *
     * @param path The path
     * @return String The path with the trailing slash
     */
    public static String addTrailingSlash(String path) {
        if (path == null) return null;
        return path.endsWith(File.separator) ? path : path + File.separator;
    }

    /**
     * Method that cleans the path and removes the trailing slash
     *
     * @param path The path to clean
     * @return String The path without the trailing slash
     */
    public static String removeTrailingSlash(String path) {
        if (path == null) return null;
        if (path.trim().compareTo(ROOT_DIRECTORY) == 0) return path;
        if (path.endsWith(File.separator)) {
            return path.substring(0, path.length()-1);
        }
        return path;
    }

    /**
     * Method that creates a new name based on the name of the {@link FileSystemObject}
     * that is not current used by the filesystem.
     *
     * @param ctx The current context
     * @param files The list of files of the current directory
     * @param attemptedName The attempted name
     * @param regexp The resource of the regular expression to create the new name
     * @return String The new non-existing name
     */
    public static String createNonExistingName(
            final Context ctx, final List<FileSystemObject> files,
            final String attemptedName, int regexp) {
        // Find a non-exiting name
        String newName = attemptedName;
        if (!isNameExists(files, newName)) return newName;
        do {
            String name  = FileHelper.getName(newName);
            String ext  = FileHelper.getExtension(newName);
            if (ext == null) {
                ext = ""; //$NON-NLS-1$
            } else {
                ext = String.format(".%s", ext); //$NON-NLS-1$
            }
            newName = ctx.getString(regexp, name, ext);
        } while (isNameExists(files, newName));
        return newName;
    }

    /**
     * Method that checks if a name exists in the current directory.
     *
     * @param files The list of files of the current directory
     * @param name The name to check
     * @return boolean Indicate if the name exists in the current directory
     */
    public static boolean isNameExists(List<FileSystemObject> files, String name) {
        //Verify if the name exists in the current file list
        int cc = files.size();
        for (int i = 0; i < cc; i++) {
            FileSystemObject fso = files.get(i);
            if (fso.getName().compareTo(name) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method that returns is a {@link FileSystemObject} can be handled by this application
     * allowing the uncompression of the file
     *
     * @param fso The file system object to verify
     * @return boolean If the file is supported
     */
    @SuppressWarnings("nls")
    public static boolean isSupportedUncompressedFile(FileSystemObject fso) {
        // Valid uncompressed formats are:
        final String[] VALID =
                {
                    "tar", "tgz", "tar.gz", "tar.bz2", "tar.lzma",
                    "zip", "gz", "bz2", "lzma", "xz", "Z", "rar"
                };
        // Null values for required commands
        final String[] OPT_KEYS =
                {
                    null, null, null, null, null,
                    "unzip", null, null, "unlzma", "unxz", "uncompress", "unrar"
                };

        // Check that have a valid file
        if (fso == null) return false;

        //Only regular files
        if (isDirectory(fso) || fso instanceof Symlink) {
            return false;
        }
        String ext = getExtension(fso);
        if (ext != null) {
            int cc = VALID.length;
            for (int i = 0; i < cc; i++) {
                if (VALID[i].compareToIgnoreCase(ext) == 0) {
                    // Is the command present
                    if (OPT_KEYS[i] != null &&
                        FileManagerApplication.hasOptionalCommand(OPT_KEYS[i])) {
                        return true;
                    }
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Method that converts an absolute path to a relative path
     *
     * @param path The absolute path to convert
     * @param relativeTo The absolute path from which make path relative to (a folder)
     * @return String The relative path
     */
    public static String toRelativePath(String path, String relativeTo) {
        // Normalize the paths
        File f1 = new File(path);
        File f2 = new File(relativeTo);
        String s1 = f1.getAbsolutePath();
        String s2 = f2.getAbsolutePath();
        if (!s2.endsWith(File.separator)) {
            s2 = s2 + File.separator;
        }

        // If s2 contains s1 then the relative is replace of the start of the path
        if (s1.startsWith(s2)) {
            return s1.substring(s2.length());
        }

        StringBuffer relative = new StringBuffer();
        do {
            File f3 = new File(s2);
            relative.append(String.format("..%s", File.separator)); //$NON-NLS-1$
            s2 = f3.getParent() + File.separator;
        } while (!s1.startsWith(s2) && !s1.startsWith(new File(s2).getAbsolutePath()));
        s2 = new File(s2).getAbsolutePath();
        return relative.toString() + s1.substring(s2.length());
    }

    /**
     * Method that creates a {@link FileSystemObject} from a {@link File}
     *
     * @param file The file or folder reference
     * @return FileSystemObject The file system object reference
     */
    public static FileSystemObject createFileSystemObject(File file) {
        try {
            // The user and group name of the files. In ChRoot, aosp give restrict access to
            // this user and group.
            final String USER = "system"; //$NON-NLS-1$
            final String GROUP = "sdcard_r"; //$NON-NLS-1$
            final String PERMISSIONS = "----rwxr-x"; //$NON-NLS-1$

            // The user and group name of the files. In ChRoot, aosp give restrict access to
            // this user and group. This applies for permission also. This has no really much
            // interest if we not allow to change the permissions
            AID userAID = AIDHelper.getAIDFromName(USER);
            AID groupAID = AIDHelper.getAIDFromName(GROUP);
            User user = new User(userAID.getId(), userAID.getName());
            Group group = new Group(groupAID.getId(), groupAID.getName());
            Permissions perm = Permissions.fromRawString(PERMISSIONS);

            // Build a directory?
            Date lastModified = new Date(file.lastModified());
            if (file.isDirectory()) {
                return
                    new Directory(
                            file.getName(),
                            file.getParent(),
                            user, group, perm,
                            lastModified, lastModified, lastModified); // The only date we have
            }

            // Build a regular file
            return
                new RegularFile(
                        file.getName(),
                        file.getParent(),
                        user, group, perm,
                        file.length(),
                        lastModified, lastModified, lastModified); // The only date we have
        } catch (Exception e) {
            Log.e(TAG, "Exception retrieving the fso", e); //$NON-NLS-1$
        }
        return null;
    }

    /**
     * Method that copies recursively to the destination
     *
     * @param src The source file or folder
     * @param dst The destination file or folder
     * @param bufferSize The buffer size for the operation
     * @return boolean If the operation complete successfully
     * @throws ExecutionException If a problem was detected in the operation
     */
    public static boolean copyRecursive(
            final File src, final File dst, int bufferSize) throws ExecutionException {
        if (src.isDirectory()) {
            // Create the directory
            if (dst.exists() && !dst.isDirectory()) {
                Log.e(TAG,
                        String.format("Failed to check destionation dir: %s", dst)); //$NON-NLS-1$
                throw new ExecutionException("the path exists but is not a folder"); //$NON-NLS-1$
            }
            if (!dst.exists()) {
                if (!dst.mkdir()) {
                    Log.e(TAG, String.format("Failed to create directory: %s", dst)); //$NON-NLS-1$
                    return false;
                }
            }
            File[] files = src.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (!copyRecursive(files[i], new File(dst, files[i].getName()), bufferSize)) {
                        return false;
                    }
                }
            }
        } else {
            // Copy the directory
            if (!bufferedCopy(src, dst,bufferSize)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Method that copies a file
     *
     * @param src The source file
     * @param dst The destination file
     * @param bufferSize The buffer size for the operation
     * @return boolean If the operation complete successfully
     */
    public static boolean bufferedCopy(final File src, final File dst, int bufferSize) {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(src), bufferSize);
            bos = new BufferedOutputStream(new FileOutputStream(dst), bufferSize);
            int read = 0;
            byte[] data = new byte[bufferSize];
            while ((read = bis.read(data, 0, bufferSize)) != -1) {
                bos.write(data, 0, read);
            }
            return true;

        } catch (Throwable e) {
            Log.e(TAG,
                    String.format(TAG, "Failed to copy from %s to %d", src, dst), e); //$NON-NLS-1$
            return false;
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
            } catch (Throwable e) {/**NON BLOCK**/}
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch (Throwable e) {/**NON BLOCK**/}
        }
    }

    /**
     * Method that deletes a folder recursively
     *
     * @param folder The folder to delete
     * @return boolean If the folder was deleted
     */
    public static boolean deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    if (!deleteFolder(files[i])) {
                        return false;
                    }
                } else {
                    if (!files[i].delete()) {
                        return false;
                    }
                }
            }
        }
        return folder.delete();
    }

    /**
     * Method that returns the canonical/absolute path of the path.<br/>
     * This method performs path resolution
     *
     * @param path The path to convert
     * @return String The canonical/absolute path
     */
    public static String getAbsPath(String path) {
        try {
            return new File(path).getCanonicalPath();
        } catch (Exception e) {
            return new File(path).getAbsolutePath();
        }
    }

    /**
     * Method that returns the .nomedia file
     *
     * @param fso The folder that contains the .nomedia file
     * @return File The .nomedia file
     */
    public static File getNoMediaFile(FileSystemObject fso) {
        File file = null;
        try {
            file = new File(fso.getFullPath()).getCanonicalFile();
        } catch (Exception e) {
            file = new File(fso.getFullPath()).getAbsoluteFile();
        }
        return new File(file, ".nomedia").getAbsoluteFile(); //$NON-NLS-1$
    }
}
