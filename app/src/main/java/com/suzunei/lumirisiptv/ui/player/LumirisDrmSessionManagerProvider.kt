package com.suzunei.lumirisiptv.ui.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.DrmSessionManager
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback

/**
 * Returns a per-[MediaItem] [DrmSessionManager] for ClearKey-protected streams.
 *
 * The ClearKey license JSON is stored as raw bytes in `MediaItem.DrmConfiguration.keySetId`
 * (see [MediaItemFactory.build]). Reading it here lets us hand it to [LocalMediaDrmCallback]
 * without any HTTP round-trip — the keys come straight from the playlist file.
 *
 * For media items without DRM, [DrmSessionManager.DRM_UNSUPPORTED] tells the player to skip DRM
 * setup entirely, which keeps the HLS / progressive / Google Drive paths cheap.
 */
@OptIn(UnstableApi::class)
class LumirisDrmSessionManagerProvider : DrmSessionManagerProvider {

    override fun get(mediaItem: MediaItem): DrmSessionManager {
        val drmConfig = mediaItem.localConfiguration?.drmConfiguration
            ?: return DrmSessionManager.DRM_UNSUPPORTED
        if (drmConfig.scheme != C.CLEARKEY_UUID) return DrmSessionManager.DRM_UNSUPPORTED
        val licenseBytes = drmConfig.keySetId
        if (licenseBytes == null || licenseBytes.isEmpty()) return DrmSessionManager.DRM_UNSUPPORTED

        val callback = LocalMediaDrmCallback(licenseBytes)
        return DefaultDrmSessionManager.Builder()
            .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
            .setMultiSession(false)
            .build(callback)
    }
}
