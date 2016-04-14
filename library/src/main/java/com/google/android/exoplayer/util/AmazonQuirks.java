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

import java.lang.String;

import android.os.Build;

import com.google.android.exoplayer.util.Util;
import com.google.android.exoplayer.util.MimeTypes;
import android.util.Log;
 public class AmazonQuirks {
  private static final String TAG = AmazonQuirks.class.getSimpleName();
  private static final String FIRETV_GEN1_DEVICE_MODEL = "AFTB";
  private static final String FIRETV_STICK_DEVICE_MODEL = "AFTM";
  private static final String FIRETV_GEN2_DEVICE_MODEL = "AFTS";
  private static final String KINDLE_TABLET_DEVICE_MODEL = "KF";
  private static final String AMAZON = "Amazon";
  private static final String DEVICEMODEL = Build.MODEL;
  private static final String MANUFACTURER = Build.MANUFACTURER;
  private static final int AUDIO_HARDWARE_LATENCY_FOR_TABLETS = 90000;
  private static final int MAX_INPUT_AVC_SIZE_FIRETV_GEN2 = (int) (2.8 * 1024 * 1024);

  public static boolean isAdaptive(String mimeType) {
    if (mimeType == null || mimeType.isEmpty()) {
      return false;
    }
    // Fire TV and tablets till now support adaptive codecs by default for video
    return ( isAmazonDevice() &&
             (mimeType.equalsIgnoreCase(MimeTypes.VIDEO_H264) ||
                mimeType.equalsIgnoreCase(MimeTypes.VIDEO_MP4)) );
  }

  public static boolean isKindleTablet() {
    return ( isAmazonDevice() && DEVICEMODEL.startsWith(KINDLE_TABLET_DEVICE_MODEL) );
  }

  public static boolean isLatencyQuirkEnabled() {
    // Sets latency quirk for Amazon KK and JB Tablets
    return ( (Util.SDK_INT <= 19) && isKindleTablet() );
  }

  public static int getAudioHWLatency() {
    // this function is called only when the above function
    // returns true for latency quirk. So no need to check for
    // SDK version and device type again
    return AUDIO_HARDWARE_LATENCY_FOR_TABLETS;
  }

  public static boolean isDolbyPassthroughQuirkEnabled() {
    // Sets dolby passthrough quirk for Amazon Fire TV Gen1  Family
    return isFireTVGen1Family();
  }

  public static boolean isAmazonDevice(){
    return MANUFACTURER.equalsIgnoreCase(AMAZON);
  }

  public static boolean shouldExtractPlayReadyHeader() {
    return ( isFireTVGen1Family() || isFireTVGen2() );
  }

  public static boolean isFireTVGen1Family() {
    return ( isAmazonDevice() &&
             ( DEVICEMODEL.equalsIgnoreCase(FIRETV_GEN1_DEVICE_MODEL) ||
                               DEVICEMODEL.equalsIgnoreCase(FIRETV_STICK_DEVICE_MODEL) ));
  }
  public static boolean isDecoderBlacklisted(String codecName) {
     if(!isAmazonDevice()) {
         return false;
     }
     if(isFireTVGen2() && codecName.startsWith("OMX.MTK.AUDIO.DECODER.MP3")) {
         return true;
     }
     return false;
  }

  public static boolean isFireTVGen2() {
    return ( isAmazonDevice() &&
             DEVICEMODEL.equalsIgnoreCase(FIRETV_GEN2_DEVICE_MODEL) );
  }

  public static boolean useDefaultPassthroughDecoder() {
    //Use platform decoder for
    //FireTV Gen1
    //FireTV Stick
    if (isFireTVGen1Family()) {
      Log.i(TAG,"using platform Dolby decoder");
      return false;
    }
    Log.i(TAG,"using default Dolby pass-through decoder");
    return true;
  }

  /* In Fire TV Gen1 family of devices, there is a platform limitation that
   * codec cannot be initialized with a crypto object before the DRM keys are
   * provided to MediaDRM - the media codec either skips processing or
   * throws error on processing the CSD provided in clear as part of the
   * media format object passed in configure API.
   * Hence, we wait for the DRM keys to be acquired before initializing the codec.
   */
  public static boolean waitForDRMKeysBeforeInitCodec() {
    return isFireTVGen1Family();
  }

  /* Fire TV Gen2 device has a limitation of max input size
   * for AVC frames - capped at 2.8 MB.
   */
  public static boolean isMaxInputAVCSizeSupported(int maxInputSize) {
    if (isFireTVGen2()) {
      return maxInputSize <= MAX_INPUT_AVC_SIZE_FIRETV_GEN2;
    }
    return true;

  }
}
