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

package com.cyanogenmod.filemanager.model;

import com.cyanogenmod.filemanager.util.ParseHelper;

import java.io.Serializable;
import java.text.ParseException;

/**
 * Permissions of a filesystem object.
 *
 * @see Permission
 * @see FileSystemObject
 */
public class Permissions implements Serializable, Comparable<Permissions> {

    private static final long serialVersionUID = -3995246732859872806L;

    private UserPermission mUser;
    private GroupPermission mGroup;
    private OthersPermission mOthers;

    /**
     * Constructor of <code>Permissions</code>.
     *
     * @param user The permissions for the proprietary user of the filesystem object
     * @param group The permissions for the proprietary group of the filesystem object
     * @param others The permissions for the non proprietary users of the filesystem object
     */
    public Permissions(UserPermission user, GroupPermission group, OthersPermission others) {
        super();
        this.mUser = user;
        this.mGroup = group;
        this.mOthers = others;
    }

    /**
     * Method that returns the permissions for the proprietary user of the filesystem object.
     *
     * @return UserPermission The permissions for the proprietary user of the filesystem object
     */
    public UserPermission getUser() {
        return this.mUser;
    }

    /**
     * Method that returns the permissions for the proprietary user of the filesystem object.
     *
     * @param user The permissions for the proprietary user of the filesystem object
     */
    public void setUser(UserPermission user) {
        this.mUser = user;
    }

    /**
     * Method that returns the permissions for the proprietary group of the filesystem object.
     *
     * @return GroupPermission The permissions for the proprietary group of the filesystem object
     */
    public GroupPermission getGroup() {
        return this.mGroup;
    }

    /**
     * Method that returns the permissions for the proprietary group of the filesystem object.
     *
     * @param group The permissions for the proprietary group of the filesystem object
     */
    public void setGroup(GroupPermission group) {
        this.mGroup = group;
    }

    /**
     * Method that returns the permissions for the non proprietary users of the filesystem object.
     *
     * @return Permission The permissions for the non proprietary users of the filesystem object
     */
    public OthersPermission getOthers() {
        return this.mOthers;
    }

