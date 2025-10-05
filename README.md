# Clarity AI - Android Mobile App

Intelligent file analysis and storage optimization powered by AI. Scans device files, uploads to FastAPI backend for ML-powered analysis, and provides smart storage recommendations.

## Features

- **Device File Scanning**: Scans images, videos, audio, and documents from Android device
- **AI-Powered Analysis**: 
  - Document summarization using DistilBART
  - Image duplicate detection using perceptual hashing
  - Relevance scoring and smart recommendations
- **Backend Integration**: Real-time file uploads to FastAPI backend
- **Storage Insights**: Detailed storage analytics and optimization suggestions
- **Dual View**:
  - Device Scan: Local file scanning and analysis
  - AI Analysis: Backend database view with AI summaries

## Architecture

### Android App (Kotlin)
- **MVVM Architecture**: ViewModel + Repository pattern
- **Jetpack Compose**: Modern declarative UI
- **Retrofit**: REST API communication
- **Coroutines**: Asynchronous operations
- **Material 3**: Modern Material Design

### Backend (FastAPI)
- **Document Analysis**: PyPDF2, python-docx for text extraction
- **AI Summarization**: DistilBART transformer model
- **Image Processing**: ImageHash for duplicate detection
- **Storage**: JSON database (clarity_db.json)

## Prerequisites

### Android Development
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK API 21+ (Android 5.0+)
- Kotlin 1.9+

### Backend
- Python 3.8+
- pip package manager

## Installation

### 1. Backend Setup

```bash
# Clone your backend repository
cd Clarity_backend

# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Start backend server
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

Backend will be available at:
- Local: `http://127.0.0.1:8000`
- Network: `http://YOUR_IP:8000`
- API Docs: `http://127.0.0.1:8000/docs`

### 2. Android App Setup

```bash
# Open project in Android Studio
# File -> Open -> Select ClarityAI folder

# Update backend URL in NetworkModule.kt
# For emulator: http://10.0.2.2:8000/
# For physical device: http://YOUR_COMPUTER_IP:8000/

# Sync Gradle and build project
```

## Configuration

### Backend URL Configuration

Edit `app/src/main/java/com/clarity/ai/network/NetworkModule.kt`:

```kotlin
object NetworkModule {
    // For Android Studio Emulator
    private const val BASE_URL = "http://10.0.2.2:8000/"
    
    // For Physical Device (replace with your IP)
    // private const val BASE_URL = "http://192.168.1.XXX:8000/"
}
```

Find your IP:
- **Windows**: `ipconfig` (look for IPv4 Address)
- **Mac/Linux**: `ifconfig | grep inet`

## Usage

### Basic Workflow

1. **Start Backend**
   ```bash
   uvicorn main:app --reload --host 0.0.0.0 --port 8000
   ```

2. **Launch App**
   - Run app on emulator or device
   - Grant file permissions when prompted

3. **Device Scan Tab**
   - Tap "Start AI Scan" to scan device files
   - Tap "Analyze" on any file to upload to backend
   - Backend processes with AI and returns results

4. **AI Analysis Tab**
   - View all files analyzed by backend
   - See AI summaries, hashes, and recommendations
   - Tap "Refresh" to update from backend database

### Testing with Sample Files

```bash
# Upload test files to emulator
adb push test_document.pdf /sdcard/Download/
adb push test_image.jpg /sdcard/DCIM/Camera/
adb push test_video.mp4 /sdcard/Movies/

# In app: Scan -> Find files -> Tap "Analyze"
```

## API Endpoints Used

### Backend Endpoints
- `GET /dashboard` - Storage statistics
- `GET /mobile/insights` - Mobile-optimized insights
- `POST /analyze/document` - Upload & analyze documents
- `POST /analyze/image` - Upload & analyze images
- `GET /documents` - Retrieve analyzed documents
- `GET /images` - Retrieve analyzed images
- `GET /suggestions` - Cleanup recommendations
- `GET /rename/suggestions` - Smart rename suggestions

## Project Structure

