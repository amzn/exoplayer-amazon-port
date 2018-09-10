/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.google.android.exoplayer2.audio;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import android.net.Uri;
import android.provider.Settings;

/**
 * Receives broadcast events indicating changes to the device's audio capabilities, notifying a
 * {@link Listener} when audio capability changes occur.
 */
public final class AudioCapabilitiesReceiver {

  /**
   * Listener notified when audio capabilities change.
   */
  public interface Listener {

    /**
     * Called when the audio capabilities change.
     *
     * @param audioCapabilities The current audio capabilities for the device.
     */
    void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities);

  }

  public static final String TAG = "AudioCapReceiver";
  private final Context context;
  private final @Nullable Handler handler;
  private final Listener listener;
  private final @Nullable BroadcastReceiver receiver;

  /* package */ @Nullable AudioCapabilities audioCapabilities;
  // AMZN_CHANGE_BEGIN
  private final ContentResolver resolver;
  private final SurroundSoundSettingObserver observer;
  private AudioCapabilities currentHDMIAudioCapabilities;
  // AMZN_CHANGE_END

  /**
   * @param context A context for registering the receiver.
   * @param listener The listener to notify when audio capabilities change.
   */
  public AudioCapabilitiesReceiver(Context context, Listener listener) {
    this(context, /* handler= */ null, listener);
  }

  /**
   * @param context A context for registering the receiver.
   * @param handler The handler to which {@link Listener} events will be posted. If null, listener
   *     methods are invoked on the main thread.
   * @param listener The listener to notify when audio capabilities change.
   */
  public AudioCapabilitiesReceiver(Context context, @Nullable Handler handler, Listener listener) {
    this.context = Assertions.checkNotNull(context);
    this.handler = handler;
    this.listener = Assertions.checkNotNull(listener);
    // AMZN_CHANGE_BEGIN
    boolean useSurroundSoundFlag = false;
    if (Util.SDK_INT >= 17) {
      this.resolver = context.getContentResolver();
      this.observer = new SurroundSoundSettingObserver();
      useSurroundSoundFlag = AudioCapabilities.useSurroundSoundFlagV17(
            this.resolver);
    } else {
      this.resolver = null;
      this.observer = null;
    }
    // Don't listen for audio plug encodings if useSurroundSoundFlag is set.
    // If useSurroundSoundFlag is set then the platform controls what the
    // audio output is by using the iSurroundSoundEnabled setting.
    this.receiver = (Util.SDK_INT >= 21 && !useSurroundSoundFlag) ?
            new HdmiAudioPlugBroadcastReceiver() : null;
    // AMZN_CHANGE_END
  }

  /**
   * Registers the receiver, meaning it will notify the listener when audio capability changes
   * occur. The current audio capabilities will be returned. It is important to call
   * {@link #unregister} when the receiver is no longer required.
   *
   * @return The current audio capabilities for the device.
   */
  @SuppressWarnings("InlinedApi")
  public AudioCapabilities register() {
    Intent stickyIntent = null;
    if (receiver != null) {
      IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_HDMI_AUDIO_PLUG);
      if (handler != null) {
        stickyIntent =
            context.registerReceiver(
                receiver, intentFilter, /* broadcastPermission= */ null, handler);
      } else {
        stickyIntent = context.registerReceiver(receiver, intentFilter);
      }
    }
    audioCapabilities = AudioCapabilities.getCapabilities(context, stickyIntent);
    // AMZN_CHANGE_BEGIN
    currentHDMIAudioCapabilities = audioCapabilities =
            AudioCapabilities.getCapabilities(context, stickyIntent);
    if (resolver != null && observer != null) {
      Uri surroundSoundUri =
              Settings.Global.getUriFor(AudioCapabilities.EXTERNAL_SURROUND_SOUND_ENABLED);
      resolver.registerContentObserver(surroundSoundUri, true, observer);
    }
    // AMZN_CHANGE_END
    return audioCapabilities;
  }

  /**
   * Unregisters the receiver, meaning it will no longer notify the listener when audio capability
   * changes occur.
   */
  public void unregister() {
    if (receiver != null) {
      context.unregisterReceiver(receiver);
    }
    // AMZN_CHANGE_BEGIN
    if (resolver != null  && observer != null) {
      resolver.unregisterContentObserver(observer);
    }
    // AMZN_CHANGE_END
  }

  // AMZN_CHANGE_BEGIN
  private void maybeNotifyAudioCapabilityChanged(AudioCapabilities newAudioCapabilities) {
    if (!newAudioCapabilities.equals(audioCapabilities)) {
      audioCapabilities = newAudioCapabilities;
      listener.onAudioCapabilitiesChanged(newAudioCapabilities);
    }
  }
  // AMZN_CHANGE_END

  private final class HdmiAudioPlugBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      if (!isInitialStickyBroadcast()) {
        // AMZN_CHANGE_BEGIN
        currentHDMIAudioCapabilities = AudioCapabilities.getCapabilities(context, intent);
        // no need to check whether HDMI audio capabilities needs to be overridden or not,
        // because AudioCapabilities.getCapabilities takes care of it. internally
        maybeNotifyAudioCapabilityChanged(currentHDMIAudioCapabilities);
        // AMZN_CHANGE_END
      }
    }

  }
  // AMZN_CHANGE_BEGIN
  @TargetApi(17)
  private final class SurroundSoundSettingObserver extends ContentObserver {
    public SurroundSoundSettingObserver() {
      super(null);
    }

    @Override
    public boolean deliverSelfNotifications() {
      return true;
    }

    @Override
    public void onChange(boolean selfChange) {
      super.onChange(selfChange);
      boolean useSurroundSoundFlag = AudioCapabilities.useSurroundSoundFlagV17(
            resolver);
      boolean isSurroundSoundEnabled = AudioCapabilities.
            isSurroundSoundEnabledV17(resolver);
      AudioCapabilities newAudioCapabilities;


      // Use the isSurroundSoundEnbled to determine audio out and ignore audio
      // plug encodings.
      if (useSurroundSoundFlag) {
        // override HDMI capabilities and enable Surround Sound audio capability
        // if surround sound enabled setting is set
        newAudioCapabilities = (isSurroundSoundEnabled) ?
                AudioCapabilities.SURROUND_AUDIO_CAPABILITIES :
                AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES;
      } else if (isSurroundSoundEnabled) {
            // This is a legacy use case.
        newAudioCapabilities = AudioCapabilities.SURROUND_AUDIO_CAPABILITIES;
      } else {
        // fallback to last known HDMI audioCapabilities,
        // hopefully updated by HDMI plugged state intent
        newAudioCapabilities = currentHDMIAudioCapabilities;
      }
      maybeNotifyAudioCapabilityChanged(newAudioCapabilities);
    }
  }
  // AMZN_CHANGE_END

}
