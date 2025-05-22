import android.content.Context
import android.widget.Toast
import com.chaquo.python.Python
import java.io.File

object PythonConverter {

    /**
     * 전달받은 m4a 파일(File 객체)을 앱 내부로 복사하고, wav로 변환.
     * @param context Context (filesDir 접근, Toast 사용)
     * @param m4aFile 변환할 m4a 파일(File 객체)
     * @param wavFileName 변환 후 저장할 파일명 (기본값 "call_audio.wav")
     * @return 변환된 wav 파일의 File 객체, 실패 시 null
     */
    fun convertM4aFileToWav(context: Context, m4aFile: File, wavFileName: String = "call_audio.wav"): File? {
        try {
            if (!m4aFile.exists()) {
                Toast.makeText(context, "m4a 파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                return null
            }
            // 내부 저장소로 복사
            val copiedM4a = File(context.filesDir, "call_audio.m4a")
            m4aFile.inputStream().use { input ->
                copiedM4a.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val wavFile = File(context.filesDir, wavFileName)

            val py = Python.getInstance()
            val module = py.getModule("convert")
            module.callAttr("convert_m4a_to_wav", copiedM4a.absolutePath, wavFile.absolutePath)

            val isValid = module.callAttr("check_wav_format", wavFile.absolutePath).toBoolean()
            if (isValid) {
                Toast.makeText(context, "변환 성공! ${wavFile.absolutePath}", Toast.LENGTH_SHORT).show()
                return wavFile
            } else {
                Toast.makeText(context, "변환 실패!", Toast.LENGTH_SHORT).show()
                return null
            }
        } catch (e: Exception) {
            Toast.makeText(context, "변환 중 예외 발생: ${e.message}", Toast.LENGTH_SHORT).show()
            return null
        }
    }
}
