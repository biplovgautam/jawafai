# Jawafai - AI-Powered Chat Application

![Jawafai Logo](app/src/main/ic_launcher-playstore.png)

## 📱 Overview

Jawafai is a modern Android chat application that integrates AI-powered conversations using the Groq API. The app provides users with an intelligent chatbot experience while maintaining a clean, Material Design 3 interface built with Jetpack Compose.

## ✨ Key Features

### 🤖 AI Chat Integration
- **Groq API Integration**: Powered by Llama3-8b-8192 model for intelligent conversations
- **Real-time Chat**: Seamless chat experience with typing indicators and message history
- **Conversation Management**: Multiple chat sessions with conversation history
- **Context Awareness**: Maintains conversation context for better responses

### 🔔 Smart Notification System (Advanced AI-Powered Feature)
- **Cross-App Notification Reading**: Intercepts notifications from supported messaging apps (WhatsApp, Instagram, Facebook Messenger, Telegram, Snapchat, Twitter)
- **AI-Generated Replies**: Automatically generates contextually aware, personalized replies using Groq LLM
- **Direct Reply Integration**: Sends AI-generated replies directly through original apps using RemoteInput actions
- **Conversation Context**: Maintains conversation history and analyzes tone for more natural responses
- **Smart Retry Mechanism**: Robust retry system with status tracking for failed send attempts
- **Real-time Status Updates**: Live updates on reply generation and sending status
- **Supported Platforms**:
  - WhatsApp & WhatsApp Business
  - Instagram Direct Messages
  - Facebook Messenger
  - Telegram
  - Snapchat
  - Twitter DMs
- **Advanced Features**:
  - Conversation tone analysis (casual, formal, affectionate, urgent, professional)
  - User persona integration for personalized responses
  - Deduplication system to prevent duplicate notifications
  - Background processing with efficient memory management
  - Notification access permission handling

### 🔐 Authentication & User Management
- **Firebase Authentication**: Secure user registration and login
- **User Profiles**: Complete user profile management with bio and profile pictures
- **Remember Me**: Persistent login with secure preferences
- **Persona System**: User persona completion tracking

### 📱 Modern UI/UX
- **Material Design 3**: Latest Material You design system
- **Jetpack Compose**: Modern declarative UI framework
- **Dark/Light Theme**: Adaptive theming support
- **Smooth Animations**: Lottie animations and custom transitions
- **Responsive Design**: Optimized for various screen sizes

### 🖼️ Media Management
- **Cloudinary Integration**: Cloud-based image storage and optimization
- **Image Upload**: Profile picture and media sharing capabilities
- **Coil Image Loading**: Efficient image loading with caching

### 📊 Data Management
- **Firebase Firestore**: Real-time database for chat messages and user data
- **Repository Pattern**: Clean architecture with repository pattern
- **MVVM Architecture**: Model-View-ViewModel pattern with LiveData

## 🏗️ Project Structure

```
app/
└── src/
    └── main/
        ├── java/com/example/jawafai/
        │   ├── JawafaiApplication.kt           # Application class
        │   ├── model/                          # Data models
        │   │   ├── UserModel.kt               # User data model
        │   │   ├── ChatBotModel.kt            # Chat message models
        │   │   ├── ChatMessage.kt             # Chat message structure
        │   │   ├── ChatPreview.kt             # Chat preview model
        │   │   ├── ChatSummary.kt             # Chat summary model
        │   │   └── PersonaQuestion.kt         # Persona questionnaire
        │   ├── managers/                       # Service managers
        │   │   ├── CloudinaryManager.kt       # Image upload management
        │   │   └── GroqApiManager.kt          # AI API management
        │   ├── repository/                     # Data repositories
        │   │   ├── UserRepository.kt          # User data repository
        │   │   └── ChatRepository.kt          # Chat data repository
        │   ├── view/                          # UI screens
        │   │   ├── MainActivity.kt            # Main entry point
        │   │   ├── auth/                      # Authentication screens
        │   │   │   ├── LoginActivity.kt       # Login screen
        │   │   │   └── RegistrationActivity.kt # Registration screen
        │   │   ├── dashboard/                 # Main dashboard
        │   │   │   ├── DashboardActivity.kt   # Dashboard container
        │   │   │   ├── home/                  # Home screen
        │   │   │   ├── chat/                  # Chat screens
        │   │   │   │   ├── ChatBotScreen.kt   # Main chat interface
        │   │   │   │   ├── ChatDetailScreen.kt # Chat details
        │   │   │   │   └── ChatScreen.kt      # Chat list
        │   │   │   ├── notifications/         # Notifications
        │   │   │   └── settings/              # Settings screens
        │   │   ├── splash/                    # Onboarding screens
        │   │   └── ui/                        # UI components
        │   ├── viewmodel/                     # ViewModels
        │   ├── navigation/                    # Navigation components
        │   ├── service/                       # Background services
        │   └── utils/                         # Utility classes
        ├── res/                               # Resources
        │   ├── layout/                        # XML layouts
        │   ├── values/                        # Strings, colors, themes
        │   ├── drawable/                      # Vector drawables
        │   └── mipmap/                        # App icons
        └── AndroidManifest.xml                # App manifest
```

