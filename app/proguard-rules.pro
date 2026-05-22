# Keep OpenGL renderer and game engine entry points used via reflection/lifecycle.
-keep class com.breakoutplus.game.GameRenderer { *; }
-keep class com.breakoutplus.game.GameGLSurfaceView { *; }
-keep class com.breakoutplus.game.GameEngine { *; }
-keep class com.breakoutplus.game.GameAudioManager { *; }

# ViewBinding
-keep class com.breakoutplus.databinding.** { *; }

# Kotlin enums persisted in JSON preferences
-keepclassmembers enum com.breakoutplus.game.GameMode { *; }
-keepclassmembers enum com.breakoutplus.game.GameSound { *; }
-keepclassmembers enum com.breakoutplus.game.GameHaptic { *; }
-keepclassmembers enum com.breakoutplus.game.ChallengeType { *; }
-keepclassmembers enum com.breakoutplus.game.RewardType { *; }
-keepclassmembers enum com.breakoutplus.game.PowerUpType { *; }
-keepclassmembers enum com.breakoutplus.game.BrickType { *; }

# Data classes used in challenge/score persistence
-keep class com.breakoutplus.game.DailyChallenge { *; }
-keep class com.breakoutplus.ScoreboardManager$HighScoreEntry { *; }
