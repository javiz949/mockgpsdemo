PROYECTO DEMO: Detector de GPS simulado (proyecto estudiantil)
=============================================================

COMO ABRIR EL PROYECTO
----------------------
1. Instala Android Studio (gratis): https://developer.android.com/studio
2. Descomprime esta carpeta y abrela con:
   File > Open > selecciona la carpeta "MockGpsDemo"
3. Espera a que Gradle termine de sincronizar (barra inferior).
4. Conecta tu telefono (ver PASOS DEL TELEFONO abajo) o inicia un emulador.
5. Presiona el boton verde "Run" (triangulo) para instalar y lanzar la app.

PASOS EN TU TELEFONO
--------------------
1. Activa "Opciones de desarrollador":
   - Ve a Ajustes > Acerca del telefono
   - Toca 7 veces "Numero de compilacion" (Build number)
2. En Opciones de desarrollador, activa "Depuracion USB" (USB debugging)
3. Conecta el telefono a la PC con cable USB
4. Acepta el mensaje "¿Permitir depuracion USB?" en el telefono

COMO PROBAR LA DETECCION (con la app de ejemplo)
------------------------------------------------
1. Instala cualquier app "Fake GPS" desde Play Store
2. En Opciones de desarrollador > "App de ubicacion simulada",
   selecciona la app Fake GPS
3. Pon una ubicacion falsa en Fake GPS
4. Abre Detector Mock GPS y presiona "Verificar mi ubicacion"
5. Resultado esperado: isFromMockProvider() = true (deteccion OK)

COMO PROBAR EL OCULTAMIENTO (demo)
----------------------------------
- En el EMULADOR de Android Studio, la ubicacion inyectada con
  "adb emu geo fix <longitud> <latitud>" NO activa el flag mock.
- En telefono fisico se requiere root + hooking (ver seccion
  teorica del reporte; no cubierto en este proyecto).

Nota: todo con fines educativos. Solo prueba sobre esta app propia.
