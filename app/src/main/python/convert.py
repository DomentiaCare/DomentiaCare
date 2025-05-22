# convert.py
from pydub import AudioSegment
import wave

def convert_m4a_to_wav(m4a_path, wav_path, sample_rate=16000, channels=1, sampwidth=2):
    audio = AudioSegment.from_file(m4a_path, format="m4a")
    audio = audio.set_frame_rate(sample_rate).set_channels(channels).set_sample_width(sampwidth)
    audio.export(wav_path, format="wav")
    return True

def check_wav_format(filepath):
    with wave.open(filepath, 'rb') as wf:
        sample_rate = wf.getframerate()
        channels = wf.getnchannels()
        sampwidth = wf.getsampwidth()
        return sample_rate == 16000 and channels == 1 and sampwidth == 2