## 🛠️ Technologies Used

### Core Android
- **Kotlin**: Primary programming language
- **Jetpack Compose**: Modern UI toolkit
- **Material Design 3**: Latest design system
- **Navigation Compose**: Type-safe navigation
- **ViewModel & LiveData**: Architecture components

### Backend & Cloud Services
- **Firebase Authentication**: User authentication
- **Firebase Firestore**: Real-time database
- **Firebase Storage**: File storage
- **Cloudinary**: Image management and optimization

### AI & Machine Learning
- **Groq API**: AI-powered chat responses
- **Llama3-8b-8192**: Large language model
- **OkHttp**: HTTP client for API calls

### Additional Libraries
- **Lottie**: Animation library
- **Coil**: Image loading library
- **WorkManager**: Background task management
- **Coroutines**: Asynchronous programming
- **Glide**: Image loading for notifications

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Kotlin 1.8.0 or later
- Android SDK 26 (Android 8.0) or higher
- Java 11

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/jawafai.git
   cd jawafai
   ```

2. **Configure Firebase**
   - Create a new Firebase project
   - Add your Android app to the project
   - Download `google-services.json` and place it in `app/` directory
   - Enable Authentication and Firestore

3. **Setup API Keys**
   Create a `local.properties` file in the root directory:
   ```properties
   # Cloudinary Configuration
   cloudinary.cloudName=your_cloudinary_cloud_name
   cloudinary.apiKey=your_cloudinary_api_key
   cloudinary.apiSecret=your_cloudinary_api_secret
   
   # Groq API Configuration
   groq.apiKey=your_groq_api_key
   ```

4. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```

## 📋 Configuration

### Firebase Setup
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or select existing one
3. Add Android app with package name `com.example.jawafai`
4. Enable the following services:
   - Authentication (Email/Password)
   - Firestore Database
   - Storage