    /**
     * Method that returns the permissions for the non proprietary users of the filesystem object.
     *
     * @param others The permissions for the non proprietary users of the filesystem object
     */
    public void setOthers(OthersPermission others) {
        this.mOthers = others;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Permissions another) {
        String o1 = this.toRawString();
        String o2 = another.toRawString();
        return o1.compareTo(o2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.mGroup == null) ? 0 : this.mGroup.hashCode());
        result = prime * result + ((this.mOthers == null) ? 0 : this.mOthers.hashCode());
        result = prime * result + ((this.mUser == null) ? 0 : this.mUser.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Permissions other = (Permissions) obj;
        if (this.mGroup == null) {
            if (other.mGroup != null) {
                return false;
            }
        } else if (!this.mGroup.equals(other.mGroup)) {
            return false;
        }
        if (this.mOthers == null) {
            if (other.mOthers != null) {
                return false;
            }
        } else if (!this.mOthers.equals(other.mOthers)) {
            return false;
        }
        if (this.mUser == null) {
            if (other.mUser != null) {
                return false;
            }
        } else if (!this.mUser.equals(other.mUser)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Permissions [user=" + this.mUser //$NON-NLS-1$
                + ", group=" + this.mGroup +  //$NON-NLS-1$
                ", others=" + this.mOthers + "]"; //$NON-NLS-1$//$NON-NLS-2$
    }

    /**
     * Method that returns a string representation of the permissions,
     * conforming with the unix style (rwx).
     *
     * @return String The string representation of the permissions
     */
    public String toRawString() {
        return this.mUser.toRawString()
                + this.mGroup.toRawString()
                + this.mOthers.toRawString();
    }

    /**
     * Method that converts every permission into octal numbers,
     * conforming with the unix style (xe: 0755).
     *
     * @return String The octal numbers string for the permissions
     */
    @SuppressWarnings("boxing")
    public String toOctalString() {
        //SetUID/SetGID/Sticky Bit
        int b = 0;
        if (this.mUser.isSetUID()) {
            b = b | 0x04;
        }
        if (this.mGroup.isSetGID()) {
            b = b | 0x02;
        }
        if (this.mOthers.isStickybit()) {
            b = b | 0x01;
        }
        //User
        int u = 0;
        if (this.mUser.isRead()) {
            u = u | 0x04;
        }
        if (this.mUser.isWrite()) {
            u = u | 0x02;
        }
        if (this.mUser.isExecute()) {
            u = u | 0x01;
        }
        //Group
        int g = 0;
        if (this.mGroup.isRead()) {
            g = g | 0x04;
        }
        if (this.mGroup.isWrite()) {
            g = g | 0x02;
        }
        if (this.mGroup.isExecute()) {
            g = g | 0x01;
        }
        //Others
        int o = 0;
        if (this.mOthers.isRead()) {
            o = o | 0x04;
        }
        if (this.mOthers.isWrite()) {
            o = o | 0x02;
        }
        if (this.mOthers.isExecute()) {
            o = o | 0x01;
        }

        //Return octal string
        return String.format("%d%d%d%d", b, u, g, o); //$NON-NLS-1$
    }

    /**
     * Method that returns the default permissions for folder
     *
     * @return Permissions The default permissions for folder
     */
    public static Permissions createDefaultFolderPermissions() {
        return new Permissions(
                new UserPermission(true, true, true, false),
                new GroupPermission(true, false, true, false),
                new OthersPermission(false, false, false, false));
    }

    /**
     * Method that returns the default permissions for folder
     *
     * @return Permissions The default permissions for folder
     */
    public static Permissions createDefaultFilePermissions() {
        return new Permissions(
                new UserPermission(true, true, false, false),
                new GroupPermission(true, true, false, false),
                new OthersPermission(false, false, false, false));
    }

    /**
     * Method that parses and extracts the permissions from a unix string format.
     *
     * @param rawPermissions The raw permissions
     * @return Permissions An object with all the permissions
     * @throws ParseException If the permissions can't be parsed
     * @see ParseHelper#parsePermission(String)
     */
    public static Permissions fromRawString(String rawPermissions) throws ParseException {
        return ParseHelper.parsePermission(rawPermissions);
    }

    /**
     * Method that converts the unix style octal number into a Permissions reference
     *
     * @param octalPermissions The octal permissions
     * @return Permissions An object with all the permissions
     * @throws ParseException If the permissions can't be parsed
     */
    public static Permissions fromOctalString(String octalPermissions) throws ParseException {
        int size = octalPermissions.length();
        if (size != 3 && size != 4) {
            throw new ParseException(
                    "Invalid permissions string length: !=3 or != 4", 0); //$NON-NLS-1$
        }

        // Extract the data into char
        int cc = 0;
        char b = 0;
        if (size == 4) {
            b = octalPermissions.charAt(cc);
            cc++;
        }
        char u = octalPermissions.charAt(cc);
        cc++;
        char g = octalPermissions.charAt(cc);
        cc++;
        char o = octalPermissions.charAt(cc);
        cc++;

        //Get permissions
        UserPermission user =
                new UserPermission(
                        (u & 0x04) == 0x04,
                        (u & 0x02) == 0x02,
                        (u & 0x01) == 0x01,
                        (b & 0x01) == 0x01);
        GroupPermission group =
                new GroupPermission(
                        (g & 0x04) == 0x04,
                        (g & 0x02) == 0x02,
                        (g & 0x01) == 0x01,
                        (b & 0x02) == 0x02);
        OthersPermission other =
                new OthersPermission(
                        (o & 0x04) == 0x04,
                        (o & 0x02) == 0x02,
                        (o & 0x01) == 0x01,
                        (b & 0x04) == 0x04);
        return new Permissions(user, group, other);
    }

}
