/*
 * Copyright (C) 20124 The CyanogenMod Project
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

package com.cyanogenmod.filemanager.console;

import android.content.Context;
import android.os.Environment;
import android.os.SystemClock;

import com.cyanogenmod.filemanager.FileManagerApplication;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.console.secure.SecureConsole;
import com.cyanogenmod.filemanager.model.Directory;
import com.cyanogenmod.filemanager.model.DiskUsage;
import com.cyanogenmod.filemanager.model.Identity;
import com.cyanogenmod.filemanager.model.MountPoint;
import com.cyanogenmod.filemanager.model.Permissions;
import com.cyanogenmod.filemanager.preferences.AccessMode;
import com.cyanogenmod.filemanager.util.AIDHelper;
import com.cyanogenmod.filemanager.util.FileHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * An abstract base class for of a {@link VirtualConsole} that has a virtual mount point
 * in the filesystem.
 */
public abstract class VirtualMountPointConsole extends VirtualConsole {

    private static final String DEFAULT_STORAGE_NAME = "storage";

//    private static File sVirtualStorageDir;

    private static List<VirtualMountPointConsole> sVirtualConsoles;
    private static Identity sVirtualIdentity;
    private static Permissions sVirtualFolderPermissions;

    public VirtualMountPointConsole(Context ctx) {
        super(ctx);
    }

    /**
     * Should return the name of the mount point name
     *
     * @return String The name of the mount point name of this console.
     */
    public abstract String getMountPointName();

    /**
     * Method that returns if the console is secure
     *
     * @return boolean If the console is secure
     */
    public abstract boolean isSecure();

    /**
     * Method that returns if the console is remote
     *
     * @return boolean If the console is remote
     */
    public abstract boolean isRemote();

    /**
     * Method that returns if the console is mounted
     *
     * @return boolean If the console is mounted
     */
    public abstract boolean isMounted();

    /**
     * Method that unmounts the filesystem
     *
     * @return boolean If the filesystem was unmounted
     */
    public abstract boolean unmount();

    /**
     * Returns the mountpoints for the console
     *
     * @return List<MountPoint> The list of mountpoints handled by the console
     */
    public abstract List<MountPoint> getMountPoints();

    /**
     * Returns the disk usage of every mountpoint for the console
     *
     * @return List<DiskUsage> The list of disk usage of the mountpoints handled by the console
     */
    public abstract List<DiskUsage> getDiskUsage();

    /**
     * Returns the disk usage of the path
     *
     * @param path The path to check
     * @return DiskUsage The disk usage for the passed path
     */
    public abstract DiskUsage getDiskUsage(String path);

    /**
     * Method that register all the implemented virtual consoles. This method should
     * be called only once on the application instantiation.
     *
     * @param context The current context
     */
    public static void registerVirtualConsoles(Context context) {
        if (sVirtualConsoles != null) return;
        sVirtualConsoles = new ArrayList<VirtualMountPointConsole>();
        sVirtualIdentity = AIDHelper.createVirtualIdentity();
        sVirtualFolderPermissions = Permissions.createDefaultFolderPermissions();

        int bufferSize = context.getResources().getInteger(R.integer.buffer_size);

        // Register every known virtual mountable console
        sVirtualConsoles.add(SecureConsole.getInstance(context, bufferSize));
        // TODO Add remote consoles. Not ready for now.
        // sVirtualConsoles.add(new RemoteConsole(context));
    }

    /**
     * Method that returns the virtual storage directory
     * @return
     */
    private static File getVirtualStorageDir() {
        final Context context = FileManagerApplication.getInstance().getApplicationContext();
        File dir = new File(context.getString(R.string.virtual_storage_dir));
        AccessMode mode = FileManagerApplication.getAccessMode();
        if (mode.equals(AccessMode.SAFE) || !dir.isDirectory()) {
            // Chroot environment (create a folder inside the external storage)
            return getChrootedVirtualStorageDir();
        }
        return dir;
    }

    /**
     * Method that returns the chrooted virtual storage directory
     *
     * @return File The Virtual storage directory
     */
    private static File getChrootedVirtualStorageDir() {
        File root = new File(Environment.getExternalStorageDirectory(), DEFAULT_STORAGE_NAME);
        root.mkdir();
        return root;
    }

