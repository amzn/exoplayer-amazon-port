/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.android.exoplayer.util;

import android.os.Build;
import android.util.Log;

 public class AmazonQuirks {
     //ordering of the static initializations is important.
     private static final String TAG = AmazonQuirks.class.getSimpleName();
     private static final String FIRETV_GEN1_DEVICE_MODEL = "AFTB";
     private static final String FIRETV_GEN2_DEVICE_MODEL = "AFTS";
     private static final String FIRETV_STICK_DEVICE_MODEL = "AFTM";
     private static final String KINDLE_TABLET_DEVICE_MODEL = "KF";
     private static final String FIRE_PHONE_DEVICE_MODEL = "SD";
     private static final String AMAZON = "Amazon";
     private static final String DEVICEMODEL = Build.MODEL;
     private static final String MANUFACTURER = Build.MANUFACTURER;
     private static final int AUDIO_HARDWARE_LATENCY_FOR_TABLETS = 90000;
     //caching
     private static final boolean isAmazonDevice;
     private static final boolean isFireTVGen1;
     private static final boolean isFireTVStick;
     private static final boolean isFireTVGen2;
     private static final boolean isKindleTablet;
     private static final boolean isFirePhone;
     private static final long fireTVFireOsBuildVersion;

     static {// This static block must be at the end
         //Init ordering is important within this block
         isAmazonDevice = MANUFACTURER.equalsIgnoreCase(AMAZON);
         isFireTVGen1 = isAmazonDevice && DEVICEMODEL.equalsIgnoreCase(FIRETV_GEN1_DEVICE_MODEL);
         isFireTVGen2 = isAmazonDevice && DEVICEMODEL.equalsIgnoreCase(FIRETV_GEN2_DEVICE_MODEL);
         isFireTVStick = isAmazonDevice && DEVICEMODEL.equalsIgnoreCase(FIRETV_STICK_DEVICE_MODEL);
         isKindleTablet = isAmazonDevice && DEVICEMODEL.startsWith(KINDLE_TABLET_DEVICE_MODEL);
         isFirePhone = isAmazonDevice && DEVICEMODEL.startsWith(FIRE_PHONE_DEVICE_MODEL);
         fireTVFireOsBuildVersion = getBuildVersion();
     }

     private AmazonQuirks(){}

     public static boolean isLatencyQuirkEnabled() {
         // Sets latency quirk for Amazon KK and JB Tablets and Fire Phone
         return ( (Util.SDK_INT <= 19) && (isKindleTablet || isFirePhone));
     }

     public static int getAudioHWLatency() {
         // this function is called only when the above function
         // returns true for latency quirk. So no need to check for
         // SDK version and device type again
         return AUDIO_HARDWARE_LATENCY_FOR_TABLETS;
     }

     public static boolean isAmazonDevice(){
         return isAmazonDevice;
     }

     public static boolean isFireTVGen1Family() {
         return isFireTVGen1 || isFireTVStick;
     }

     public static boolean isFireTVGen2() {
         return isFireTVGen2;
     }

     private static long getBuildVersion() {
         try {
             String[] verSplit = Build.VERSION.INCREMENTAL.split("_");
             if (verSplit.length > 2) {
                 return Long.valueOf(verSplit[2]);
             }
         } catch (Exception e) {
             Log.e(TAG,"Exception in finding build version",e);
         }
         return Long.MAX_VALUE;
     }
 }
