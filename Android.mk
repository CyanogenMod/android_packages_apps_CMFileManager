#
# Copyright (C) 2012 The CyanogenMod Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
appcompat_dir := ../../../external/android/support-prebuilt/appcompat/res
supportdesign_dir := ../../../external/android/support-prebuilt/support-design/res
res_dirs := $(supportdesign_dir) $(appcompat_dir) res

LOCAL_RESOURCE_DIR := \
    $(addprefix $(LOCAL_PATH)/, $(res_dirs))\
    frameworks/support/v7/cardview/res

LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_SRC_FILES += $(call all-java-files-under, libs/android-syntax-highlight/src)
LOCAL_SRC_FILES += $(call all-java-files-under, libs/color-picker-view/src)

LOCAL_STATIC_JAVA_LIBRARIES += libtruezip
LOCAL_STATIC_JAVA_LIBRARIES += juniversalchardet
LOCAL_STATIC_JAVA_LIBRARIES += cmfm-ambientsdk
LOCAL_STATIC_JAVA_LIBRARIES += cmfm-android-support-v4
LOCAL_STATIC_JAVA_LIBRARIES += cmfm-android-support-v7-appcompat
LOCAL_STATIC_JAVA_LIBRARIES += cmfm-android-support-design
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-cardview

#LOCAL_SDK_VERSION := current

LOCAL_PACKAGE_NAME := CMFileManager
LOCAL_CERTIFICATE := platform
#LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_PROGUARD_ENABLED := disabled

#LOCAL_AAPT_INCLUDE_ALL_RESOURCES := true
LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages android.support.design:android.support.v7.appcompat:android.support.v7.cardview

include $(BUILD_PACKAGE)
include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
    juniversalchardet:libs/juniversalchardet/juniversalchardet-1.0.3.jar \
    cmfm-ambientsdk:libs/classes.jar \
    cmfm-android-support-v4:../../../external/android/support-prebuilt/appcompat/android-support-v4.jar \
    cmfm-android-support-v7-appcompat:../../../external/android/support-prebuilt/appcompat/android-support-v7-appcompat.jar \
    cmfm-android-support-design:../../../external/android/support-prebuilt/support-design/android-support-design.jar

include $(BUILD_MULTI_PREBUILT)
include $(call all-makefiles-under,$(LOCAL_PATH))
