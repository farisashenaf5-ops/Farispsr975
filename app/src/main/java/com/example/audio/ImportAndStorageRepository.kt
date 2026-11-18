package com.example.audio

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ImportAndStorageRepository(private val context: Context) {

  private val importsDir = File(context.filesDir, "imports").apply { mkdirs() }
  private val recordingsDir = File(context.filesDir, "recordings").apply { mkdirs() }
  private val indexFile = File(context.filesDir, "imports_index.json")
  private val regFile = File(context.filesDir, "registrations.json")
  private val freezeFile = File(context.filesDir, "freeze_config.json")

  data class ImportLoadResult(
    val sf2Banks: List<ImportedSf2Bank>,
    val wavSamples: List<ImportedWavSample>,
    val styFiles: List<ImportedStyFile>,
  )

  fun loadAllImports(): ImportLoadResult {
    val sf2List = mutableListOf<ImportedSf2Bank>()
    val wavList = mutableListOf<ImportedWavSample>()
    val styList = mutableListOf<ImportedStyFile>()

    if (!indexFile.exists()) return ImportLoadResult(sf2List, wavList, styList)

    return try {
      val arr = JSONArray(indexFile.readText())
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val id = obj.getString("id")
        val type = obj.getString("type")
        val fileName = obj.getString("fileName")
        val localPath = obj.getString("localPath")
        val file = File(localPath)
        if (!file.exists()) continue

        when (type) {
          "sf2" -> Sf2Parser.parse(file, id, fileName)?.let { sf2List.add(it) }
          "wav" -> file.inputStream().use { WavParser.parse(it, id, fileName) }?.let { wavList.add(it) }
          "sty" -> file.inputStream().use { StyParser.parse(it, id, fileName) }?.let { styList.add(it) }
        }
      }
      ImportLoadResult(sf2List, wavList, styList)
    } catch (_: Exception) {
      ImportLoadResult(sf2List, wavList, styList)
    }
  }

  sealed class ImportOutcome {
    data class Sf2Imported(val bank: ImportedSf2Bank) : ImportOutcome()
    data class WavImported(val sample: ImportedWavSample) : ImportOutcome()
    data class StyImported(val sty: ImportedStyFile) : ImportOutcome()
    data class Error(val message: String) : ImportOutcome()
  }

  fun importFromUri(uri: Uri): ImportOutcome {
    val fileName = resolveFileName(uri) ?: "imported_file"
    val ext = fileName.substringAfterLast('.', "").lowercase()
    if (ext !in listOf("sf2", "sty", "wav")) {
      return ImportOutcome.Error("Unsupported file: .$ext (use .sf2, .sty, or .wav)")
    }

    val id = "${ext}_${UUID.randomUUID().toString().take(8)}"
    val targetFile = File(importsDir, "${id}_$fileName")

    return try {
      context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(targetFile).use { output ->
          input.copyTo(output)
        }
      } ?: return ImportOutcome.Error("Unable to open selected file")

      val outcome = when (ext) {
        "sf2" -> {
          val bank = Sf2Parser.parse(targetFile, id, fileName)
          if (bank != null) ImportOutcome.Sf2Imported(bank)
          else ImportOutcome.Error("Invalid SoundFont 2 (.sf2) file")
        }
        "wav" -> {
          val wav = targetFile.inputStream().use { WavParser.parse(it, id, fileName) }
          if (wav != null) ImportOutcome.WavImported(wav)
          else ImportOutcome.Error("Unsupported or corrupt .wav file")
        }
        "sty" -> {
          val sty = targetFile.inputStream().use { StyParser.parse(it, id, fileName) }
          if (sty != null) ImportOutcome.StyImported(sty)
          else ImportOutcome.Error("Unsupported or corrupt Yamaha .sty file")
        }
        else -> ImportOutcome.Error("Unsupported format")
      }

      if (outcome is ImportOutcome.Error) {
        targetFile.delete()
      } else {
        appendImportIndex(id, ext, fileName, targetFile.absolutePath)
      }
      outcome
    } catch (e: Exception) {
      targetFile.delete()
      ImportOutcome.Error(e.message ?: "Import failed")
    }
  }

  fun deleteImport(id: String) {
    if (!indexFile.exists()) return
    try {
      val arr = JSONArray(indexFile.readText())
      val next = JSONArray()
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        if (obj.getString("id") == id) {
          File(obj.getString("localPath")).delete()
        } else {
          next.put(obj)
        }
      }
      indexFile.writeText(next.toString())
    } catch (_: Exception) {}
  }

  private fun appendImportIndex(id: String, type: String, fileName: String, localPath: String) {
    val arr = if (indexFile.exists()) {
      try { JSONArray(indexFile.readText()) } catch (_: Exception) { JSONArray() }
    } else {
      JSONArray()
    }
    arr.put(
      JSONObject().apply {
        put("id", id)
        put("type", type)
        put("fileName", fileName)
        put("localPath", localPath)
      }
    )
    indexFile.writeText(arr.toString())
  }

  private fun resolveFileName(uri: Uri): String? {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
      val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (idx >= 0 && cursor.moveToFirst()) {
        return cursor.getString(idx)
      }
    }
    return uri.lastPathSegment?.substringAfterLast('/')
  }

  fun saveRegistrations(banks: Map<Int, List<RegistrationSlot>>) {
    try {
      val root = JSONObject()
      for ((bankNum, slots) in banks) {
        val slotsArr = JSONArray()
        for (slot in slots) {
          slotsArr.put(
            JSONObject().apply {
              put("slotNumber", slot.slotNumber)
              put("label", slot.label)
              put("isEmpty", slot.isEmpty)
              put("styleId", slot.styleId)
              put("variation", slot.variation.name)
              put("tempo", slot.tempo)
              put("transpose", slot.transpose)
              put("upperOctave", slot.upperOctave)
              put("acmpEnabled", slot.acmpEnabled)
              put("sustainEnabled", slot.sustainEnabled)
              put("dspEnabled", slot.dspEnabled)
              put("harmonyEnabled", slot.harmonyEnabled)
              put("modulation", slot.modulation.toDouble())
              val partsObj = JSONObject()
              for ((pId, pState) in slot.parts) {
                partsObj.put(
                  pId.name,
                  JSONObject().apply {
                    put("isEnabled", pState.isEnabled)
                    put("voiceId", pState.voiceId)
                    put("volume", pState.volume.toDouble())
                    put("octaveShift", pState.octaveShift)
                  }
                )
              }
              put("parts", partsObj)
              val chObj = JSONObject()
              for ((ch, en) in slot.channelEnabled) {
                chObj.put(ch.name, en)
              }
              put("channels", chObj)
            }
          )
        }
        root.put(bankNum.toString(), slotsArr)
      }
      regFile.writeText(root.toString())
    } catch (_: Exception) {}
  }

  fun loadRegistrations(defaultFactorySlots: () -> Map<Int, List<RegistrationSlot>>): Map<Int, List<RegistrationSlot>> {
    if (!regFile.exists()) return defaultFactorySlots()
    return try {
      val root = JSONObject(regFile.readText())
      val result = mutableMapOf<Int, List<RegistrationSlot>>()
      for (bankNum in 1..8) {
        val arr = root.optJSONArray(bankNum.toString()) ?: continue
        val list = mutableListOf<RegistrationSlot>()
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          val partsObj = obj.getJSONObject("parts")
          val partsMap = mutableMapOf<PartId, PartState>()
          for (pId in PartId.entries) {
            val pJson = partsObj.optJSONObject(pId.name)
            if (pJson != null) {
              partsMap[pId] = PartState(
                partId = pId,
                isEnabled = pJson.optBoolean("isEnabled", pId == PartId.RIGHT1),
                voiceId = pJson.optString("voiceId", "v_grand_piano"),
                volume = pJson.optDouble("volume", 0.85).toFloat(),
                octaveShift = pJson.optInt("octaveShift", 0),
              )
            }
          }
          val chObj = obj.optJSONObject("channels")
          val chMap = StyleChannel.entries.associateWith { chObj?.optBoolean(it.name, true) ?: true }

          list.add(
            RegistrationSlot(
              slotNumber = obj.getInt("slotNumber"),
              label = obj.getString("label"),
              isEmpty = obj.getBoolean("isEmpty"),
              styleId = obj.getString("styleId"),
              variation = runCatching { StyleSection.valueOf(obj.getString("variation")) }.getOrDefault(StyleSection.MAIN_A),
              tempo = obj.getInt("tempo"),
              transpose = obj.getInt("transpose"),
              upperOctave = obj.getInt("upperOctave"),
              acmpEnabled = obj.getBoolean("acmpEnabled"),
              parts = partsMap,
              sustainEnabled = obj.getBoolean("sustainEnabled"),
              dspEnabled = obj.getBoolean("dspEnabled"),
              harmonyEnabled = obj.getBoolean("harmonyEnabled"),
              modulation = obj.optDouble("modulation", 0.0).toFloat(),
              channelEnabled = chMap,
            )
          )
        }
        if (list.size == 8) result[bankNum] = list
      }
      if (result.isEmpty()) defaultFactorySlots() else defaultFactorySlots().toMutableMap().apply { putAll(result) }
    } catch (_: Exception) {
      defaultFactorySlots()
    }
  }

  fun saveFreezeOptions(opts: FreezeOptions) {
    try {
      freezeFile.writeText(
        JSONObject().apply {
          put("freezeStyle", opts.freezeStyle)
          put("freezeTempo", opts.freezeTempo)
          put("freezeTranspose", opts.freezeTranspose)
          put("freezeVoices", opts.freezeVoices)
          put("freezeHarmony", opts.freezeHarmony)
        }.toString()
      )
    } catch (_: Exception) {}
  }

  fun loadFreezeOptions(): FreezeOptions {
    if (!freezeFile.exists()) return FreezeOptions()
    return try {
      val obj = JSONObject(freezeFile.readText())
      FreezeOptions(
        freezeStyle = obj.optBoolean("freezeStyle", true),
        freezeTempo = obj.optBoolean("freezeTempo", true),
        freezeTranspose = obj.optBoolean("freezeTranspose", true),
        freezeVoices = obj.optBoolean("freezeVoices", false),
        freezeHarmony = obj.optBoolean("freezeHarmony", false),
      )
    } catch (_: Exception) {
      FreezeOptions()
    }
  }

  fun createRecordingFile(): File = File(recordingsDir, "PSR_REC_${System.currentTimeMillis()}.wav")

  fun listRecordings(): List<File> =
    recordingsDir.listFiles()?.filter { it.extension.equals("wav", true) }?.sortedByDescending { it.lastModified() } ?: emptyList()
}