```
ClarityAI/
├── app/
│   ├── src/main/
│   │   ├── java/com/clarity/ai/
│   │   │   ├── data/
│   │   │   │   └── repository/
│   │   │   │       └── FileRepository.kt
│   │   │   ├── model/
│   │   │   │   ├── FileInfo.kt
│   │   │   │   ├── FileAnalysis.kt
│   │   │   │   ├── ScanState.kt
│   │   │   │   └── StorageInsights.kt
│   │   │   ├── network/
│   │   │   │   ├── ClarityApiService.kt
│   │   │   │   └── NetworkModule.kt
│   │   │   ├── ui/
│   │   │   │   ├── screens/
│   │   │   │   │   ├── MainScreen.kt
│   │   │   │   │   └── AnalyzedFilesScreen.kt
│   │   │   │   └── theme/
│   │   │   ├── utils/
│   │   │   │   └── PermissionHandler.kt
│   │   │   ├── viewmodel/
│   │   │   │   └── FileAnalysisViewModel.kt
│   │   │   └── MainActivity.kt
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
└── README.md
```

## Troubleshooting

### Connection Issues

**Problem**: "Backend unavailable"
```bash
# Verify backend is running on correct host
uvicorn main:app --reload --host 0.0.0.0 --port 8000

# Check firewall allows port 8000
# Verify BASE_URL in NetworkModule.kt
```

**Problem**: "Connection refused" on physical device
```bash
# Use computer's actual IP, not localhost
# Windows: ipconfig
# Mac/Linux: ifconfig | grep inet
# Update BASE_URL to http://YOUR_IP:8000/
```

### File Upload Issues

**Problem**: Files not uploading
- Check file permissions granted
- Verify file exists on device: `adb shell ls /sdcard/Download/`
- Watch Logcat for errors: Filter by "FileRepository"

**Problem**: Upload timeout
- Large files (>20MB) may take time
- Check backend processing logs
- DistilBART model loads slowly first time

### Permission Issues

**Problem**: No files found
- Grant storage permissions in app
- For Android 13+: Need READ_MEDIA_* permissions
- Check AndroidManifest.xml has correct permissions

## Monitoring & Debugging

### Android Logcat Filters
```
FileRepository - File upload logs
OkHttp - Network traffic
NetworkModule - API calls
```

### Backend Logs
FastAPI automatically logs all requests:
```
INFO: POST /analyze/document - 200 OK
Analyzing: document.pdf
DistilBART summary generated
Saved to clarity_db.json
```

## Performance Considerations

- **File Scanning**: Limited to 50 files per type for performance
- **Upload Size**: Works well up to 20MB per file
- **Analysis Speed**: 
  - Images: ~1-2 seconds (hashing)
  - PDFs: ~5-10 seconds (text extraction + summarization)
  - First analysis slower (model loading)

## Privacy & Security

- Files sent only to YOUR backend (localhost or your IP)
- No third-party services used
- FileProvider used for secure file access
- Temporary URI permissions only
- Backend stores only analysis results, not full files

## Dependencies

See `app/build.gradle.kts` for complete Android dependencies.

Key libraries:
- Jetpack Compose
- Retrofit 2.9.0
- OkHttp 4.11.0
- Room (for future local caching)
- Accompanist Permissions

## Backend Dependencies

See `requirements.txt` for Python dependencies.

## Future Enhancements

- [ ] Offline mode with local ML models
- [ ] File cleanup actions (delete, archive)
- [ ] Bulk file analysis
- [ ] Export analysis reports
- [ ] Cloud backup integration
- [ ] Advanced duplicate detection
- [ ] File categorization

## Contributing

This is a project for demonstrating AI-powered file analysis on Android.

## License

[Your License Here]

## Support

For issues:
1. Check Logcat for Android errors
2. Check backend terminal for Python errors
3. Verify network connectivity
4. Review API documentation at `/docs` endpoint

## Credits

- **ML Models**: HuggingFace Transformers (DistilBART)
- **Image Hashing**: ImageHash library
- **PDF Processing**: PyPDF2
- **Backend**: FastAPI
- **Frontend**: Jetpack Compose