"""Procedural loops for the powered tools: run from the repo root, needs numpy and ffmpeg."""
import numpy as np, subprocess, wave, os, tempfile

SR = 22050
OUT = "common/src/main/resources/assets/simplemachines/sounds/item"
rng = np.random.default_rng(7)


def lp(x, a):
    # one-pole low pass applied circularly, so the last sample runs into the first
    n = len(x); X = np.fft.rfft(x)
    z = np.exp(-2j * np.pi * np.arange(len(X)) / n)
    return np.fft.irfft(X * a / (1 - (1 - a) * z), n)


def loop_noise(n, a):
    return lp(rng.standard_normal(n), a)


def write(name, x, gain):
    x = x / np.max(np.abs(x)) * gain
    f = tempfile.mktemp(suffix=".wav")
    with wave.open(f, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((x * 32767).astype("<i2").tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", f, "-c:a", "libvorbis", "-q:a", "3", f"{OUT}/{name}.ogg"], check=True)
    os.remove(f)


def drill():
    d = 1.0; n = int(SR * d); t = np.arange(n) / SR
    f0 = 180  # whole cycles per second keep the loop seamless
    motor = sum(np.sin(2 * np.pi * f0 * k * t + k) / k ** 1.3 for k in range(1, 9))
    whine = 0.25 * np.sin(2 * np.pi * 1440 * t)
    grind = loop_noise(n, 0.35) * (0.6 + 0.4 * np.sin(2 * np.pi * 12 * t) ** 2)
    wobble = 1 + 0.15 * np.sin(2 * np.pi * 6 * t)
    write("electric_drill", (0.7 * motor + whine) * wobble + 1.1 * grind, 0.5)


def chainsaw():
    d = 1.0; n = int(SR * d); t = np.arange(n) / SR
    f0 = 95
    ph = (f0 * t) % 1.0
    engine = np.tanh(3 * (2 * ph - 1)) + 0.5 * np.sign(np.sin(2 * np.pi * f0 * 2 * t))
    engine = lp(engine, 0.3)
    chain = loop_noise(n, 0.6) * (0.5 + 0.5 * np.abs(np.sin(2 * np.pi * 30 * t)))
    write("chainsaw", 0.8 * engine * (1 + 0.2 * np.sin(2 * np.pi * 8 * t)) + 0.7 * chain, 0.45)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    drill(); chainsaw()
