# Canales CC

App Android nativa (Kotlin + Jetpack Compose + Media3/ExoPlayer) para Fire TV y celular.

- Carga listas M3U por URL o archivo, y permite añadir canales sueltos.
- Reproduce HLS/TS y muestra closed captions (CEA-608/708 y WebVTT) con botón CC.
- Muestra si el canal trae pistas de texto ("CC detectados").

## Usar
1. Abre la carpeta en Android Studio (Koala o más nuevo) y espera el sync de Gradle.
2. Ejecuta en un celular/emulador, o en Fire TV por ADB:
   adb connect IP_DEL_FIRE_TV:5555
   adb install app/build/outputs/apk/debug/app-debug.apk
3. En Fire TV activa antes "Depuración ADB" en Opciones de desarrollador.

## Compilar con GitHub Actions
1. Sube la carpeta a un repositorio de GitHub (rama main).
2. Ve a la pestaña Actions, abre el workflow "Build APK" (se ejecuta solo en cada push, o con "Run workflow").
3. Al terminar, descarga el APK desde "Artifacts" (CanalesCC-debug-apk).
El APK es de debug, instalable directo en Fire TV o celular.
