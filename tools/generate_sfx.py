import math
import wave
import struct
from pathlib import Path

SAMPLE_RATE = 44100

def write_wav(path: Path, samples):
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), 'wb') as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(SAMPLE_RATE)
        frames = b"".join(struct.pack('<h', s) for s in samples)
        wf.writeframes(frames)


def sine(freq, duration, volume=0.6, attack=0.01, release=0.05):
    total = int(SAMPLE_RATE * duration)
    samples = []
    for i in range(total):
        t = i / SAMPLE_RATE
        env = 1.0
        if t < attack:
            env = t / attack
        elif t > duration - release:
            env = max(0.0, (duration - t) / release)
        amp = volume * env
        samples.append(int(math.sin(2 * math.pi * freq * t) * amp * 32767))
    return samples


def sweep(freq_start, freq_end, duration, volume=0.6):
    total = int(SAMPLE_RATE * duration)
    samples = []
    for i in range(total):
        t = i / SAMPLE_RATE
        freq = freq_start + (freq_end - freq_start) * (t / duration)
        amp = volume * (1 - (t / duration))
        samples.append(int(math.sin(2 * math.pi * freq * t) * amp * 32767))
    return samples


def noise(duration, volume=0.4):
    import random
    total = int(SAMPLE_RATE * duration)
    samples = []
    for i in range(total):
        amp = volume * (1 - i / total)
        samples.append(int((random.random() * 2 - 1) * amp * 32767))
    return samples


def mix(*tracks):
    length = max(len(t) for t in tracks)
    out = [0] * length
    for t in tracks:
        for i, s in enumerate(t):
            out[i] += s
    max_val = max(1, max(abs(s) for s in out))
    if max_val > 32767:
        scale = 32767 / max_val
        out = [int(s * scale) for s in out]
    return out


def main():
    base = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res" / "raw"
    write_wav(base / "sfx_bounce.wav", sine(880, 0.08, 0.45))
    write_wav(base / "sfx_brick.wav", mix(sine(520, 0.12, 0.5), sine(780, 0.08, 0.3)))
    write_wav(base / "sfx_powerup.wav", sweep(600, 1200, 0.2, 0.6))
    write_wav(base / "sfx_life.wav", sweep(400, 900, 0.25, 0.7))
    write_wav(base / "sfx_explosion.wav", mix(noise(0.25, 0.5), sine(90, 0.25, 0.3)))
    write_wav(base / "sfx_laser.wav", sweep(1200, 600, 0.12, 0.5))
    write_wav(base / "sfx_gameover.wav", sweep(500, 120, 0.5, 0.6))
    # Music loop: soft pulsing chord
    chord = mix(sine(220, 6.0, 0.12), sine(277, 6.0, 0.1), sine(330, 6.0, 0.1))
    write_wav(base / "music_loop.wav", chord)


if __name__ == "__main__":
    main()
