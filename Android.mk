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
appcompat_dir := libs/appcompat/res
supportdesign_dir := libs/support-design/res
res_dirs := res $(supportdesign_dir) $(appcompat_dir)

LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_SRC_FILES += $(call all-java-files-under, libs/android-syntax-highlight/src)
LOCAL_SRC_FILES += $(call all-java-files-under, libs/color-picker-view/src)

LOCAL_STATIC_JAVA_LIBRARIES += libtruezip
LOCAL_STATIC_JAVA_LIBRARIES += cmfm-android-support-v4
LOCAL_STATIC_JAVA_LIBRARIES += cmfm-android-support-v7-appcompat
LOCAL_STATIC_JAVA_LIBRARIES += cmfm-android-support-design
LOCAL_STATIC_JAVA_LIBRARIES += juniversalchardet

LOCAL_PACKAGE_NAME := CMFileManager
LOCAL_CERTIFICATE := platform
LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))
LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages android.support.v7:android.support.v7.appcompat:android.support.design

include $(BUILD_PACKAGE)
include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
    juniversalchardet:libs/juniversalchardet/juniversalchardet-1.0.3.jar \
    cmfm-android-support-v4:libs/appcompat/android-support-v4.jar \
    cmfm-android-support-v7-appcompat:libs/appcompat/android-support-v7-appcompat.jar \
    cmfm-android-support-design:libs/support-design/android-support-design.jar

include $(BUILD_MULTI_PREBUILT)
include $(call all-makefiles-under,$(LOCAL_PATH))
