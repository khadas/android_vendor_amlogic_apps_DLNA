ifneq ($(BOARD_USE_DEFAULT_APPINSTALL),false)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-subdir-java-files)

ifeq ($(shell test $(PLATFORM_SDK_VERSION) -ge 26 && echo OK),OK)
LOCAL_PROPRIETARY_MODULE := true
endif

LOCAL_PACKAGE_NAME := AppInstaller
LOCAL_CERTIFICATE := platform
LOCAL_JAVA_LIBRARIES := droidlogic

#LOCAL_PRIVATE_PLATFORM_APIS := true

include $(BUILD_PACKAGE)
endif
