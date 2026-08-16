cmake_minimum_required(VERSION 3.22.1)
project("iboostx")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(
        iboostx_native
        SHARED
        JNI.cpp
        CPUMonitor.cpp
        ThermalManager.cpp
        BatteryNative.cpp
        Booster.cpp
)

find_library(log-lib log)

target_link_libraries(
        iboostx_native
        ${log-lib}
        android
)

target_include_directories(iboostx_native PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
