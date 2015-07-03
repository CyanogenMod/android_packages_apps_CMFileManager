/*
 * Copyright (C) 2015 The CyanogenMod Project
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

import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.util.HashMap;

/**
 * Provides the ability to determine the mimetype of a known file extension that can support
 * multiple mimetypes.
 */
public abstract class AmbiguousExtensionHelper {
    /**
     * All available ambiguous extension helpers.
     */
    public static final HashMap<String, AmbiguousExtensionHelper> AMBIGUOUS_EXTENSIONS_MAP = new
            HashMap<String, AmbiguousExtensionHelper>();

    static {
       addAmbiguousHelperToMap(new ThreeGPExtensionHelper());
    }

    public abstract String getMimeType(String absolutePath, String extension);
    public abstract String[] getSupportedExtensions();

    private static void addAmbiguousHelperToMap(AmbiguousExtensionHelper instance) {
        for(String extension : instance.getSupportedExtensions()) {
            AmbiguousExtensionHelper.AMBIGUOUS_EXTENSIONS_MAP.put(extension, instance);
        }
    }

    /**
     * An AmbiguousExtensionHelper subclass that can distinguish the mimetype of a given
     * .g3p, .g3pp, .3g2 or .3gpp2 file. The 3GP and 3G2 file formats support both audio and
     * video, and a file with that extension has the possibility of multiple mimetypes, depending
     * on the content of the file.
     */
    public static class ThreeGPExtensionHelper extends AmbiguousExtensionHelper {
        private static final String TAG = "ThreeGPExtensionHelper";
        private static final String[] sSupportedExtensions = {"3gp", "3gpp", "3g2", "3gpp2"};
        public static final String VIDEO_3GPP_MIME_TYPE = "video/3gpp";
        public static final String AUDIO_3GPP_MIME_TYPE = "audio/3gpp";
        public static final String VIDEO_3GPP2_MIME_TYPE = "video/3gpp2";
        public static final String AUDIO_3GPP2_MIME_TYPE = "audio/3gpp2";

        @Override
        public String getMimeType(String absolutePath, String extension) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(absolutePath);
                boolean hasVideo =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) !=
                        null;
                if (is3GPP(extension)) {
                    return hasVideo ? VIDEO_3GPP_MIME_TYPE : AUDIO_3GPP_MIME_TYPE;
                } else if (is3GPP2(extension)) {
                    return hasVideo ? VIDEO_3GPP2_MIME_TYPE : AUDIO_3GPP2_MIME_TYPE;
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to open 3GP file to determine mimetype");
            }
            // Default to video 3gp if the file is unreadable as this was the default before
            // ambiguous resolution support was added.
            return VIDEO_3GPP_MIME_TYPE;
        }

        @Override
        public String[] getSupportedExtensions() {
            return sSupportedExtensions;
        }

        private boolean is3GPP(String ext) {
            return "3gp".equals(ext) || "3gpp".equals(ext);
        }

        private boolean is3GPP2(String ext) {
            return "3g2".equals(ext) || "3gpp2".equals(ext);
        }
    }
}