### Groq API Setup
1. Visit [Groq Console](https://console.groq.com/)
2. Create an account and generate API key
3. Add the API key to `local.properties`

### Cloudinary Setup
1. Create account at [Cloudinary](https://cloudinary.com/)
2. Get your cloud name, API key, and API secret
3. Add credentials to `local.properties`

## 🔧 Build Configuration

### Gradle Dependencies
The app uses the following major dependencies:
- `androidx.compose.bom`: Compose Bill of Materials
- `firebase.bom`: Firebase Bill of Materials
- `androidx.navigation.compose`: Navigation for Compose
- `androidx.lifecycle`: Lifecycle components
- `com.cloudinary:cloudinary-android`: Cloudinary SDK
- `com.squareup.okhttp3:okhttp`: HTTP client
- `com.airbnb.android:lottie-compose`: Lottie animations

### Build Types
- **Debug**: Development build with logging enabled
- **Release**: Production build with optimization

## 📱 App Features in Detail

### Authentication Flow
1. **Splash Screen**: App intro with smooth animations
2. **Onboarding**: Welcome screens for new users
3. **Registration**: User signup with email verification
4. **Login**: Secure authentication with remember me option
5. **Profile Setup**: Complete user profile with image upload

### Chat System
1. **AI Chat Interface**: Clean chat UI with message bubbles
2. **Real-time Messaging**: Instant message delivery
3. **Typing Indicators**: Shows when AI is responding
4. **Message History**: Persistent conversation storage
5. **Multiple Conversations**: Support for multiple chat sessions

### Dashboard Navigation
1. **Home**: Overview and quick actions
2. **Chat**: AI conversation interface
3. **Notifications**: Message and system notifications
4. **Settings**: App configuration and preferences
5. **Profile**: User profile management

## 📸 Screenshots

### Authentication Flow
<table>
  <tr>
    <td><img src="screenshots/splash_screen.png" width="250" alt="Splash Screen"/></td>
    <td><img src="screenshots/onboarding.png" width="250" alt="Onboarding"/></td>
    <td><img src="screenshots/login.png" width="250" alt="Login Screen"/></td>
  </tr>
  <tr>
    <td align="center"><b>Splash Screen</b></td>
    <td align="center"><b>Onboarding</b></td>
    <td align="center"><b>Login Screen</b></td>
  </tr>
</table>

### Main Application
<table>
  <tr>
    <td><img src="screenshots/dashboard.png" width="250" alt="Dashboard"/></td>
    <td><img src="screenshots/chat_interface.png" width="250" alt="Chat Interface"/></td>
    <td><img src="screenshots/chat_list.png" width="250" alt="Chat List"/></td>
  </tr>
  <tr>
    <td align="center"><b>Dashboard</b></td>
    <td align="center"><b>AI Chat Interface</b></td>
    <td align="center"><b>Chat List</b></td>
  </tr>
</table>

### User Profile & Settings
<table>
  <tr>
    <td><img src="screenshots/profile.png" width="250" alt="User Profile"/></td>
    <td><img src="screenshots/settings.png" width="250" alt="Settings"/></td>
    <td><img src="screenshots/notifications.png" width="250" alt="Notifications"/></td>
  </tr>
  <tr>
    <td align="center"><b>User Profile</b></td>
    <td align="center"><b>Settings</b></td>
    <td align="center"><b>Notifications</b></td>
  </tr>
</table>

### Smart Notification System (AI-Powered Auto-Reply)
<table>
  <tr>
    <td><img src="screenshots/smart_notification_reply.png" width="250" alt="Smart Notification Auto-Reply"/></td>
    <td><img src="screenshots/notification_permission.png" width="250" alt="Notification Permission Setup"/></td>
    <td><img src="screenshots/ai_reply_status.png" width="250" alt="AI Reply Status"/></td>
  </tr>
  <tr>
    <td align="center"><b>AI Auto-Reply in Action</b></td>
    <td align="center"><b>Permission Setup</b></td>
    <td align="center"><b>Reply Status & History</b></td>
  </tr>
</table>

**Smart Notification Features:**
- 🤖 **AI reads incoming notifications** from WhatsApp, Instagram, Telegram, etc.
- 🧠 **Generates contextual replies** using Groq LLM based on conversation tone
- 📱 **Sends replies directly** through the original messaging apps
- 🔄 **Real-time status updates** showing reply generation and sending progress
- 📊 **Conversation analysis** for personalized, tone-appropriate responses

## 🎨 UI Components

### Material Design 3 Components
- `NavigationBar`: Bottom navigation with animated icons
- `TopAppBar`: Contextual app bars with actions
- `FloatingActionButton`: Primary action buttons
- `Card`: Content containers with elevation
- `Button`: Various button styles and states

### Custom Animations
- Smooth screen transitions
- Loading animations with Lottie
- Fade and slide animations
- Progress indicators

## 🛡️ Security Features

### Data Protection
- Firebase Authentication for secure user management
- Encrypted local storage for sensitive data
- HTTPS for all API communications
- Input validation and sanitization

### Privacy
- Minimal data collection
- User consent for permissions
- Secure image upload to Cloudinary
- No sensitive data in logs (release builds)

## 📊 Performance Optimizations

### Memory Management
- Efficient image loading with Coil
- Proper lifecycle management
- Background processing with WorkManager
- Database query optimization

### Network Optimization
- Request caching with OkHttp
- Image optimization with Cloudinary
- Efficient API calls with coroutines
- Network state monitoring

## 🧪 Testing

### Unit Tests
- Model classes testing
- Repository pattern testing
- ViewModel testing
- Utility function testing

### UI Tests
- Compose UI testing
- Navigation testing
- User interaction testing
- Screen rotation testing

## 🚀 Deployment

### Release Build
1. Update version in `build.gradle.kts`
2. Generate signed APK/AAB
3. Test on multiple devices
4. Upload to Google Play Console

### CI/CD Pipeline
- Automated testing on push
- Build verification
- Code quality checks
- Security scanning

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Firebase team for excellent backend services
- Groq for powerful AI API
- Cloudinary for image management
- Material Design team for beautiful components
- Android Jetpack Compose team for modern UI framework

## 📞 Support

For support and questions:
- Create an issue in the GitHub repository
- Contact the development team
- Check the documentation

---

**Made with ❤️ using Kotlin, Jetpack Compose and firebase**
