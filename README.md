# 🎵 Voice Controlled Music Player App  

### 🔊 Offline Voice-Controlled Android Music Player

This was my first project experience — a **voice-controlled music player** Android application designed to enhance your music listening and interaction experience.  
Unlike traditional music apps, this player offers **hands-free navigation**, **offline voice commands**, and **privacy-focused control** without relying on Google Assistant or Alexa.

---

## 🚀 Features  

- 🎙️ **Voice Command Control:** Navigate and control music playback using your voice.  
- 🗣️ **Wake Word Detection:** Uses **Picovoice Porcupine** for offline wake word recognition.  
- 🧠 **Offline Speech Recognition:** Works **completely offline**, ensuring your privacy and faster response.  
- 🧍‍♂️ **Accessibility Support:** Designed to be helpful for **visually impaired** or **physically challenged** users.  
- 🚗 **Hands-Free Mode:** Ideal for multitasking scenarios such as **driving**, **cooking**, or **running**.  
- 🎨 **User-Friendly UI:** Clean, attractive interface with smooth runtime animations.  

---

## 🛠️ Tools & Technologies  

| Tool / Technology | Description |
|--------------------|-------------|
| **Android Studio** | Development environment |
| **Java** | Core programming language |
| **XML** | Used for UI design |
| **Android Speech Recognizer & STT** | Speech-to-text engine for commands |
| **Picovoice Porcupine** | Wake word detection for offline control |
| **Google Text-to-Speech (TTS)** | Provides voice feedback to users |

---

## 🎯 Objectives  

1. Provide **easy navigation** through voice commands.  
2. Enable **safe and simple use** during multitasking (driving, cooking, running, etc.).  
3. Make the app **accessible for visually or physically challenged** individuals.  
4. Offer **fully offline voice control**, maintaining **user privacy** and data security.  

---

## 🧩 How It Works  

1. The app continuously listens for a **wake word** (e.g., “Hey Music”).  
2. Once activated, it processes user voice commands like:  
   - “Play next song”  
   - “Pause music”  
   - “Play artist [name]”  
3. All processing happens **locally** on the device using **Picovoice** and Android’s **STT** — no internet required.  
4. **Google TTS** responds to confirm each command.

---

## 📱 Screenshots  

*(You can add screenshots here later)*  

---

## 🏗️ Installation  

1. Clone this repository:  
   ```bash
   git clone https://github.com/<your-username>/voice-controlled-music-player.git

