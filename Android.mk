LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_STATIC_JAVA_LIBRARIES := libandroid-support-v4 libdlna libmid libmta liblebo libumeng libutd libce
LOCAL_JAVA_LIBRARIES := droidlogic
LOCAL_SDK_VERSION := current
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_PACKAGE_NAME := DLNA 
LOCAL_CERTIFICATE := platform
LOCAL_PROGUARD_ENABLED := disabled
LOCAL_PRIVILEGED_MODULE := true

ifeq ($(shell test $(PLATFORM_SDK_VERSION) -ge 26 && echo OK),OK)
LOCAL_PROPRIETARY_MODULE := true
endif

LOCAL_PREBUILT_JNI_LIBS := \
                          libs/armeabi-v7a/libhpplayaudio.so \
                          libs/armeabi-v7a/libhpplaysmdns.so \
                          libs/armeabi-v7a/libhpplaymdns.so \
                          libs/armeabi-v7a/libhpplaymirror.so \
                          libs/armeabi-v7a/libhpplayvideo.so \
                          libs/armeabi-v7a/libhpplayvideo19.so \
                          libs/armeabi-v7a/libhisivideo.so \
                          libs/armeabi-v7a/libhisivideo19.so \
                          libs/armeabi-v7a/libhisivideo_3798m.so \
                          libs/armeabi-v7a/liblebodlna-jni.so
#LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_MULTILIB := 32
include $(BUILD_PACKAGE)

##############################################

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := liblebo:libs/LEBO-SDK-4.0.0.2r_external_amlogic.jar \
                                        libmid:libs/mid-mid-sdk-2.3.jar \
                                        libmta:libs/mta-android-stat-sdk-2.1.0_20160111.jar \
                                        libandroid-support-v4:libs/android-support-v4.jar \
                                        libdlna:libs/dlna.jar \
										libumeng:libs/umeng-analytics-v6.0.1.jar \
										libutd:libs/utdid4all-1.0.4.jar \
										libce:libs/ce-premium-cn-2.4.13.jar
LOCAL_MULTILIB := 32

ifeq ($(shell test $(PLATFORM_SDK_VERSION) -ge 26 && echo OK),OK)
LOCAL_PROPRIETARY_MODULE := true
endif
include $(BUILD_MULTI_PREBUILT)


#################################################################
####### copy the library to /system/lib #########################
#################################################################
include $(CLEAR_VARS)
LOCAL_MODULE := libhpplayaudio.so
LOCAL_MODULE_CLASS := SHARED_LIBRARIES

ifeq ($(shell test $(PLATFORM_SDK_VERSION) -ge 26 && echo OK),OK)
LOCAL_MODULE_PATH := $(TARGET_OUT_VENDOR)/lib
else
LOCAL_MODULE_PATH := $(TARGET_OUT)/lib#$(TARGET_OUT_SHARED_LIBRARIES)
endif

LOCAL_SRC_FILES := libs/armeabi-v7a/$(LOCAL_MODULE)
OVERRIDE_BUILD_MODULE_PATH := $(TARGET_OUT_INTERMEDIATE_LIBRARIES)
LOCAL_MULTILIB := 32
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := libhpplaysmdns.so
LOCAL_MODULE_CLASS := SHARED_LIBRARIES

ifeq ($(shell test $(PLATFORM_SDK_VERSION) -ge 26 && echo OK),OK)
LOCAL_MODULE_PATH := $(TARGET_OUT_VENDOR)/lib
else
LOCAL_MODULE_PATH := $(TARGET_OUT)/lib
endif

LOCAL_SRC_FILES := libs/armeabi-v7a/$(LOCAL_MODULE)
OVERRIDE_BUILD_MODULE_PATH := $(TARGET_OUT_INTERMEDIATE_LIBRARIES)
LOCAL_MULTILIB := 32
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := libhpplaymdns.so
LOCAL_MODULE_CLASS := SHARED_LIBRARIES

