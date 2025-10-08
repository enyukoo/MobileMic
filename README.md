# MobileMic

MobileMic is an Android application that allows you to use your Android device's microphone as a Bluetooth audio input for your PC. The app acts as a Bluetooth audio profile server, making the microphone services available to any PC after a Bluetooth connection is established.

## Features

- **Bluetooth Microphone Streaming**: Stream audio from your Android device's microphone to a PC over Bluetooth
- **Simple UI**: Easy-to-use interface with start/stop controls
- **Real-time Status**: Shows connection status and streaming state
- **Foreground Service**: Runs as a foreground service to ensure reliable audio streaming
- **Standard Bluetooth Profile**: Uses the Serial Port Profile (SPP) for compatibility

## Requirements

- Android device with Android 6.0 (API 23) or higher
- Bluetooth support on both Android device and PC
- Microphone hardware on Android device

## Permissions

The app requires the following permissions:
- **BLUETOOTH**: For Bluetooth connectivity
- **BLUETOOTH_ADMIN**: To manage Bluetooth settings
- **BLUETOOTH_CONNECT**: To connect to Bluetooth devices (Android 12+)
- **BLUETOOTH_ADVERTISE**: To make device discoverable (Android 12+)
- **RECORD_AUDIO**: To access the device microphone
- **ACCESS_FINE_LOCATION**: Required for Bluetooth on Android 12+

## How to Use

1. **Install the App**: Build and install the MobileMic app on your Android device
2. **Enable Bluetooth**: Make sure Bluetooth is enabled on both your Android device and PC
3. **Pair Devices**: Pair your Android device with your PC via Bluetooth settings
4. **Launch the App**: Open MobileMic on your Android device
5. **Grant Permissions**: Allow the app to access Bluetooth and Microphone when prompted
6. **Start Service**: Tap "Start Bluetooth Mic" button
7. **Make Device Discoverable**: Accept the prompt to make your device discoverable
8. **Connect from PC**: On your PC, connect to the Android device as a Bluetooth audio input device
9. **Start Using**: The app will start streaming audio from the microphone to your PC

## Technical Details

### Audio Configuration
- **Sample Rate**: 44100 Hz
- **Channel**: Mono
- **Encoding**: PCM 16-bit
- **Bluetooth Profile**: Serial Port Profile (SPP) with UUID `00001101-0000-1000-8000-00805F9B34FB`

### Architecture
- **MainActivity**: Handles UI, permissions, and service lifecycle
- **BluetoothMicService**: Manages Bluetooth server socket and audio streaming
- **AudioRecord**: Captures audio from the device microphone
- **BluetoothSocket**: Streams audio data over Bluetooth

## Building the Project

```bash
# Clone the repository
git clone https://github.com/enyukoo/MobileMic.git
cd MobileMic

# Build the APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

## PC Setup

After starting the MobileMic service on your Android device:

1. Open Bluetooth settings on your PC
2. Find your Android device in the list
3. Connect to the device
4. Configure your audio applications to use the Bluetooth audio input

Note: Some PCs may require additional software or drivers to properly handle Bluetooth audio input devices.

## Troubleshooting

- **Connection fails**: Ensure devices are properly paired and Bluetooth is enabled
- **No audio**: Check microphone permissions and ensure the service is running
- **Audio quality issues**: Try reducing distance between devices or eliminating interference
- **Service stops**: Make sure battery optimization is disabled for the app

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.
