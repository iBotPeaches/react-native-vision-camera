package com.mrousavy.camera.extensions

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.view.Surface
import com.mrousavy.camera.parsers.Flash
import com.mrousavy.camera.parsers.Orientation
import com.mrousavy.camera.parsers.QualityPrioritization

fun CameraDevice.createPhotoCaptureRequest(cameraManager: CameraManager,
                                           surface: Surface,
                                           qualityPrioritization: QualityPrioritization,
                                           flashMode: Flash,
                                           enableRedEyeReduction: Boolean,
                                           enableAutoStabilization: Boolean,
                                           orientation: Orientation): CaptureRequest {
  val cameraCharacteristics = cameraManager.getCameraCharacteristics(this.id)

  val captureRequest = when (qualityPrioritization) {
    // If speed, use snapshot template for fast capture
    QualityPrioritization.SPEED -> this.createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT)
    // Otherwise create standard still image capture template
    else -> this.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
  }

  // TODO: Maybe we can even expose that prop directly?
  val jpegQuality = when (qualityPrioritization) {
    QualityPrioritization.SPEED -> 85
    QualityPrioritization.BALANCED -> 92
    QualityPrioritization.QUALITY -> 100
  }
  captureRequest[CaptureRequest.JPEG_QUALITY] = jpegQuality.toByte()

  captureRequest.set(CaptureRequest.JPEG_ORIENTATION, orientation.toDegrees())

  when (flashMode) {
    // Set the Flash Mode
    Flash.OFF -> {
      captureRequest[CaptureRequest.FLASH_MODE] = CaptureRequest.FLASH_MODE_OFF
      captureRequest[CaptureRequest.CONTROL_AE_MODE] = CaptureRequest.CONTROL_AE_MODE_ON
    }
    Flash.ON -> {
      captureRequest[CaptureRequest.FLASH_MODE] = CaptureRequest.FLASH_MODE_SINGLE
      captureRequest[CaptureRequest.CONTROL_AE_MODE] = CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH
    }
    Flash.AUTO -> {
      if (enableRedEyeReduction) {
        captureRequest[CaptureRequest.CONTROL_AE_MODE] = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE
      } else {
        captureRequest[CaptureRequest.CONTROL_AE_MODE] = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
      }
    }
  }

  if (enableAutoStabilization) {
    // Enable optical or digital image stabilization
    val digitalStabilization = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
    val hasDigitalStabilization = digitalStabilization?.contains(CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON) ?: false

    val opticalStabilization = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
    val hasOpticalStabilization = opticalStabilization?.contains(CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON) ?: false
    if (hasOpticalStabilization) {
      captureRequest[CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE] = CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
      captureRequest[CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE] = CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
    } else if (hasDigitalStabilization) {
      captureRequest[CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE] = CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
    } else {
      // no stabilization is supported. ignore it
    }
  }

  captureRequest.addTarget(surface)

  return captureRequest.build()
}
