// Один мягкий волшебный сигнал для успешного предсказания и завершения готовки.
// Синтезируется локально: без аудиофайлов, сети, вибрации и автозапуска.
let context = null;

export function unlockSuccessSound() {
  try {
    const AudioContextClass = window.AudioContext || window.webkitAudioContext;
    if (!AudioContextClass) return null;
    if (!context || context.state === "closed") context = new AudioContextClass();
    if (context.state === "suspended") context.resume().catch(() => {});
    return context;
  } catch (error) {
    console.debug("Звук недоступен:", error);
    return null;
  }
}

export function playSuccessSound() {
  const audio = unlockSuccessSound();
  if (!audio) return;

  // Нежный восходящий аккорд с короткими сияющими обертонами.
  const start = audio.currentTime + 0.02;
  const notes = [523.25, 659.25, 783.99, 1046.5];
  notes.forEach((frequency, index) => {
    const at = start + index * 0.115;
    const length = 0.55 + index * 0.045;

    for (const [multiple, volume, type] of [[1, 0.10, "sine"], [2.02, 0.025, "sine"]]) {
      const oscillator = audio.createOscillator();
      const envelope = audio.createGain();
      oscillator.type = type;
      oscillator.frequency.setValueAtTime(frequency * multiple, at);
      envelope.gain.setValueAtTime(0.0001, at);
      envelope.gain.exponentialRampToValueAtTime(volume, at + 0.015);
      envelope.gain.exponentialRampToValueAtTime(0.0001, at + length);
      oscillator.connect(envelope);
      envelope.connect(audio.destination);
      oscillator.start(at);
      oscillator.stop(at + length + 0.02);
    }
  });
}