ifeq ($(shell test $(PLATFORM_SDK_VERSION) -ge 26 && echo OK),OK)
LOCAL_MODULE_PATH := $(TARGET_OUT_VENDOR)/lib
else
LOCAL_MODULE_PATH := $(TARGET_OUT)/lib
endif

LOCAL_SRC_FILES := libs/armeabi-v7a/$(LOCAL_MODULE)
OVERRIDE_BUILD_MODULE_PATH := $(TARGET_OUT_INTERMEDIATE_LIBRARIES)
LOCAL_MULTILIB := 32
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := libhpplayvideo.so
LOCAL_MODULE_CLASS := SHARED_LIBRARIES

ifeq ($(shell test $(PLATFORM_SDK_VERSION) -ge 26 && echo OK),OK)
LOCAL_MODULE_PATH := $(TARGET_OUT_VENDOR)/lib
else
LOCAL_MODULE_PATH := $(TARGET_OUT)/lib
endif

LOCAL_SRC_FILES := libs/armeabi-v7a/$(LOCAL_MODULE)
OVERRIDE_BUILD_MODULE_PATH := $(TARGET_OUT_INTERMEDIATE_LIBRARIES)
LOCAL_MULTILIB := 32
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := libhpplayvideo19.so
LOCAL_MODULE_CLASS := SHARED_LIBRARIES

ifeq ($(shell test $(PLATFORM_SDK_VERSION) -ge 26 && echo OK),OK)
LOCAL_MODULE_PATH := $(TARGET_OUT_VENDOR)/lib
else
LOCAL_MODULE_PATH := $(TARGET_OUT)/lib
endif

LOCAL_SRC_FILES := libs/armeabi-v7a/$(LOCAL_MODULE)
OVERRIDE_BUILD_MODULE_PATH := $(TARGET_OUT_INTERMEDIATE_LIBRARIES)
LOCAL_MULTILIB := 32
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := libhisivideo.so
LOCAL_MODULE_CLASS := SHARED_LIBRARIES

ifeq ($(shell test $(PLATFORM_SDK_VERSION) -ge 26 && echo OK),OK)
LOCAL_MODULE_PATH := $(TARGET_OUT_VENDOR)/lib
else
LOCAL_MODULE_PATH := $(TARGET_OUT)/lib
endif

LOCAL_SRC_FILES := libs/armeabi-v7a/$(LOCAL_MODULE)
OVERRIDE_BUILD_MODULE_PATH := $(TARGET_OUT_INTERMEDIATE_LIBRARIES)
LOCAL_MULTILIB := 32
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := libhisivideo19.so
LOCAL_MODULE_CLASS := SHARED_LIBRARIES

ifeq ($(shell test $(PLATFORM_SDK_VERSION) -ge 26 && echo OK),OK)
LOCAL_MODULE_PATH := $(TARGET_OUT_VENDOR)/lib
else
LOCAL_MODULE_PATH := $(TARGET_OUT)/lib
endif

LOCAL_SRC_FILES := libs/armeabi-v7a/$(LOCAL_MODULE)
OVERRIDE_BUILD_MODULE_PATH := $(TARGET_OUT_INTERMEDIATE_LIBRARIES)
LOCAL_MULTILIB := 32
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := libhisivideo_3798m.so
LOCAL_MODULE_CLASS := SHARED_LIBRARIES

ifeq ($(shell test $(PLATFORM_SDK_VERSION) -ge 26 && echo OK),OK)
LOCAL_MODULE_PATH := $(TARGET_OUT_VENDOR)/lib
else
LOCAL_MODULE_PATH := $(TARGET_OUT)/lib
endif

LOCAL_SRC_FILES := libs/armeabi-v7a/$(LOCAL_MODULE)
OVERRIDE_BUILD_MODULE_PATH := $(TARGET_OUT_INTERMEDIATE_LIBRARIES)
LOCAL_MULTILIB := 32
include $(BUILD_PREBUILT)