    /**
     * Method that list all the virtual directories
     *
     * @return List<Directory> The list of virtual directories
     */
    public static List<Directory> getVirtualMountableDirectories() {
        final Date date = new Date(System.currentTimeMillis() - SystemClock.elapsedRealtime());
        List<Directory> directories = new ArrayList<Directory>();
        for (VirtualMountPointConsole console : sVirtualConsoles) {
            File dir = null;
            do {
                dir = console.getVirtualMountPoint();
            } while (dir.getParentFile() != null && !isVirtualStorageDir(dir.getParent()));

            if (dir != null) {
                Directory directory = new Directory(
                        dir.getName(),
                        getVirtualStorageDir().getAbsolutePath(),
                        sVirtualIdentity.getUser(),
                        sVirtualIdentity.getGroup(),
                        sVirtualFolderPermissions,
                        date, date, date);
                directory.setSecure(console.isSecure());
                directory.setRemote(console.isRemote());

                if (!directories.contains(directory)) {
                    directories.add(directory);
                }
            }
        }
        return directories;
    }

    /**
     * Method that returns the virtual mountpoints of every register console
     * @return
     */
    public static List<MountPoint> getVirtualMountPoints() {
        List<MountPoint> mountPoints = new ArrayList<MountPoint>();
        for (VirtualMountPointConsole console : sVirtualConsoles) {
            mountPoints.addAll(console.getMountPoints());
        }
        return mountPoints;
    }

    /**
     * Method that returns the virtual disk usage of the mountpoints of every register console
     * @return
     */
    public static List<DiskUsage> getVirtualDiskUsage() {
        List<DiskUsage> diskUsage = new ArrayList<DiskUsage>();
        for (VirtualMountPointConsole console : sVirtualConsoles) {
            diskUsage.addAll(console.getDiskUsage());
        }
        return diskUsage;
    }

    /**
     * Returns if the passed directory is the current virtual storage directory
     *
     * @param directory The directory to check
     * @return boolean If is the current virtual storage directory
     */
    public static boolean isVirtualStorageDir(String directory) {
        return getVirtualStorageDir().equals(new File(directory));
    }

    /**
     * Returns if the passed resource belongs to a virtual filesystem
     *
     * @param path The path to check
     * @return boolean If is the resource belongs to a virtual filesystem
     */
    public static boolean isVirtualStorageResource(String path) {
        for (VirtualMountPointConsole console : sVirtualConsoles) {
            if (FileHelper.belongsToDirectory(new File(path), console.getVirtualMountPoint())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method that returns the virtual console for the path or null if the path
     * is not a virtual filesystem
     *
     * @param path the path to check
     * @return VirtualMountPointConsole The found console
     */
    public static VirtualMountPointConsole getVirtualConsoleForPath(String path) {
        File file = new File(path);
        for (VirtualMountPointConsole console : sVirtualConsoles) {
            if (FileHelper.belongsToDirectory(file, console.getVirtualMountPoint())) {
                return console;
            }
        }
        return null;
    }

    public static List<Console> getVirtualConsoleForSearchPath(String path) {
        List<Console> consoles = new ArrayList<Console>();
        File dir = new File(path);
        for (VirtualMountPointConsole console : sVirtualConsoles) {
            if (FileHelper.belongsToDirectory(console.getVirtualMountPoint(), dir)) {
                // Only mount consoles can participate in the search
                if (console.isMounted()) {
                    consoles.add(console);
                }
            }
        }
        return consoles;
    }

    /**
     * Returns if the passed directory is the virtual mountpoint directory of the virtual console
     *
     * @param directory The directory to check
     * @return boolean If is the virtual mountpoint directory of the virtual console
     */
    public boolean isVirtualMountPointDir(String directory) {
        return getVirtualMountPoint().equals(new File(directory));
    }

    /**
     * Method that returns the virtual mount point for this console
     *
     * @return String The virtual mount point
     */
    public final File getVirtualMountPoint() {
        return new File(getVirtualStorageDir(), getMountPointName());
    }
}
